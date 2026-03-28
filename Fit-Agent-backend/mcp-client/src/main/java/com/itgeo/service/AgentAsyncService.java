package com.itgeo.service;

import com.itgeo.bean.AgentExecuteContext;

/**
 * Agent 异步执行服务。
 *
 * 职责：
 * 1. 在独立线程中推进 run / step 状态；
 * 2. 调用聊天能力生成最终结果；
 * 3. 回填 assistant 消息并释放并发锁。
 */
public interface AgentAsyncService {

    /**
     * 异步执行 Agent 工作流。
     *
     * @param context Agent 执行上下文
     */
    void executeAsync(AgentExecuteContext context);
}
