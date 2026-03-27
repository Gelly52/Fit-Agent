package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgentFinishResponse {
    private String message;
    private String botMsgId;
    private Long runId;
    private String status;
    private Object sources;
}
