package com.itgeo.service.impl;

import cn.hutool.json.JSONUtil;
import com.itgeo.bean.*;
import com.itgeo.mapper.RagBenchmarkRunMapper;
import com.itgeo.pojo.RagBenchmarkRun;
import com.itgeo.service.DocumentService;
import com.itgeo.service.RagBenchmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagBenchmarkServiceImpl implements RagBenchmarkService {

    private static final int MAX_QUESTION_COUNT = 20;
    private static final int TOP_HIT_PREVIEW_MAX_LENGTH = 120;

    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";

    private final DocumentService documentService;
    private final RagBenchmarkRunMapper ragBenchmarkRunMapper;

    @Override
    public RagBenchmarkEvaluateResponse evaluate(Long userId, RagBenchmarkEvaluateRequest request) {
        validateRequest(userId, request);

        String datasetName = normalizeNullableText(request.getDatasetName());
        RagConfigResponse ragConfig = requireRagConfig();
        int effectiveTopK = resolveEffectiveTopK(request.getTopK(), ragConfig);

        RagBenchmarkRun run = new RagBenchmarkRun();
        run.setUserId(userId);
        run.setDatasetName(datasetName);
        run.setQuestionCount(request.getQuestions().size());
        run.setStatus(STATUS_RUNNING);
        run.setConfigSnapshotJson(JSONUtil.toJsonStr(
                buildConfigSnapshot(request.getTopK(), effectiveTopK, ragConfig)
        ));
        ragBenchmarkRunMapper.insert(run);

        try {
            List<RagBenchmarkQuestionResultResponse> results = new ArrayList<>();
            int hitCount = 0;

            for (int i = 0; i < request.getQuestions().size(); i++) {
                RagBenchmarkQuestionRequest item = request.getQuestions().get(i);
                String expectedFileName = item.getExpectedFileName().trim();

                List<Document> documents = documentService.doSearch(
                        item.getQuestion(),
                        userId,
                        effectiveTopK
                );

                List<String> retrievedFileNames = extractRetrievedFileNames(documents);
                boolean hit = retrievedFileNames.stream().anyMatch(expectedFileName::equals);

                if (hit) {
                    hitCount++;
                }

                RagBenchmarkQuestionResultResponse questionResult = new RagBenchmarkQuestionResultResponse();
                questionResult.setIndex(i + 1);
                questionResult.setQuestion(item.getQuestion());
                questionResult.setExpectedFileName(expectedFileName);
                questionResult.setHit(hit);
                questionResult.setRetrievedFileNames(retrievedFileNames);
                questionResult.setTopHitPreview(buildTopHitPreview(documents));
                results.add(questionResult);
            }

            RagBenchmarkEvaluateResponse response = new RagBenchmarkEvaluateResponse();
            response.setRunId(run.getId());
            response.setDatasetName(datasetName);
            response.setQuestionCount(request.getQuestions().size());
            response.setTopK(effectiveTopK);
            response.setStatus(STATUS_SUCCESS);
            response.setHitCount(hitCount);
            response.setHitRate((double) hitCount / request.getQuestions().size());
            response.setUserIsolationEnabled(ragConfig.getUserIsolationEnabled());
            response.setIsolationStrategy(ragConfig.getIsolationStrategy());
            response.setResults(results);

            markRunSuccess(run.getId(), response);
            return response;
        } catch (Exception e) {
            markRunFailed(run.getId(), datasetName, effectiveTopK, e.getMessage());
            log.error("RAG benchmark 评测失败, userId={}, runId={}", userId, run.getId(), e);
            throw e;
        }
    }

    private void validateRequest(Long userId, RagBenchmarkEvaluateRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("questions不能为空");
        }
        if (request.getQuestions().size() > MAX_QUESTION_COUNT) {
            throw new IllegalArgumentException("questions最多20条");
        }

        for (int i = 0; i < request.getQuestions().size(); i++) {
            RagBenchmarkQuestionRequest item = request.getQuestions().get(i);
            int index = i + 1;

            if (item == null) {
                throw new IllegalArgumentException("questions[" + index + "]不能为空");
            }
            if (!hasText(item.getQuestion())) {
                throw new IllegalArgumentException("questions[" + index + "].question不能为空");
            }
            if (!hasText(item.getExpectedFileName())) {
                throw new IllegalArgumentException("questions[" + index + "].expectedFileName不能为空");
            }
        }
    }

    private RagConfigResponse requireRagConfig() {
        RagConfigResponse ragConfig = documentService.getRagConfig();
        if (ragConfig == null) {
            throw new IllegalArgumentException("RAG配置不存在");
        }
        return ragConfig;
    }

    private int resolveEffectiveTopK(Integer requestedTopK, RagConfigResponse ragConfig) {
        int safeDefaultTopK = (ragConfig.getDefaultTopK() == null || ragConfig.getDefaultTopK() <= 0)
                ? 4
                : ragConfig.getDefaultTopK();

        int safeMaxTopK = (ragConfig.getMaxTopK() == null || ragConfig.getMaxTopK() <= 0)
                ? safeDefaultTopK
                : Math.max(ragConfig.getMaxTopK(), safeDefaultTopK);

        if (requestedTopK == null || requestedTopK <= 0) {
            return safeDefaultTopK;
        }
        return Math.min(requestedTopK, safeMaxTopK);
    }

    private Map<String, Object> buildConfigSnapshot(Integer requestedTopK,
                                                    Integer effectiveTopK,
                                                    RagConfigResponse ragConfig) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("requestedTopK", requestedTopK);
        snapshot.put("effectiveTopK", effectiveTopK);
        snapshot.put("defaultTopK", ragConfig.getDefaultTopK());
        snapshot.put("maxTopK", ragConfig.getMaxTopK());
        snapshot.put("filterScanLimit", ragConfig.getFilterScanLimit());
        snapshot.put("userIsolationEnabled", ragConfig.getUserIsolationEnabled());
        snapshot.put("isolationStrategy", ragConfig.getIsolationStrategy());
        return snapshot;
    }

    private List<String> extractRetrievedFileNames(List<Document> documents) {
        LinkedHashSet<String> fileNames = new LinkedHashSet<>();
        if (documents == null) {
            return new ArrayList<>();
        }

        for (Document document : documents) {
            if (document == null || document.getMetadata() == null) {
                continue;
            }
            Object metadataFileName = document.getMetadata().get("fileName");
            if (metadataFileName == null) {
                continue;
            }
            String fileName = String.valueOf(metadataFileName);
            if (hasText(fileName)) {
                fileNames.add(fileName);
            }
        }

        return new ArrayList<>(fileNames);
    }

    private String buildTopHitPreview(List<Document> documents) {
        if (documents == null || documents.isEmpty() || documents.get(0) == null) {
            return null;
        }

        String text = documents.get(0).getText();
        if (!hasText(text)) {
            return null;
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= TOP_HIT_PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, TOP_HIT_PREVIEW_MAX_LENGTH) + "...";
    }

    private void markRunSuccess(Long runId, RagBenchmarkEvaluateResponse response) {
        RagBenchmarkRun update = new RagBenchmarkRun();
        update.setId(runId);
        update.setStatus(STATUS_SUCCESS);
        update.setResultJson(JSONUtil.toJsonStr(response));
        ragBenchmarkRunMapper.updateById(update);
    }

    private void markRunFailed(Long runId, String datasetName, Integer topK, String message) {
        try {
            Map<String, Object> failed = new LinkedHashMap<>();
            failed.put("status", STATUS_FAILED);
            failed.put("datasetName", datasetName);
            failed.put("topK", topK);
            failed.put("message", message);

            RagBenchmarkRun update = new RagBenchmarkRun();
            update.setId(runId);
            update.setStatus(STATUS_FAILED);
            update.setResultJson(JSONUtil.toJsonStr(failed));
            ragBenchmarkRunMapper.updateById(update);
        } catch (Exception ex) {
            log.error("更新RAG benchmark失败状态失败, runId={}", runId, ex);
        }
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String normalizeNullableText(String text) {
        return hasText(text) ? text.trim() : null;
    }
}
