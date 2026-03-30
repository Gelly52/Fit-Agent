package com.itgeo.service.impl;

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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 异步工作流执行服务实现。
 *
 * 当前阶段职责：
 * 1. 推进固定 5 个步骤的状态流转；
 * 2. 调用聊天能力执行普通问答或联网问答；
 * 3. 成功时回填 assistant 占位消息；
 * 4. 失败时标记 run / step 失败并尽量回填失败消息；
 * 5. 最终释放 Redis 并发锁。
 *
 * 说明：
 * - 这里禁止再使用 UserContextHolder，必须只依赖 AgentExecuteContext 中显式传入的用户上下文；
 * - 当前 Phase 1 先不接入 RAG 自动检索与自然语言写库能力。
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

    @Resource
    private AgentRunService agentRunService;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ChatService chatService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 异步推进 Agent 工作流。
     *
     * @param context Agent 执行上下文
     */
    @Async("agentTaskExecutor")
    @Override
    public void executeAsync(AgentExecuteContext context) {
        Integer currentStepNo = null;
        boolean finishEventAlreadySent = false;
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
            finishEventAlreadySent = true;

            agentRunService.markStepSuccess(context.getRunId(), 3, "{\"status\":\"ok\"}");
            pushStepEvent(context, 3, resolveStepName(3), "success", "核心能力执行完成");

            currentStepNo = 4;
            agentRunService.markStepRunning(context.getRunId(), 4, "{\"phase\":\"result\"}");
            pushStepEvent(context, 4, resolveStepName(4), "running", "开始整理最终结果");

            AgentFinishResponse finish = buildSuccessFinishResponse(context, response);
            agentRunService.markStepSuccess(context.getRunId(), 4, JSONUtil.toJsonStr(finish));
            pushStepEvent(context, 4, resolveStepName(4), "success", "结果整理完成");

            currentStepNo = 5;
            agentRunService.markStepRunning(context.getRunId(), 5, "{\"phase\":\"finish\"}");
            pushStepEvent(context, 5, resolveStepName(5), "running", "开始回填最终结果");


            agentRunService.markStepSuccess(context.getRunId(), 5, "{\"status\":\"ok\"}");
            pushStepEvent(context, 5, resolveStepName(5), "success", "任务回传完成");

            // 5. 将最终 finish 结构落到 run 主记录中，便于后续查询与审计。
            agentRunService.markRunSuccess(context.getRunId(), JSONUtil.toJsonStr(finish));
        } catch (Exception e) {
            log.error("Agent异步执行失败, runId={}", context.getRunId(), e);
            String failedMessage = "任务执行失败：" + (e.getMessage() == null ? "未知错误" : e.getMessage());

            if (currentStepNo != null) {
                agentRunService.markStepFailed(context.getRunId(), currentStepNo, failedMessage);
                pushStepEvent(context, currentStepNo, resolveStepName(currentStepNo), "failed", failedMessage);
            }
            agentRunService.markRunFailed(context.getRunId(), failedMessage);

            // 只有当聊天链路尚未发出 FINISH 时，才补发失败 FINISH，避免用户收到双重结束事件。
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
            releaseLock(context.getLockKey());
        }
    }

    /**
     * 执行核心能力。
     *
     * 当前 Phase 1 规则：
     * - 包含“联网/搜索”等关键词时走联网问答；
     * - 其他场景默认走普通问答。
     */
    private ChatResponseEntity executeCoreAbility(AgentExecuteContext context) {
        return chatService.doAgentWithEnhancers(
                context.getChatEntity(),
                context.getChatSessionId(),
                context.getAssistantMessageId(),
                context.getAuthenticatedUser()
        );
    }
//    private ChatResponseEntity executeCoreAbility(AgentExecuteContext context) {
//        String message = context.getChatEntity().getMessage();
//        if (message != null && (message.contains("联网") || message.contains("搜索"))) {
//            return chatService.doAgentInternetSearch(
//                    context.getChatEntity(),
//                    context.getChatSessionId(),
//                    context.getAssistantMessageId(),
//                    context.getAuthenticatedUser()
//            );
//        }
//        return chatService.doAgentChat(
//                context.getChatEntity(),
//                context.getChatSessionId(),
//                context.getAssistantMessageId(),
//                context.getAuthenticatedUser()
//        );
//    }

    /**
     * 将聊天返回结果转换为 AgentFinishResponse，便于 run 结果落库。
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
     * 释放 Redis 并发锁。
     */
    private void releaseLock(String lockKey) {
        if (lockKey != null && !lockKey.isBlank()) {
            stringRedisTemplate.delete(lockKey);
        }
    }

    //RAG：
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

    //联网：
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
