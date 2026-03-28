package com.itgeo.service;

import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.bean.AgentExecuteAckResponse;
import com.itgeo.bean.ChatEntity;

/**
 * Agent 任务受理服务。
 *
 * 职责：
 * 1. 校验 /agent/execute 请求参数；
 * 2. 处理幂等与并发锁；
 * 3. 在事务内创建会话、消息、run、step；
 * 4. 在事务提交后派发异步执行。
 */
public interface AgentExecuteService {

    /**
     * 受理一条 Agent 执行请求，并立即返回 ack。
     *
     * @param authenticatedUser 当前登录用户上下文
     * @param chatEntity 聊天请求体
     * @return 受理结果
     */
    AgentExecuteAckResponse execute(AuthenticatedUserContext authenticatedUser, ChatEntity chatEntity);
}
