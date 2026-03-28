package com.itgeo.controller;

import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.auth.UserContextHolder;
import com.itgeo.bean.AgentExecuteAckResponse;
import com.itgeo.bean.ChatEntity;
import com.itgeo.service.AgentExecuteService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 执行入口控制器。
 *
 * 职责：
 * 1. 接收 /agent/execute 请求；
 * 2. 从鉴权上下文提取当前用户；
 * 3. 调用 AgentExecuteService 完成任务受理；
 * 4. 返回标准 LeeResult 响应。
 */
@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Resource
    private AgentExecuteService agentExecuteService;

    /**
     * 受理一条 Agent 执行请求。
     *
     * 说明：
     * - 该接口只负责“受理”，不会等待最终回答生成；
     * - 最终 SSE 推流与结果落库由异步链路继续完成。
     *
     * @param chatEntity 聊天请求体
     * @return 受理结果
     */
    @PostMapping("/execute")
    public LeeResult execute(@RequestBody ChatEntity chatEntity) {
        try {
            AuthenticatedUserContext authenticatedUser = UserContextHolder.getRequired();
            AgentExecuteAckResponse ack = agentExecuteService.execute(authenticatedUser, chatEntity);
            return LeeResult.ok(ack);
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("Agent 任务受理失败", e);
            return LeeResult.errorException("Agent 任务受理失败");
        }
    }
}
