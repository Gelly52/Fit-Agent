package com.itgeo.service;

import com.itgeo.bean.rag.RagRetrievedChunk;

import java.util.List;

public interface RerankService {
    List<RagRetrievedChunk> rerank(String query, List<RagRetrievedChunk> candidates, int finalTopK);
}
