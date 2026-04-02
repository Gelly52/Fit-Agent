package com.itgeo.service;

import com.itgeo.bean.rag.RagRetrievedChunk;
import org.springframework.ai.document.Document;

import java.util.List;

public interface KeywordSearchService {
    void indexChunks(List<Document> splitDocuments);

    List<RagRetrievedChunk> search(String question, Long userId, Integer topK);
}
