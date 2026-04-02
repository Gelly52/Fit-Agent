package com.itgeo.bean;

import com.itgeo.auth.AuthenticatedUserContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Agent 异步执行上下文，封装受理阶段产出的必要运行信息。
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgentExecuteContext {
    /** Agent 运行ID。 */
    private Long runId;
    /** 关联的聊天会话ID。 */
    private Long chatSessionId;
    /** assistant 占位消息ID。 */
    private Long assistantMessageId;
    /** 登录 sessionId 维度锁的 key。 */
    private String lockKey;
    /** 当前 run 对应的锁持有者标识。 */
    private String lockOwner;
    /** 当前登录用户上下文。 */
    private AuthenticatedUserContext authenticatedUser;
    /** 原始聊天请求体。 */
    private ChatEntity chatEntity;
}
