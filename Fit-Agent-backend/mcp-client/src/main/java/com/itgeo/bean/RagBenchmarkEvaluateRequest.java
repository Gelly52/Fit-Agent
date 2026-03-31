package com.itgeo.bean;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagBenchmarkEvaluateRequest {
    private String datasetName;
    private Integer topK;
    private List<RagBenchmarkQuestionRequest> questions;
}
