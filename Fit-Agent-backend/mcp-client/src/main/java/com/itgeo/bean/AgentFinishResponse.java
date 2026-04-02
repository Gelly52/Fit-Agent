package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Agent run 的结果快照，也是聊天链路未正常收尾时使用的失败兜底 FINISH 结构。
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgentFinishResponse {
    /** 最终输出内容或失败提示。 */
    private String message;
    /** 本轮输出对应的机器人消息ID。 */
    private String botMsgId;
    /** Agent 运行ID。 */
    private Long runId;
    /** 当前运行状态，如 success / failed。 */
    private String status;
    /** 可选来源信息快照。 */
    private Object sources;
    /** 关联的聊天会话ID。 */
    private Long chatSessionId;
    /** 关联的会话编码。 */
    private String sessionCode;
}
