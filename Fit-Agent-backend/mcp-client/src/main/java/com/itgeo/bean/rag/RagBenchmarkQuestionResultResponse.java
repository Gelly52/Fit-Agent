package com.itgeo.bean.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagBenchmarkQuestionResultResponse {
    private Integer index;
    private String question;
    private String expectedFileName;
    private Boolean hit;
    private List<String> retrievedFileNames;
    private String topHitPreview;
    private Integer firstHitChunkRank;
    private Integer firstHitFileRank;
    private Boolean top1Hit;
    private Double reciprocalRank;
    private Integer retrievedChunkCount;
    private Integer uniqueRetrievedFileCount;
    private Integer duplicateChunkCount;
    private String top1FileName;
}
