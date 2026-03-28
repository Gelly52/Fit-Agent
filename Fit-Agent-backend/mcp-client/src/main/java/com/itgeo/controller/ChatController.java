package com.itgeo.controller;

import cn.hutool.core.util.StrUtil;
import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.auth.UserContextHolder;
import com.itgeo.bean.ChatEntity;
import com.itgeo.pojo.AgentRun;
import com.itgeo.service.AgentRunService;
import com.itgeo.service.ChatService;
import com.itgeo.service.ChatSessionService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        chatService.doChat(chatEntity, authenticatedUser);
        return LeeResult.ok();
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
}
