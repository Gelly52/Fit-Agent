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
public class AgentRunListItemResponse {
    private Long runId;
    private Long chatSessionId;
    private String sessionCode;
    private String botMsgId;
    private String requestText;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
