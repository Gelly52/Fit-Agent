package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgentExecuteAckResponse {
    private Long runId;
    private Long chatSessionId;
    private String sessionCode;
    private String botMsgId;
    private String status;
    private Boolean duplicate;
}
