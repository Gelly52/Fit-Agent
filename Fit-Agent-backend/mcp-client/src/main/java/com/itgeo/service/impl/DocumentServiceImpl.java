package com.itgeo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itgeo.bean.rag.RagConfigResponse;
import com.itgeo.bean.rag.RagDocumentItem;
import com.itgeo.bean.rag.RagRetrievedChunk;
import com.itgeo.bean.rag.RagSearchResult;
import com.itgeo.config.RagEmbeddingProperties;
import com.itgeo.mapper.RagDocumentMapper;

import com.itgeo.pojo.RagDocument;
import com.itgeo.service.DocumentService;
import com.itgeo.service.KeywordSearchService;
import com.itgeo.service.RagFusionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import com.itgeo.service.chunking.SemanticDocumentChunker;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    private final SemanticDocumentChunker semanticDocumentChunker;

    private final RagEmbeddingProperties ragEmbeddingProperties;

    private final KeywordSearchService keywordSearchService;

    private final RagFusionService ragFusionService;

    /**
     * 读取文本、切分语义 chunk 并写入向量库。
     */
    @Override
    public List<Document> loadText(Resource resource, String fileName, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }

        /*
         * 入库步骤：
         * 1. 先把 `fileName`、`userId` 写入原始文档 metadata，确保后续切分链路继承文档归属信息；
         * 2. 调用 `SemanticDocumentChunker` 生成适合向量检索的 chunk；
         * 3. 为每个 chunk 再次补齐 `fileName`、`userId` 与 `source`，保证向量库检索与来源展示字段完整；
         * 4. 调用 `RedisVectorStore.add(...)` 写入向量索引；
         * 5. 最后把文件级入库结果写入 `t_rag_document`，供文档列表与运维排查使用。
         */

        String safeFileName = (fileName == null || fileName.isBlank()) ? "unknown" : fileName;
        String metadataUserId = String.valueOf(userId);

        TextReader reader = new TextReader(resource);
        reader.getCustomMetadata().put("fileName", safeFileName);
        reader.getCustomMetadata().put("userId", metadataUserId);

        List<Document> documentList = reader.get();
        List<Document> splitDocuments = semanticDocumentChunker.splitDocuments(documentList);

        //
        RagDocument ragDocument = new RagDocument();
        ragDocument.setUserId(userId);
        ragDocument.setFileName(safeFileName);
        ragDocument.setSourceCount(documentList.size());
        ragDocument.setChunkCount(splitDocuments.size());
        ragDocument.setStatus("READY");
        ragDocumentMapper.insert(ragDocument);

        String documentId = String.valueOf(ragDocument.getId());


        for (int i = 0; i < splitDocuments.size(); i++) {
            Document document = splitDocuments.get(i);
            String chunkId = documentId + ":" + i;

            document.getMetadata().put("fileName", safeFileName);
            document.getMetadata().put("userId", metadataUserId);
            document.getMetadata().put("source", safeFileName);
            document.getMetadata().put("documentId", documentId);
            document.getMetadata().put("chunkId", chunkId);
            document.getMetadata().put("chunkSeq", i);
        }

        redisVectorStore.add(splitDocuments);
        keywordSearchService.indexChunks(splitDocuments);

        log.info("RAG文档入库完成, userId={}, fileName={}, sourceCount={}, chunkCount={}",
                userId,
                safeFileName,
                documentList.size(),
                splitDocuments.size());
        return documentList;
    }

    /**
     * 基于问题检索当前用户可见的 RAG 文档片段。
     */
    @Override
    public List<Document> doSearch(String question, Long userId, Integer topK) {
        return doSearchWithTrace(question, userId, topK).getFinalDocuments();
    }


    @Override
    public RagSearchResult doSearchWithTrace(String question, Long userId, Integer topK) {
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question不能为空");
        }
        int finalTopK = normalizeTopK(topK);
        int vectorRecallK = ragEmbeddingProperties.getRetrieval().getVectorRecallK();
        int keywordRecallK = ragEmbeddingProperties.getRetrieval().getKeywordRecallK();

        /*
         * 检索隔离说明：
         * 1. 这里通过 `SearchRequest.filterExpression("userId == '...'")` 把 userId 条件下推到 RedisVectorStore；
         * 2. 用户隔离发生在向量检索侧，只有满足 metadata.userId 的文档才会参与返回；
         * 3. 当前实现不是“先查全量结果，再在 Java 内存中过滤”。
         */
        List<RagRetrievedChunk> vectorHits = vectorRecall(question, userId, vectorRecallK);
        List<RagRetrievedChunk> keywordHits = keywordSearchService.search(question, userId, keywordRecallK);
        List<RagRetrievedChunk> fusedHits = ragFusionService.fuse(vectorHits, keywordHits, finalTopK);

        RagSearchResult result = new RagSearchResult();
        result.setQuestion(question);
        result.setFinalTopK(finalTopK);
        result.setVectorHits(vectorHits);
        result.setKeywordHits(keywordHits);
        result.setFusedHits(fusedHits);
        result.setFinalDocuments(
                fusedHits.stream().map(RagRetrievedChunk::getDocument).collect(Collectors.toList())
        );

        log.info("RAG混合检索完成, userId={}, finalTopK={}, vectorHits={}, keywordHits={}, fusedHits={}",
                userId, finalTopK, vectorHits.size(), keywordHits.size(), fusedHits.size());

        return result;
    }

    /**
     * 查询当前用户已上传的 RAG 文档列表。
     */
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

    /**
     * 返回当前代码中生效的手动 RAG 配置快照。
     *
     * 其中 `isolationStrategy` 明确表示当前采用 RedisVectorStore `filterExpression` 的检索侧过滤。
     */
    @Override
    public RagConfigResponse getRagConfig() {
        RagConfigResponse response = new RagConfigResponse();

        response.setDefaultTopK(ragEmbeddingProperties.getRetrieval().getDefaultTopK());
        response.setMaxTopK(ragEmbeddingProperties.getRetrieval().getMaxTopK());
        response.setFilterScanLimit(FILTER_SCAN_LIMIT);
        response.setUserIsolationEnabled(true);
        response.setIsolationStrategy("vectorstore_filterExpression_userId");

        response.setRetrievalMode(ragEmbeddingProperties.getRetrieval().getMode());
        response.setVectorRecallK(ragEmbeddingProperties.getRetrieval().getVectorRecallK());
        response.setKeywordRecallK(ragEmbeddingProperties.getRetrieval().getKeywordRecallK());
        response.setRrfK(ragEmbeddingProperties.getRetrieval().getRrfK());
        response.setVectorWeight(ragEmbeddingProperties.getRetrieval().getVectorWeight());
        response.setKeywordWeight(ragEmbeddingProperties.getRetrieval().getKeywordWeight());
        response.setKeywordIndexName(ragEmbeddingProperties.getRetrieval().getKeywordIndexName());

        if(ragEmbeddingProperties.getChunking() != null){
            response.setChunkingStrategy(ragEmbeddingProperties.getChunking().getStrategy());
            response.setMergeThreshold(ragEmbeddingProperties.getChunking().getMergeThreshold());
            response.setBreakpointDropThreshold(ragEmbeddingProperties.getChunking().getBreakpointDropThreshold());
            response.setMaxChunkSentenceCount(ragEmbeddingProperties.getChunking().getMaxChunkSentenceCount());
            response.setMaxChunkChars(ragEmbeddingProperties.getChunking().getMaxChunkChars());
            response.setParagraphBoundaryEnabled(ragEmbeddingProperties.getChunking().getParagraphBoundaryEnabled());
            response.setSentenceEmbeddingBatchSize(ragEmbeddingProperties.getChunking().getSentenceEmbeddingBatchSize());
            response.setDebugLogEnabled(ragEmbeddingProperties.getChunking().getDebugLogEnabled());
        }
        return response;
    }

    /**
     * 规范化 topK，避免一次检索请求过大。
     */
    private int normalizeTopK(Integer topK) {
        int defaultTopK = ragEmbeddingProperties.getRetrieval().getDefaultTopK();
        int maxTopK = ragEmbeddingProperties.getRetrieval().getMaxTopK();
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private List<RagRetrievedChunk> vectorRecall(String question, Long userId, int topK) {
        String filterExpression = "userId == '" + userId + "'";
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .filterExpression(filterExpression)
                .build();

        List<Document> results = redisVectorStore.similaritySearch(request);
        int resultSize = results == null ? 0 : results.size();
        log.info("vectorRecall完成, userId={}, topK={}, resultSize={}, question={}, filterExpression={}",
                userId, topK, resultSize, question, filterExpression);

        if (resultSize == 0) {
            SearchRequest noFilterRequest = SearchRequest.builder()
                    .query(question)
                    .topK(topK)
                    .build();
            List<Document> noFilterResults = redisVectorStore.similaritySearch(noFilterRequest);
            int noFilterResultSize = noFilterResults == null ? 0 : noFilterResults.size();
            log.info("vectorRecall无过滤对照, userId={}, topK={}, resultSize={}, question={}",
                    userId, topK, noFilterResultSize, question);
            if (noFilterResults != null) {
                for (int i = 0; i < Math.min(noFilterResults.size(), 3); i++) {
                    Document doc = noFilterResults.get(i);
                    log.info("vectorRecall无过滤hit idx={}, fileName={}, userId={}, chunkId={}, preview={}",
                            i + 1,
                            doc.getMetadata().get("fileName"),
                            doc.getMetadata().get("userId"),
                            doc.getMetadata().get("chunkId"),
                            doc.getText() == null ? "" : doc.getText().substring(0, Math.min(80, doc.getText().length())));
                }
            }
            return List.of();
        }

        List<RagRetrievedChunk> hits = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            log.info("vectorRecall hit idx={}, fileName={}, chunkId={}, preview={}",
                    i + 1,
                    doc.getMetadata().get("fileName"),
                    doc.getMetadata().get("chunkId"),
                    doc.getText() == null ? "" : doc.getText().substring(0, Math.min(80, doc.getText().length())));
            RagRetrievedChunk item = new RagRetrievedChunk();
            item.setChunkId(String.valueOf(doc.getMetadata().get("chunkId")));
            item.setDocumentId(String.valueOf(doc.getMetadata().get("documentId")));
            item.setChunkSeq(parseInteger(doc.getMetadata().get("chunkSeq")));
            item.setFileName(String.valueOf(doc.getMetadata().get("fileName")));
            item.setUserId(String.valueOf(doc.getMetadata().get("userId")));
            item.setDocument(doc);
            item.setVectorRank(i + 1);
            hits.add(item);
        }
        return hits;
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
