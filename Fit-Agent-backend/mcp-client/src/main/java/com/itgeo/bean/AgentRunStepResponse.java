package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgentRunStepResponse {

    private Integer stepNo;
    private String stepName;
    private String stepStatus;
    private String toolName;
    private String inputJson;
    private String outputJson;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

}
