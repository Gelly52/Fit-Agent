package com.itgeo.controller;

import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.auth.UserContextHolder;
import com.itgeo.bean.AgentExecuteAckResponse;
import com.itgeo.bean.AgentRunDetailResponse;
import com.itgeo.bean.ChatEntity;
import com.itgeo.service.AgentExecuteService;
import com.itgeo.service.AgentRunService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 控制器。
 *
 * 职责：
 * 1. 接收 Agent 执行与查询请求；
 * 2. 从鉴权上下文提取当前登录用户；
 * 3. 调用对应服务完成受理或查询；
 * 4. 统一包装 LeeResult 响应。
 */
@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Resource
    private AgentExecuteService agentExecuteService;

    @Resource
    private AgentRunService agentRunService;

/**
     * 接收一条 Agent 执行请求并返回受理结果。
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

/**
     * 查询当前登录用户最近的 Agent run 列表。
     */
    @GetMapping("/runs")
    public LeeResult listRuns(@RequestParam(required = false) String status,
                         @RequestParam(required = false, defaultValue = "10") Integer limit) {
        try {
            AuthenticatedUserContext authenticatedUser = UserContextHolder.getRequired();
            return LeeResult.ok(
                    agentRunService.listRuns(authenticatedUser.getUserId(), status, limit)
            );
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("查询Agent运行列表失败", e);
            return LeeResult.errorException("查询Agent运行列表失败");
        }
    }

/**
     * 查询当前登录用户指定的 Agent run 详情。
     */
    @GetMapping("/runs/{runId}")
    public LeeResult getRunDetail(@PathVariable Long runId) {
        try {
            AuthenticatedUserContext authenticatedUser = UserContextHolder.getRequired();
            AgentRunDetailResponse detail = agentRunService.getRunDetail(
                    authenticatedUser.getUserId(),
                    runId
            );
            if (detail == null) {
                return LeeResult.errorMsg("Agent运行记录不存在");
            }
            return LeeResult.ok(detail);
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("查询Agent运行详情失败, runId={}", runId, e);
            return LeeResult.errorException("查询Agent运行详情失败");
        }
    }
}
