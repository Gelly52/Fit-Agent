package com.itgeo.controller;

import cn.hutool.core.util.StrUtil;
import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.auth.UserContextHolder;
import com.itgeo.bean.ChatEntity;
import com.itgeo.bean.ChatResponseEntity;
import com.itgeo.pojo.AgentRun;
import com.itgeo.service.AgentRunService;
import com.itgeo.service.ChatService;
import com.itgeo.service.ChatSessionService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

/**
 * 普通聊天控制器。
 * <p>
 * 说明：
 * 1. `/chat/doChat` 是正式聊天入口，也承担前端从 `/agent/execute` 回退时的兜底请求；
 * 2. 当同一 `botMsgId` 已被 Agent 运行链路受理或已落库时，会短路返回，避免重复执行；
 * 3. 控制器只负责鉴权上下文提取与请求分发，具体生成与 SSE 推送由服务层处理。
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private AgentRunService agentRunService;

    @Resource
    private ChatSessionService chatSessionService;

    @PostMapping("/chatTest")
    public String chatTest(@RequestBody ChatEntity chatEntity) {
        return chatService.chatTest(chatEntity.getMessage());
    }

    /**
     * 普通聊天入口。
     * <p>
     * 步骤：
     * 1. 设置响应编码并提取当前登录用户；
     * 2. 对 Agent 回退场景做 botMsgId 去重短路；
     * 3. 把请求委派给服务层，由服务层按增强开关决定具体执行分支。
     */
    @PostMapping("/doChat")
    public LeeResult doChat(@RequestBody ChatEntity chatEntity, HttpServletResponse response) {
        // 1. SSE/流式文本统一使用 UTF-8，避免前端中文分片出现乱码
        response.setCharacterEncoding("UTF-8");

        // 2. 当前接口必须在已鉴权上下文下执行
        AuthenticatedUserContext authenticatedUser = UserContextHolder.getRequired();

        // 3. 如果这是 agent 回退请求且 botMsgId 已被受理或落库，则直接跳过本次兜底执行
        if (shouldSkipFallback(chatEntity, authenticatedUser)) {
            return LeeResult.ok();
        }

        // 4. 绑定当前用户名，并交给服务层按增强开关自动分发
        chatEntity.setCurrentUserName(authenticatedUser.getUserKey());
        ChatResponseEntity result = chatService.doChatWithEnhancers(chatEntity, authenticatedUser);
        return LeeResult.ok(result);
    }

    /**
     * 当前端从 `/agent/execute` 回退到 `/chat/doChat` 时，按既定顺序检查是否需要跳过。
     * <p>
     * 检查顺序：
     * 1. 先看 Agent 运行记录是否已受理该 botMsgId；
     * 2. 再看聊天消息历史中是否已存在同一 botMsgId；
     * 3. 只要命中其一，就认为当前兜底请求无需再次执行。
     */
    private boolean shouldSkipFallback(ChatEntity chatEntity, AuthenticatedUserContext authenticatedUser) {
        String botMsgId = chatEntity == null ? null : chatEntity.getBotMsgId();
        if (StrUtil.isBlank(botMsgId)) {
            return false;
        }

        AgentRun existingRun = agentRunService.findByUserIdAndBotMsgId(
                authenticatedUser.getUserId(),
                botMsgId
        );
        if (existingRun != null) {
            return true;
        }

        return chatSessionService.existsByUserIdAndBotMsgId(
                authenticatedUser.getUserId(),
                botMsgId
        );
    }

    /**
     * 查询当前登录用户的聊天历史记录。
     */
    @GetMapping("/records")
    public LeeResult getRecords(
            @RequestParam(required = false) String who,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        // 1. 查询身份始终以当前登录用户为准
        AuthenticatedUserContext authenticatedUser = UserContextHolder.getRequired();

        // 2. who 仅用于兼容前端已有调用，真实查询身份仍以当前登录用户为准
        return LeeResult.ok(
                chatSessionService.getChatRecords(
                        authenticatedUser.getUserId(),
                        sessionId,
                        limit
                )
        );
    }
}
