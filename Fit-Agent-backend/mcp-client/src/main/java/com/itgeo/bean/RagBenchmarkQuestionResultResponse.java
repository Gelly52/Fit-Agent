package com.itgeo.bean;

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
}
