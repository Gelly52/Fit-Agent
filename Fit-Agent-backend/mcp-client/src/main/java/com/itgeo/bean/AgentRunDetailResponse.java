package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgentRunDetailResponse {
    private Long runId;
    private Long chatSessionId;
    private String sessionCode;
    private String botMsgId;
    private String requestText;
    private String status;
    private String resultJson;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<AgentRunStepResponse> steps;
}
