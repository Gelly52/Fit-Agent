package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ChatStreamChunkResponse {
    private String contentChunk;
    private String botMsgId;
    private Long runId;
    private Long chatSessionId;
    private String sessionCode;
    private String sceneType;
    private String sourceType;
}
