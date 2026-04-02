package com.itgeo.bean.rag;

import lombok.Data;
import org.springframework.ai.document.Document;

@Data
public class RagRetrievedChunk {

    private String chunkId;
    private String documentId;
    private Integer chunkSeq;
    private String fileName;
    private String userId;

    private Document document;

    private Integer vectorRank;
    private Integer keywordRank;
    private Double fusionScore;
}