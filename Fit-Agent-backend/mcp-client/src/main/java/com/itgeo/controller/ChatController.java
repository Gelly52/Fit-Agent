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
 *
 * 说明：
 * 1. /chat/doChat 既服务普通聊天，也承担 /agent/execute 前端 fallback 兜底；
 * 2. 当同一 botMsgId 已由 Agent 链路受理或已落库时，需要短路返回，避免重复执行。
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
     */
    @PostMapping("/doChat")
    public LeeResult doChat(@RequestBody ChatEntity chatEntity, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        AuthenticatedUserContext authenticatedUser = UserContextHolder.getRequired();

        if (shouldSkipFallback(chatEntity, authenticatedUser)) {
            return LeeResult.ok();
        }

        chatEntity.setCurrentUserName(authenticatedUser.getUserKey());
        ChatResponseEntity result = chatService.doChatWithEnhancers(chatEntity, authenticatedUser);
        return LeeResult.ok(result);
    }

    /**
     * 当前端从 /agent/execute 回退到 /chat/doChat 时，按既定顺序检查是否需要跳过。
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

        return chatSessionService.existsByBotMsgId(botMsgId);
    }

    /**
     * 查询当前登录用户的聊天历史记录。
     */
    @GetMapping("/records")
    public LeeResult getRecords(
            @RequestParam(required = false) String who,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        // 1.获取当前登录用户
        AuthenticatedUserContext authenticatedUser = UserContextHolder.getRequired();

        // who 仅用于兼容前端已有调用，真实查询身份仍以当前登录用户为准

        return LeeResult.ok(
                chatSessionService.getChatRecords(
                        authenticatedUser.getUserId(),
                        sessionId,
                        limit
                )
        );
    }
}
