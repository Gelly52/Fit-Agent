package com.itgeo.service.impl;

import cn.hutool.core.util.StrUtil;
import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.bean.AgentExecuteAckResponse;
import com.itgeo.bean.AgentExecuteContext;
import com.itgeo.bean.ChatEntity;
import com.itgeo.pojo.AgentRun;
import com.itgeo.pojo.ChatSession;
import com.itgeo.service.AgentAsyncService;
import com.itgeo.service.AgentExecuteService;
import com.itgeo.service.AgentRunService;
import com.itgeo.service.ChatSessionService;
import com.itgeo.utils.SSEServer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

/**
 * Agent 任务受理服务实现。
 *
 * 职责：
 * 1. 校验 /agent/execute 请求参数；
 * 2. 做 userId + botMsgId 幂等判断；
 * 3. 做同一登录会话的并发锁控制；
 * 4. 在事务内创建会话、消息、run、step；
 * 5. 在事务提交后派发异步执行。
 *
 * 说明：
 * - 这里只负责“短事务受理”，不负责真正的大模型调用；
 * - 真正执行工作流的逻辑在 AgentAsyncService 中完成；
 * - 同步阶段一旦异常，需要释放本次 Redis 锁，避免产生假占用。
 */
@Slf4j
@Service
public class AgentExecuteServiceImpl implements AgentExecuteService {

    private static final long AGENT_LOCK_TTL_SECONDS = 120L;
    private static final String AGENT_LOCK_KEY_PREFIX = "fit-agent:agent:run:session:";

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private AgentRunService agentRunService;

    @Resource
    private AgentAsyncService agentAsyncService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 受理一条 Agent 执行请求，并在事务提交后异步派发执行。
     *
     * @param authenticatedUser 当前登录用户上下文
     * @param chatEntity 聊天请求体
     * @return 受理 ack
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentExecuteAckResponse execute(AuthenticatedUserContext authenticatedUser, ChatEntity chatEntity) {
        validateExecuteRequest(authenticatedUser, chatEntity);

        String sseClientId = authenticatedUser.getSseClientId();
        if (!SSEServer.isConnected(sseClientId)) {
            throw new IllegalArgumentException("SSE通道未建立，请先连接后再发起任务");
        }

        Long userId = authenticatedUser.getUserId();
        String botMsgId = chatEntity.getBotMsgId().trim();

        // 当前阶段后端用户身份只认 token，对 currentUserName 做覆盖，避免信任前端传值。
        chatEntity.setCurrentUserName(authenticatedUser.getUserKey());

        // 1. 先做数据库幂等检查：已存在则直接返回已有 run 信息。
        AgentRun existing = agentRunService.findByUserIdAndBotMsgId(userId, botMsgId);
        if (existing != null) {
            ChatSession existingSession = chatSessionService.findByIdAndUserId(
                    existing.getChatSessionId(),
                    userId
            );
            String existingSessionCode = existingSession == null ? null : existingSession.getSessionCode();
            return new AgentExecuteAckResponse(
                    existing.getId(),
                    existing.getChatSessionId(),
                    existingSessionCode,
                    botMsgId,
                    existing.getStatus(),
                    true
            );
        }

        String lockKey = buildLockKey(authenticatedUser.getSessionId());
        boolean lockAcquired = false;
        try {
            // 2. 同一登录 session 同时只允许一个 running agent。
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    botMsgId,
                    AGENT_LOCK_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
            if (!Boolean.TRUE.equals(locked)) {
                throw new IllegalArgumentException("当前已有任务执行中，请稍后再试");
            }
            lockAcquired = true;

            // 3. 创建会话、写入用户消息、创建 assistant 占位消息。
            ChatSession session = chatSessionService.resolveOrCreateSession(
                    userId,
                    chatEntity.getSessionCode(),
                    "agent",
                    chatEntity.getMessage(),
                    botMsgId
            );
            String sourceType = resolveSourceType(chatEntity);
            chatSessionService.appendUserMessage(session.getId(), chatEntity.getMessage(), sourceType);
            Long assistantMessageId = chatSessionService.createAssistantPlaceholder(session.getId(), botMsgId, sourceType);

            // 4. 创建运行主记录并初始化固定步骤。
            Long runId = agentRunService.createRun(userId, session.getId(), botMsgId, chatEntity.getMessage());
            agentRunService.initSteps(runId, chatEntity);

            chatEntity.setSessionCode(session.getSessionCode());

            // 5. 组装异步执行上下文。
            AgentExecuteContext context = new AgentExecuteContext(
                    runId,
                    session.getId(),
                    assistantMessageId,
                    lockKey,
                    authenticatedUser,
                    chatEntity
            );

            // 6. 必须在事务提交后再派发异步任务，避免异步线程读到未提交数据。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        agentAsyncService.executeAsync(context);
                    } catch (Exception e) {
                        log.error("Agent任务派发失败, runId={}", runId, e);
                        releaseLock(lockKey);
                        agentRunService.markRunFailed(runId, "Agent任务派发失败");
                        chatSessionService.finishAssistantMessage(
                                assistantMessageId,
                                "任务派发失败，请稍后重试",
                                null
                        );
                    }
                }
            });

            return new AgentExecuteAckResponse(
                    runId,
                    session.getId(),
                    session.getSessionCode(),
                    botMsgId,
                    "pending",
                    false
            );
        } catch (RuntimeException e) {
            // 同步受理阶段异常时，必须立刻释放锁，避免本次事务回滚后仍占用锁。
            if (lockAcquired) {
                releaseLock(lockKey);
            }
            throw e;
        }
    }

    /**
     * 校验 Agent 执行请求的基础入参。
     */
    private void validateExecuteRequest(AuthenticatedUserContext authenticatedUser, ChatEntity chatEntity) {
        if (authenticatedUser == null || authenticatedUser.getUserId() == null || authenticatedUser.getSessionId() == null) {
            throw new IllegalArgumentException("登录状态已失效，请重新登录");
        }
        if (chatEntity == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (StrUtil.isBlank(chatEntity.getMessage())) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (StrUtil.isBlank(chatEntity.getBotMsgId())) {
            throw new IllegalArgumentException("botMsgId不能为空");
        }
    }

    /**
     * 构造 session 级别的并发锁 key。
     */
    private String buildLockKey(Long sessionId) {
        return AGENT_LOCK_KEY_PREFIX + sessionId;
    }

    /**
     * 释放 Redis 并发锁。
     */
    private void releaseLock(String lockKey) {
        if (StrUtil.isNotBlank(lockKey)) {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private String resolveSourceType(ChatEntity chatEntity) {
        boolean ragEnabled = chatEntity != null && Boolean.TRUE.equals(chatEntity.getRagEnabled());
        boolean internetEnabled = chatEntity != null && Boolean.TRUE.equals(chatEntity.getInternetEnabled());

        if (ragEnabled && internetEnabled) {
            throw new IllegalArgumentException("暂不支持同时开启知识库增强与联网补充");
        }
        if (ragEnabled) {
            return "rag";
        }
        if (internetEnabled) {
            return "internet";
        }
        return "chat";
    }
}
