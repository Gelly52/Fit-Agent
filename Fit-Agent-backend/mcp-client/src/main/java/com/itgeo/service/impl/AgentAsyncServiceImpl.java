package com.itgeo.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.itgeo.bean.*;
import com.itgeo.enums.SSEMsgType;
import com.itgeo.service.AgentAsyncService;
import com.itgeo.service.AgentRunService;
import com.itgeo.service.ChatService;
import com.itgeo.service.ChatSessionService;
import com.itgeo.utils.SSEServer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Agent 异步工作流执行服务实现。
 *
 * 职责：
 * 1. 在独立线程中推进 run / step 的固定五步状态流转；
 * 2. 在第 3 步按请求中的增强开关分发 Agent 聊天能力；
 * 3. 在成功路径上汇总运行结果快照，并把结果写回 run / step；
 * 4. 在失败路径上补齐 run / step 失败状态，并在必要时发送失败兜底 FINISH；
 * 5. 负责登录 sessionId 并发锁的续期与最终释放。
 *
 * 说明：
 * - 成功路径的 assistant 占位消息回填与成功 FINISH 发送由 ChatServiceImpl.streamAndSend 负责；
 * - 本类不重复回填成功消息，只做运行态状态闭环、结果快照落库和失败兜底；
 * - 这里只依赖 AgentExecuteContext 中显式传入的用户上下文，不再使用 UserContextHolder。
 */
@Service
@Slf4j
public class AgentAsyncServiceImpl implements AgentAsyncService {

    private static final List<String> DEFAULT_STEP_NAMES = List.of(
            "解析任务意图",
            "加载上下文",
            "执行核心能力",
            "生成结果",
            "完成回传"
    );

    private static final long AGENT_LOCK_TTL_SECONDS = 120L;
    private static final long AGENT_LOCK_RENEW_INTERVAL_SECONDS = 30L;
    private static final DefaultRedisScript<Long> COMPARE_AND_EXPIRE_SCRIPT;
    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE_SCRIPT;

    static {
        COMPARE_AND_EXPIRE_SCRIPT = new DefaultRedisScript<>();
        COMPARE_AND_EXPIRE_SCRIPT.setResultType(Long.class);
        COMPARE_AND_EXPIRE_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "  return redis.call('expire', KEYS[1], ARGV[2]) " +
                        "else " +
                        "  return 0 " +
                        "end"
        );

        COMPARE_AND_DELETE_SCRIPT = new DefaultRedisScript<>();
        COMPARE_AND_DELETE_SCRIPT.setResultType(Long.class);
        COMPARE_AND_DELETE_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "  return redis.call('del', KEYS[1]) " +
                        "else " +
                        "  return 0 " +
                        "end"
        );
    }

    @Resource
    private AgentRunService agentRunService;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ChatService chatService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

/**
     * 在异步线程中推进一次 Agent run 的运行态闭环。
     * <p>
     * 执行步骤：
     * 1. 启动登录 sessionId 维度锁的续期任务，避免长任务执行时锁过期；
     * 2. 依次推进固定五个 step 的 running / success / failed 状态；
     * 3. 在第 3 步按增强开关分发 Agent 聊天能力；
     * 4. 在成功路径上生成运行结果快照，并完成 run / step 的最终落库；
     * 5. 在失败路径上仅在聊天链路尚未结束时补发失败兜底 FINISH；
     * 6. 最终停止续期任务并释放锁。
     *
     * @param context Agent 执行上下文
     */
    @Async("agentTaskExecutor")
    @Override
    public void executeAsync(AgentExecuteContext context) {
        Integer currentStepNo = null;
        boolean finishEventAlreadySent = false;

        ScheduledExecutorService renewExecutor = null;
        ScheduledFuture<?> renewFuture = null;
        // 运行期间持续续期登录 sessionId 维度的 Redis 锁，避免长任务执行时锁提前过期。
        if (StrUtil.isNotBlank(context.getLockKey()) && StrUtil.isNotBlank(context.getLockOwner())) {
            renewExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "agent-lock-renew-" + context.getRunId());
                thread.setDaemon(true);
                return thread;
            });
            renewFuture = renewExecutor.scheduleAtFixedRate(
                    () -> {
                        try {
                            boolean renewed = renewLock(
                                    context.getLockKey(),
                                    context.getLockOwner(),
                                    AGENT_LOCK_TTL_SECONDS
                            );
                            if (!renewed) {
                                log.warn("Agent锁续期失败, runId={}, lockKey={}",
                                        context.getRunId(),
                                        context.getLockKey());
                            }
                        } catch (Exception ex) {
                            log.warn("Agent锁续期异常, runId={}, lockKey={}",
                                    context.getRunId(),
                                    context.getLockKey(),
                                    ex);
                        }
                    },
                    AGENT_LOCK_RENEW_INTERVAL_SECONDS,
                    AGENT_LOCK_RENEW_INTERVAL_SECONDS,
                    TimeUnit.SECONDS
            );
        }

        try {
            // 1. 运行主记录进入 running 状态。
            agentRunService.markRunRunning(context.getRunId());

            currentStepNo = 1;
            agentRunService.markStepRunning(context.getRunId(), 1, "{\"phase\":\"intent\"}");
            pushStepEvent(context, 1, resolveStepName(1), "running", "开始解析任务意图");
            agentRunService.markStepSuccess(context.getRunId(), 1, "{\"status\":\"ok\"}");
            pushStepEvent(context, 1, resolveStepName(1), "success", "任务意图解析完成");

            currentStepNo = 2;
            agentRunService.markStepRunning(context.getRunId(), 2, "{\"phase\":\"context\"}");
            pushStepEvent(context, 2, resolveStepName(2), "running", "开始加载上下文");
            agentRunService.markStepSuccess(context.getRunId(), 2, "{\"status\":\"ok\"}");
            pushStepEvent(context, 2, resolveStepName(2), "success", "上下文加载完成");

            currentStepNo = 3;
            agentRunService.markStepRunning(context.getRunId(), 3, "{\"phase\":\"core\"}");
            pushStepEvent(context, 3, resolveStepName(3), "running", "开始执行核心能力");

            ChatResponseEntity response = executeCoreAbility(context);
            // 第3步成功返回时，assistant 占位消息回填与成功 FINISH 已由 ChatServiceImpl.streamAndSend 完成；这里仅继续推进运行态状态。
            finishEventAlreadySent = true;

            agentRunService.markStepSuccess(context.getRunId(), 3, "{\"status\":\"ok\"}");
            pushStepEvent(context, 3, resolveStepName(3), "success", "核心能力执行完成");

            currentStepNo = 4;
            agentRunService.markStepRunning(context.getRunId(), 4, "{\"phase\":\"result\"}");
            pushStepEvent(context, 4, resolveStepName(4), "running", "开始整理最终结果");

            AgentFinishResponse finish = buildSuccessFinishResponse(context, response);
            agentRunService.markStepSuccess(context.getRunId(), 4, JSONUtil.toJsonStr(finish));
            pushStepEvent(context, 4, resolveStepName(4), "success", "结果整理完成");

            // 步骤5：这里只做运行态收尾与状态闭环，确认 run / step 已完整落库，不在这里重复发送成功 FINISH。

            currentStepNo = 5;
            agentRunService.markStepRunning(context.getRunId(), 5, "{\"phase\":\"finish\"}");
            pushStepEvent(context, 5, resolveStepName(5), "running", "开始回填最终结果");
            agentRunService.markStepSuccess(context.getRunId(), 5, "{\"status\":\"ok\"}");
            pushStepEvent(context, 5, resolveStepName(5), "success", "任务回传完成");

            // 将最终 finish 快照写回 run 主记录，供运行查询、审计和失败补偿判断复用。
            agentRunService.markRunSuccess(context.getRunId(), JSONUtil.toJsonStr(finish));
        } catch (Exception e) {
            log.error("Agent异步执行失败, runId={}", context.getRunId(), e);
            String failedMessage = "任务执行失败：" + (e.getMessage() == null ? "未知错误" : e.getMessage());

            if (currentStepNo != null) {
                agentRunService.markStepFailed(context.getRunId(), currentStepNo, failedMessage);
                pushStepEvent(context, currentStepNo, resolveStepName(currentStepNo), "failed", failedMessage);
            }
            agentRunService.markRunFailed(context.getRunId(), failedMessage);

            // 仅当聊天链路尚未发出 FINISH 时，才补发失败兜底 FINISH，避免用户收到双重结束事件。
            if (!finishEventAlreadySent) {
                try {
                    chatSessionService.finishAssistantMessage(
                            context.getAssistantMessageId(),
                            failedMessage,
                            null
                    );
                } catch (Exception ex) {
                    log.warn("回填失败消息失败, runId={}", context.getRunId(), ex);
                }

                AgentFinishResponse failed = new AgentFinishResponse(
                        failedMessage,
                        context.getChatEntity().getBotMsgId(),
                        context.getRunId(),
                        "failed",
                        null,
                        context.getChatSessionId(),
                        context.getChatEntity() == null ? null : context.getChatEntity().getSessionCode()
                );
                SSEServer.sendMsg(
                        context.getAuthenticatedUser().getSseClientId(),
                        JSONUtil.toJsonStr(failed),
                        SSEMsgType.FINISH
                );
            }
        } finally {
            stopRenewTask(renewFuture, renewExecutor);
            releaseLock(context.getLockKey(), context.getLockOwner());
        }
    }

/**
     * 按请求中的增强开关分发 Agent 场景能力。
     *
     * 说明：
     * - 统一复用 ChatService 的 doAgentWithEnhancers 分发逻辑；
     * - 是否走普通问答、知识库增强或联网增强，由请求中的开关决定；
     * - 成功路径的 assistant 占位消息回填和成功 FINISH 发送由 ChatServiceImpl.streamAndSend 完成。
     */
    private ChatResponseEntity executeCoreAbility(AgentExecuteContext context) {
        return chatService.doAgentWithEnhancers(
                context.getChatEntity(),
                context.getChatSessionId(),
                context.getAssistantMessageId(),
                context.getRunId(),
                context.getAuthenticatedUser()
        );
    }

/**
     * 将聊天返回结果整理为 run 结果快照，供成功落库与失败兜底结构复用。
     */
    private AgentFinishResponse buildSuccessFinishResponse(AgentExecuteContext context, ChatResponseEntity response) {
        if (response == null) {
            throw new IllegalStateException("聊天服务未返回结果");
        }
        return new AgentFinishResponse(
                response.getMessage(),
                response.getBotMsgId(),
                context.getRunId(),
                "success",
                response.getSources(),
                response.getChatSessionId(),
                response.getSessionCode()
        );
    }

    /**
     * 根据步骤编号解析固定步骤名称。
     */
    private String resolveStepName(Integer stepNo) {
        if (stepNo == null || stepNo <= 0 || stepNo > DEFAULT_STEP_NAMES.size()) {
            return "未知步骤";
        }
        return DEFAULT_STEP_NAMES.get(stepNo - 1);
    }

    /**
     * 将 sources 对象规范化为 JSON 字符串，供消息表持久化使用。
     */
    private String normalizeSourcesJson(Object sources) {
        return sources == null ? null : JSONUtil.toJsonStr(sources);
    }

    /**
     * 发送步骤事件到 SSE 通道。
     */
    private void pushStepEvent(AgentExecuteContext context,
                               Integer stepNo,
                               String stepName,
                               String stepStatus,
                               String message) {
        AgentStepEvent event = new AgentStepEvent(
                context.getRunId(),
                stepNo,
                stepName,
                stepStatus,
                message
        );
        SSEServer.sendMsg(
                context.getAuthenticatedUser().getSseClientId(),
                JSONUtil.toJsonStr(event),
                SSEMsgType.CUSTOM_EVENT
        );
    }

/**
     * 仅在 owner 仍归属当前 run 时为登录 sessionId 维度锁续期。
     */
    private boolean renewLock(String lockKey, String lockOwner, long ttlSeconds) {
        if (StrUtil.isBlank(lockKey) || StrUtil.isBlank(lockOwner)) {
            return false;
        }
        Long result = stringRedisTemplate.execute(
                COMPARE_AND_EXPIRE_SCRIPT,
                Collections.singletonList(lockKey),
                lockOwner,
                String.valueOf(ttlSeconds)
        );
        return result != null && result > 0;
    }

/**
     * 仅当 owner 匹配时释放登录 sessionId 维度的 Redis 并发锁。
     */
    private void releaseLock(String lockKey, String lockOwner) {
        if (StrUtil.isBlank(lockKey) || StrUtil.isBlank(lockOwner)) {
            return;
        }
        stringRedisTemplate.execute(
                COMPARE_AND_DELETE_SCRIPT,
                Collections.singletonList(lockKey),
                lockOwner
        );
    }

    /**
     * 停止锁续期任务并关闭调度线程。
     */

    private void stopRenewTask(ScheduledFuture<?> renewFuture, ScheduledExecutorService renewExecutor) {
        if (renewFuture != null) {
            renewFuture.cancel(true);
        }
        if (renewExecutor != null) {
            renewExecutor.shutdownNow();
        }
    }

    /**
     * 将 RAG 文档来源转换为统一的 sources 结构。
     */
    private Object buildRagSources(List<Document> ragContext) {
        if (ragContext == null) {
            return List.of();
        }
        return ragContext.stream().map(doc -> {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("title", doc.getMetadata() != null ? doc.getMetadata().getOrDefault("fileName", "知识库来源") : "知识库来源");
            item.put("snippet", doc.getText());
            item.put("url", "");
            item.put("extra", "");
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 将联网搜索结果转换为统一的 sources 结构。
     */
    private Object buildInternetSources(List<SearchResult> searchResults) {
        if (searchResults == null) {
            return List.of();
        }
        return searchResults.stream().map(result -> {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("title", result.getTitle() == null || result.getTitle().isBlank() ? "联网来源" : result.getTitle());
            item.put("snippet", result.getContent());
            item.put("url", result.getUrl());
            item.put("extra", "");
            return item;
        }).collect(Collectors.toList());
    }
}
