package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BodyMetricsLogRequest {
    private String date;
    private BigDecimal weight;
    private BigDecimal bodyFat;
    private BigDecimal sleep;
    private String fatigue;
    private String note;
}
