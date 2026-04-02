package com.itgeo.service;

import com.itgeo.bean.rag.RagRetrievedChunk;

import java.util.List;

/*
 * 融合算法
 */
public interface RagFusionService {
    List<RagRetrievedChunk> fuse(List<RagRetrievedChunk> vectorHits,
                                 List<RagRetrievedChunk> keywordHits,
                                 int topK);
}
