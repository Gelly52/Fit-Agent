package com.itgeo.bean.rag;

import lombok.Data;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;

@Data
public class RagSearchResult {

    private String question;
    private Integer finalTopK;

    private List<RagRetrievedChunk> vectorHits = new ArrayList<>();
    private List<RagRetrievedChunk> keywordHits = new ArrayList<>();
    private List<RagRetrievedChunk> fusedHits = new ArrayList<>();

    private List<Document> finalDocuments = new ArrayList<>();
}
