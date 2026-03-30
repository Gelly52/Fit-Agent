package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Agent 执行完成事件响应。
 *
 * 说明：
 * 1. message 为最终输出内容或失败提示；
 * 2. runId 用于前端关联当前 Agent 任务；
 * 3. chatSessionId / sessionCode 用于前端续聊与历史回显；
 * 4. sources 仅在成功且存在来源信息时返回。
 */
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
    private Long chatSessionId;
    private String sessionCode;
}
