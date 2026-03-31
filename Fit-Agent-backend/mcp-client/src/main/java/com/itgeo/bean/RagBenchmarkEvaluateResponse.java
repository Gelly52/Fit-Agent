package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagBenchmarkEvaluateResponse {
    private Long runId;
    private String datasetName;
    private Integer questionCount;
    private Integer topK;
    private String status;
    private Integer hitCount;
    private Double hitRate;
    private Boolean userIsolationEnabled;
    private String isolationStrategy;
    private List<RagBenchmarkQuestionResultResponse> results;
}
