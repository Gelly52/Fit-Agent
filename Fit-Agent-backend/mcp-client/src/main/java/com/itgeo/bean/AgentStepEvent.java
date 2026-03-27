package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgentStepEvent {
    private Long runId;
    private Integer stepNo;
    private String stepName;
    private String stepStatus;
    private String message;
}
