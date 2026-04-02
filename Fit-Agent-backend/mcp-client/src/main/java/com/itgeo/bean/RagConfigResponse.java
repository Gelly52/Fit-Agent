package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagConfigResponse {
    private Integer defaultTopK;
    private Integer maxTopK;
    private Integer filterScanLimit;
    private Boolean userIsolationEnabled;
    private String isolationStrategy;
}
