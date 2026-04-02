package com.itgeo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itgeo.bean.RagConfigResponse;
import com.itgeo.bean.RagDocumentItem;
import com.itgeo.mapper.RagDocumentMapper;

import com.itgeo.pojo.RagDocument;
import com.itgeo.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档向量化与检索服务实现。
 *
 * 说明：
 * 1. 上传时会把 fileName、userId、source 写入 metadata；
 * 2. 检索时通过 RedisVectorStore 的 filterExpression 基于 userId 做用户隔离；
 * 3. 当前仅服务手动 RAG 接口，不自动接入普通聊天与 Agent 执行链路。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final int DEFAULT_TOP_K = 4;
    private static final int MAX_TOP_K = 10;
    private static final int FILTER_SCAN_LIMIT = 50;

    private final RedisVectorStore redisVectorStore;

    private final RagDocumentMapper ragDocumentMapper;

    @Override
    public List<Document> loadText(Resource resource, String fileName, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }

        String safeFileName = (fileName == null || fileName.isBlank()) ? "unknown" : fileName;
        String metadataUserId = String.valueOf(userId);

        TextReader reader = new TextReader(resource);
        reader.getCustomMetadata().put("fileName", safeFileName);
        reader.getCustomMetadata().put("userId", metadataUserId);

        List<Document> documentList = reader.get();
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = tokenTextSplitter.apply(documentList);

        for (Document document : splitDocuments) {
            document.getMetadata().put("fileName", safeFileName);
            document.getMetadata().put("userId", metadataUserId);
            document.getMetadata().putIfAbsent("source", safeFileName);
        }

        redisVectorStore.add(splitDocuments);

        RagDocument ragDocument = new RagDocument();
        ragDocument.setUserId(userId);
        ragDocument.setFileName(safeFileName);
        ragDocument.setSourceCount(documentList.size());
        ragDocument.setChunkCount(splitDocuments.size());
        ragDocument.setStatus("READY");
        ragDocumentMapper.insert(ragDocument);

        log.info("RAG文档入库完成, userId={}, fileName={}, sourceCount={}, chunkCount={}",
                userId,
                safeFileName,
                documentList.size(),
                splitDocuments.size());
        return documentList;
    }

    @Override
    public List<Document> doSearch(String question, Long userId, Integer topK) {
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        int safeTopK = normalizeTopK(topK);
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(safeTopK)
                .filterExpression("userId == '" + userId + "'")
                .build();

        List<Document> results = redisVectorStore.similaritySearch(request);
        if (results == null || results.isEmpty()) {
            log.info("RAG检索无结果, userId={}, requestedTopK={}", userId, safeTopK);
            return List.of();
        }

        log.info("RAG检索完成, userId={}, requestedTopK={}, resultSize={}",
                userId, safeTopK, results.size());
        return results;
    }

    @Override
    public List<RagDocumentItem> listUserDocuments(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }

        List<RagDocument> ragDocuments = ragDocumentMapper.selectList(new LambdaQueryWrapper<RagDocument>()
                .eq(RagDocument::getUserId, userId)
                .orderByDesc(RagDocument::getCreatedAt));

        return ragDocuments.stream().map(ragDocument -> new RagDocumentItem(
                        ragDocument.getId(),
                        ragDocument.getFileName(),
                        ragDocument.getSourceCount(),
                        ragDocument.getChunkCount(),
                        ragDocument.getStatus(),
                        ragDocument.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public RagConfigResponse getRagConfig() {
        RagConfigResponse response = new RagConfigResponse();
        response.setDefaultTopK(DEFAULT_TOP_K);
        response.setMaxTopK(MAX_TOP_K);
        response.setFilterScanLimit(FILTER_SCAN_LIMIT);
        response.setUserIsolationEnabled(true);
        response.setIsolationStrategy("vectorstore_filterExpression_userId");
        return response;
    }

    /**
     * 规范化 topK，避免一次检索请求过大。
     */
    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }
}
