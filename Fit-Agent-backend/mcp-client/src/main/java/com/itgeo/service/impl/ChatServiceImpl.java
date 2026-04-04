package com.itgeo.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.bean.ChatEntity;
import com.itgeo.bean.ChatResponseEntity;
import com.itgeo.bean.ChatStreamChunkResponse;
import com.itgeo.bean.SearchResult;
import com.itgeo.enums.SSEMsgType;
import com.itgeo.mapper.BodyMetricsMapper;
import com.itgeo.mapper.TrainingLogMapper;
import com.itgeo.pojo.BodyMetrics;
import com.itgeo.pojo.ChatSession;
import com.itgeo.pojo.TrainingLog;
import com.itgeo.prompt.PromptTemplateManager;
import com.itgeo.service.ChatService;
import com.itgeo.service.ChatSessionService;
import com.itgeo.service.DocumentService;
import com.itgeo.service.SearXngService;
import com.itgeo.utils.SSEServer;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天能力服务实现。
 * <p>
 * 说明：
 * 1. 普通问答、手动 RAG 问答、联网问答统一在这里完成模型调用；
 * 2. 所有正式聊天入口都要求显式传入 AuthenticatedUserContext；
 * 3. 实现类负责把流式分片通过 SSE 推送给前端，并在结束时发送 FINISH 事件。
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Resource
    private DocumentService documentService;
    @Resource
    private ChatSessionService chatSessionService;
    @Resource
    private PromptTemplateManager promptTemplateManager;
    // 查询用户数据来构建上下文，需要注入对应的 Mapper
    @Resource
    private TrainingLogMapper trainingLogMapper;
    @Resource
    private BodyMetricsMapper bodyMetricsMapper;

    @Data
    private static class PreparedChatContext {
        private ChatSession session;
        private Long assistantMessageId;
    }

    /**
     * 增强数据容器
     * 用于存储 RAG 和联网搜索的数据
     */
    @Data
    private static class EnhancementData {
        private List<Document> ragContext;
        private List<SearchResult> searchResults;
    }

    /**
     * 为一次正式聊天准备可落库的会话上下文。
     * <p>
     * 步骤：
     * 1. 先按 userId + sessionCode 尝试复用会话，必要时新建会话；
     * 2. 追加本轮用户消息，保证历史记录完整；
     * 3. 创建 assistant 占位消息，供流式结束后统一回填；
     * 4. 返回本轮调用所需的会话对象与占位消息ID。
     */
    private PreparedChatContext prepareConversation(
            ChatEntity chatEntity,
            AuthenticatedUserContext authenticatedUser,
            String sceneType,
            String sourceType
    ) {
        ChatSession session = chatSessionService.resolveOrCreateSession(
                authenticatedUser.getUserId(),
                chatEntity.getSessionCode(),
                sceneType,
                chatEntity.getMessage(),
                chatEntity.getBotMsgId()
        );

        chatSessionService.appendUserMessage(session.getId(), chatEntity.getMessage(), sourceType);
        Long assistantMessageId = chatSessionService.createAssistantPlaceholder(
                session.getId(),
                chatEntity.getBotMsgId(),
                sourceType
        );

        PreparedChatContext ctx = new PreparedChatContext();
        ctx.setSession(session);
        ctx.setAssistantMessageId(assistantMessageId);
        return ctx;
    }


    private final ChatClient chatClient;

    @Resource
    private SearXngService searXngService;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder,
                           ToolCallbackProvider tools,
                           ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(tools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Override
    public String chatTest(String prompt) {
        return chatClient.prompt(prompt).call().content();
    }

    @Override
    public Flux<ChatResponse> streamResponse(String prompt) {
        return chatClient.prompt(prompt).stream().chatResponse();
    }

    @Override
    public Flux<String> streamStr(String prompt) {
        return chatClient.prompt(prompt).stream().content();
    }

    /**
     * 执行普通问答。
     */
    @Override
    public ChatResponseEntity doChat(ChatEntity chatEntity, AuthenticatedUserContext authenticatedUser) {
        String sourceType = resolveSourceType(chatEntity);
        PreparedChatContext ctx = prepareConversation(
                chatEntity,
                authenticatedUser,
                "chat",
                sourceType
        );
        String prompt = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();
        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();
        return streamAndSend(
                stringFlux,
                authenticatedUser,
                botMsgId,
                ctx.getSession().getId(),
                ctx.getSession().getSessionCode(),
                ctx.getAssistantMessageId(),
                "chat",
                sourceType,
                null
        );
    }

    /**
     * 执行带手动 RAG 上下文的问答。
     */
    @Override
    public ChatResponseEntity doChatRagSearch(ChatEntity chatEntity,
                                              List<Document> ragContext,
                                              AuthenticatedUserContext authenticatedUser) {
        String sourceType = resolveSourceType(chatEntity);
        PreparedChatContext ctx = prepareConversation(
                chatEntity,
                authenticatedUser,
                "chat",
                sourceType
        );

        String question = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();
        String context = extractRagText(ragContext);

        String ragPrompt = promptTemplateManager.buildRagPrompt(context, question);
        Prompt prompt = new Prompt(ragPrompt);

        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();
        Object normalizedSources = buildRagSources(ragContext);
        return streamAndSend(
                stringFlux,
                authenticatedUser,
                botMsgId,
                ctx.getSession().getId(),
                ctx.getSession().getSessionCode(),
                ctx.getAssistantMessageId(),
                "chat",
                sourceType,
                normalizedSources
        );
    }

    /**
     * 执行联网搜索增强问答。
     */
    @Override
    public ChatResponseEntity doInternetSearch(ChatEntity chatEntity, AuthenticatedUserContext authenticatedUser) {
        String sourceType = resolveSourceType(chatEntity);
        PreparedChatContext ctx = prepareConversation(
                chatEntity,
                authenticatedUser,
                "chat",
                sourceType
        );

        String question = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();

        List<SearchResult> searchResults = searXngService.search(question);
        String internetText = extractInternetText(searchResults);
        String internetPrompt = promptTemplateManager.buildInternetPrompt(internetText, question);

        Prompt prompt = new Prompt(internetPrompt);
        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();
        Object normalizedSources = buildInternetSources(searchResults);
        return streamAndSend(
                stringFlux,
                authenticatedUser,
                botMsgId,
                ctx.getSession().getId(),
                ctx.getSession().getSessionCode(),
                ctx.getAssistantMessageId(),
                "chat",
                sourceType,
                normalizedSources
        );
    }

    /**
     * 执行 Agent 场景下的纯模型问答。
     * <p>
     * 步骤：
     * 1. 校验 Agent 链路传入的会话确实存在且属于当前用户；
     * 2. 使用 Agent 专用提示词包装当前问题；
     * 3. 发起流式调用，并把结果回填到既有 assistant 占位消息。
     */
    public ChatResponseEntity doAgentChat(
            ChatEntity chatEntity,
            Long chatSessionId,
            Long assistantMessageId,
            Long runId,
            AuthenticatedUserContext authenticatedUser
    ) {
        String sourceType = resolveSourceType(chatEntity);
        ChatSession session = requireExistingSession(chatSessionId, authenticatedUser);

        // 1. 查询用户最近训练数据（最近14天）
        LocalDate fourteenDaysAgo = LocalDate.now().minusDays(14);
        QueryWrapper<TrainingLog> trainingQuery = new QueryWrapper<>();
        trainingQuery.eq("user_id", authenticatedUser.getUserId())
                .ge("training_date", fourteenDaysAgo)
                .orderByDesc("training_date");
        List<TrainingLog> recentLogs = trainingLogMapper.selectList(trainingQuery);

        // 2. 查询最新身体指标
        QueryWrapper<BodyMetrics> metricsQuery = new QueryWrapper<>();
        metricsQuery.eq("user_id", authenticatedUser.getUserId())
                .orderByDesc("record_date")
                .last("LIMIT 1");
        BodyMetrics latestMetrics = bodyMetricsMapper.selectOne(metricsQuery);

        // 3. 构建用户上下文
        String userContext = promptTemplateManager.buildUserContext(
                authenticatedUser.getUserId(),
                recentLogs,
                latestMetrics
        );

        // 4. 构建最终提示词
        String finalPrompt = promptTemplateManager.buildAgentPrompt(
                userContext,
                chatEntity.getMessage()
        );
        Prompt prompt = new Prompt(finalPrompt);
        String botMsgId = chatEntity.getBotMsgId();
        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();

        return streamAndSend(
                stringFlux,
                authenticatedUser,
                botMsgId,
                session.getId(),
                session.getSessionCode(),
                assistantMessageId,
                runId,
                "agent",
                sourceType,
                null
        );
    }

    /**
     * 执行 Agent 场景下的联网增强问答。
     * <p>
     * 步骤：
     * 1. 校验 Agent 会话归属；
     * 2. 加载增强数据（Internet）；
     * 3. 构建提示词并发起流式调用；
     * 4. 将搜索来源一并透传给流式发送与最终回填逻辑。
     */
    public ChatResponseEntity doAgentInternetSearch(
            ChatEntity chatEntity,
            Long chatSessionId,
            Long assistantMessageId,
            Long runId,
            AuthenticatedUserContext authenticatedUser
    ) {
        String sourceType = resolveSourceType(chatEntity);
        ChatSession session = requireExistingSession(chatSessionId, authenticatedUser);

        // 1. 加载增强数据
        EnhancementData data = loadEnhancementData(chatEntity, authenticatedUser);

        // 2. 构建提示词（Internet 模式不需要用户上下文）
        String finalPrompt = buildPromptByEnhancement(chatEntity, data, null);

        // 3. 调用模型
        Prompt prompt = new Prompt(finalPrompt);
        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();

        // 4. 构建来源
        Object sources = buildSourcesByEnhancement(data, chatEntity);

        return streamAndSend(
                stringFlux,
                authenticatedUser,
                chatEntity.getBotMsgId(),
                session.getId(),
                session.getSessionCode(),
                assistantMessageId,
                runId,
                "agent",
                sourceType,
                sources
        );
    }

    /**
     * 根据增强开关分发普通聊天执行路径。
     * <p>
     * 当前正式主链路只支持单开增强：
     * 1. 仅开 RAG 时，自动检索知识库后进入 RAG 问答；
     * 2. 仅开联网时，进入联网增强问答；
     * 3. 两者都关闭时，执行普通聊天；
     * 4. 两者同时开启时直接拒绝，不走 hybrid 预留分支。
     */
    @Override
    public ChatResponseEntity doChatWithEnhancers(ChatEntity chatEntity, AuthenticatedUserContext authenticatedUser) {
        {
            boolean ragEnabled = isRagEnabled(chatEntity);
            boolean internetEnabled = isInternetEnabled(chatEntity);

            if (ragEnabled && internetEnabled) {
                throw new IllegalArgumentException("暂不支持同时开启知识库增强与联网补充");
            }
            if (ragEnabled) {
                return doChatRagSearchAuto(chatEntity, authenticatedUser);
            }
            if (internetEnabled) {
                return doInternetSearch(chatEntity, authenticatedUser);
            }
            return doChat(chatEntity, authenticatedUser);
        }
    }

    /**
     * 根据增强开关分发 Agent 场景执行路径。
     * <p>
     * 当前正式主链路同样只支持单开增强：
     * 1. 仅开 RAG 时，自动检索知识库后进入 Agent RAG 问答；
     * 2. 仅开联网时，进入 Agent 联网增强问答；
     * 3. 两者都关闭时，执行纯 Agent 问答；
     * 4. 两者同时开启时直接拒绝，不走 hybrid 预留分支。
     */
    @Override
    public ChatResponseEntity doAgentWithEnhancers(
            ChatEntity chatEntity,
            Long chatSessionId,
            Long assistantMessageId,
            Long runId,
            AuthenticatedUserContext authenticatedUser
    ) {
        {
            boolean ragEnabled = isRagEnabled(chatEntity);
            boolean internetEnabled = isInternetEnabled(chatEntity);

            if (ragEnabled && internetEnabled) {
                throw new IllegalArgumentException("暂不支持同时开启知识库增强与联网补充");
            }
            if (ragEnabled) {
                return doAgentRagSearch(
                        chatEntity,
                        chatSessionId,
                        assistantMessageId,
                        runId,
                        authenticatedUser
                );
            }
            if (internetEnabled) {
                return doAgentInternetSearch(
                        chatEntity,
                        chatSessionId,
                        assistantMessageId,
                        runId,
                        authenticatedUser
                );
            }
            return doAgentChat(
                    chatEntity,
                    chatSessionId,
                    assistantMessageId,
                    runId,
                    authenticatedUser
            );
        }
    }

    /**
     * 适配无 runId 场景的流式发送入口。
     */
    private ChatResponseEntity streamAndSend(Flux<String> stringFlux,
                                             AuthenticatedUserContext authenticatedUser,
                                             String botMsgId,
                                             Long chatSessionId,
                                             String sessionCode,
                                             Long assistantMessageId,
                                             String sceneType,
                                             String sourceType,
                                             Object sources) {
        return streamAndSend(
                stringFlux,
                authenticatedUser,
                botMsgId,
                chatSessionId,
                sessionCode,
                assistantMessageId,
                null,
                sceneType,
                sourceType,
                sources
        );
    }

    /**
     * 消费模型返回的流式分片，并同步推送到 SSE。
     * <p>
     * 处理步骤：
     * 1. 逐片消费模型输出，并按 ADD 事件实时推送给前端；
     * 2. 汇总完整回答后，回填 assistant 占位消息内容与来源；
     * 3. 组装最终响应对象，补齐会话、运行与来源元数据；
     * 4. 在连接可用时发送 FINISH 事件，通知前端本轮输出结束。
     */
    private ChatResponseEntity streamAndSend(Flux<String> stringFlux,
                                             AuthenticatedUserContext authenticatedUser,
                                             String botMsgId,
                                             Long chatSessionId,
                                             String sessionCode,
                                             Long assistantMessageId,
                                             Long runId,
                                             String sceneType,
                                             String sourceType,
                                             Object sources) {
        String sseClientId = authenticatedUser == null ? null : authenticatedUser.getSseClientId();

        // 1. 逐片消费模型输出，并实时推送 ADD 事件
        List<String> chunks = stringFlux.toStream().map(chunk -> {
            String content = chunk == null ? "" : chunk;
            if (StrUtil.isNotBlank(sseClientId) && !content.isEmpty()) {
                ChatStreamChunkResponse addEvent = new ChatStreamChunkResponse();
                addEvent.setContentChunk(content);
                addEvent.setBotMsgId(botMsgId);
                addEvent.setRunId(runId);
                addEvent.setChatSessionId(chatSessionId);
                addEvent.setSessionCode(sessionCode);
                addEvent.setSceneType(sceneType);
                addEvent.setSourceType(sourceType);

                SSEServer.sendMsg(sseClientId, JSONUtil.toJsonStr(addEvent), SSEMsgType.ADD);
            }
            log.debug("chat chunk received, botMsgId={}, size={}", botMsgId, content.length());
            return content;
        }).collect(Collectors.toList());

        // 2. 拼接完整回答，并把 assistant 占位消息回填为最终内容
        String fullContent = String.join("", chunks);
        chatSessionService.finishAssistantMessage(
                assistantMessageId,
                fullContent,
                sources == null ? null : JSONUtil.toJsonStr(sources)
        );

        // 3. 组装 FINISH 响应体，补齐会话与来源元数据
        ChatResponseEntity chatResponseEntity = new ChatResponseEntity();
        chatResponseEntity.setMessage(fullContent);
        chatResponseEntity.setBotMsgId(botMsgId);
        chatResponseEntity.setRunId(runId);
        chatResponseEntity.setChatSessionId(chatSessionId);
        chatResponseEntity.setSessionCode(sessionCode);
        chatResponseEntity.setSceneType(sceneType);
        chatResponseEntity.setSourceType(sourceType);
        chatResponseEntity.setSources(sources);

        // 4. 推送 FINISH 事件
        if (StrUtil.isNotBlank(sseClientId)) {
            SSEServer.sendMsg(
                    sseClientId,
                    JSONUtil.toJsonStr(chatResponseEntity),
                    SSEMsgType.FINISH
            );
        }

        log.info("chat stream finished, botMsgId={}, runId={}, contentLength={}",
                botMsgId, runId, fullContent.length());
        return chatResponseEntity;
    }

    /**
     * 把 RAG 检索结果转换为前端可直接消费的来源结构。
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
     * 把联网搜索结果转换为前端可直接消费的来源结构。
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

    /**
     * 校验 Agent 运行链路传入的会话是否存在且属于当前用户。
     */
    private ChatSession requireExistingSession(Long chatSessionId, AuthenticatedUserContext authenticatedUser) {
        ChatSession session = chatSessionService.findByIdAndUserId(
                chatSessionId,
                authenticatedUser.getUserId()
        );
        if (session == null) {
            throw new IllegalArgumentException("Agent会话不存在或不属于当前用户");
        }
        return session;
    }

    private boolean isRagEnabled(ChatEntity chatEntity) {
        return chatEntity != null && Boolean.TRUE.equals(chatEntity.getRagEnabled());
    }

    private boolean isInternetEnabled(ChatEntity chatEntity) {
        return chatEntity != null && Boolean.TRUE.equals(chatEntity.getInternetEnabled());
    }

    /**
     * 根据增强类型加载对应数据
     */
    private EnhancementData loadEnhancementData(
            ChatEntity chatEntity,
            AuthenticatedUserContext authenticatedUser
    ) {
        EnhancementData data = new EnhancementData();

        if (isRagEnabled(chatEntity)) {
            data.setRagContext(loadRagContext(chatEntity, authenticatedUser));
        }
        if (isInternetEnabled(chatEntity)) {
            data.setSearchResults(searXngService.search(chatEntity.getMessage()));
        }

        return data;
    }

    /**
     * 提取 RAG 文本
     */
    private String extractRagText(List<Document> ragContext) {
        return ragContext == null ? "" : ragContext.stream()
                .map(Document::getText)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 提取联网搜索文本
     */
    private String extractInternetText(List<SearchResult> searchResults) {
        return searchResults == null ? "" : searchResults.stream()
                .filter(java.util.Objects::nonNull)
                .map(item -> "[来源] " + item.getUrl() + "\n[内容] " + item.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 根据增强类型构建提示词
     *
     * @param chatEntity  聊天实体
     * @param data        增强数据
     * @param userContext 用户上下文（Agent 模式需要，Chat 模式传 null）
     * @return 构建好的提示词
     */
    private String buildPromptByEnhancement(
            ChatEntity chatEntity,
            EnhancementData data,
            String userContext
    ) {
        boolean ragEnabled = isRagEnabled(chatEntity);
        boolean internetEnabled = isInternetEnabled(chatEntity);
        String question = chatEntity.getMessage();

        // Hybrid 模式
        if (ragEnabled && internetEnabled) {
            String ragText = extractRagText(data.getRagContext());
            String internetText = extractInternetText(data.getSearchResults());
            return promptTemplateManager.buildHybridPrompt(ragText, internetText, question);
        }

        // RAG 模式
        if (ragEnabled) {
            String ragText = extractRagText(data.getRagContext());
            return promptTemplateManager.buildRagPrompt(ragText, question);
        }

        // Internet 模式
        if (internetEnabled) {
            String internetText = extractInternetText(data.getSearchResults());
            return promptTemplateManager.buildInternetPrompt(internetText, question);
        }

        // 纯对话模式
        if (userContext != null) {
            // Agent 模式：需要用户上下文
            return promptTemplateManager.buildAgentPrompt(userContext, question);
        } else {
            // Chat 模式：简单对话
            return promptTemplateManager.buildChatPrompt(question);
        }
    }

    /**
     * 根据增强类型构建来源信息
     */
    private Object buildSourcesByEnhancement(EnhancementData data, ChatEntity chatEntity) {
        boolean ragEnabled = isRagEnabled(chatEntity);
        boolean internetEnabled = isInternetEnabled(chatEntity);

        if (ragEnabled && internetEnabled) {
            return mergeSources(
                    buildRagSources(data.getRagContext()),
                    buildInternetSources(data.getSearchResults())
            );
        }
        if (ragEnabled) {
            return buildRagSources(data.getRagContext());
        }
        if (internetEnabled) {
            return buildInternetSources(data.getSearchResults());
        }
        return null;
    }

    private String resolveSourceType(ChatEntity chatEntity) {
        boolean ragEnabled = isRagEnabled(chatEntity);
        boolean internetEnabled = isInternetEnabled(chatEntity);

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

    /**
     * 为当前问题自动加载当前用户的 RAG 上下文。
     * <p>
     * 当前固定检索 4 条候选片段，供自动 RAG 分支复用。
     */
    private List<Document> loadRagContext(ChatEntity chatEntity, AuthenticatedUserContext authenticatedUser) {
        return documentService.doSearch(
                chatEntity.getMessage(),
                authenticatedUser.getUserId(),
                4
        );
    }

    /**
     * 自动 RAG 问答入口。
     * <p>
     * 先自动检索知识库片段，再复用手动 RAG 问答主流程执行回答。
     */
    private ChatResponseEntity doChatRagSearchAuto(
            ChatEntity chatEntity,
            AuthenticatedUserContext authenticatedUser
    ) {
        List<Document> ragContext = loadRagContext(chatEntity, authenticatedUser);
        return doChatRagSearch(chatEntity, ragContext, authenticatedUser);
    }

    /**
     * 执行 Agent 场景下的自动 RAG 问答。
     * <p>
     * 步骤：
     * 1. 校验 Agent 会话归属；
     * 2. 加载增强数据（RAG/Internet）；
     * 3. 构建提示词并发起流式调用；
     * 4. 把来源一并回填到 assistant 消息与 FINISH 响应中。
     */
    private ChatResponseEntity doAgentRagSearch(
            ChatEntity chatEntity,
            Long chatSessionId,
            Long assistantMessageId,
            Long runId,
            AuthenticatedUserContext authenticatedUser
    ) {
        String sourceType = resolveSourceType(chatEntity);
        ChatSession session = requireExistingSession(chatSessionId, authenticatedUser);

        // 1. 加载增强数据
        EnhancementData data = loadEnhancementData(chatEntity, authenticatedUser);

        // 2. 构建提示词（RAG 模式不需要用户上下文）
        String finalPrompt = buildPromptByEnhancement(chatEntity, data, null);

        // 3. 调用模型
        Prompt prompt = new Prompt(finalPrompt);
        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();

        // 4. 构建来源
        Object sources = buildSourcesByEnhancement(data, chatEntity);

        return streamAndSend(
                stringFlux,
                authenticatedUser,
                chatEntity.getBotMsgId(),
                session.getId(),
                session.getSessionCode(),
                assistantMessageId,
                runId,
                "agent",
                sourceType,
                sources
        );
    }

    /**
     * 预留的 chat hybrid 组合分支。
     * <p>
     * 当前未接入正式主链路；现阶段普通聊天在 RAG 与联网同时开启时会直接抛错。
     */
    private ChatResponseEntity doChatRagInternetSearch(
            ChatEntity chatEntity,
            AuthenticatedUserContext authenticatedUser
    ) {
        String sourceType = resolveSourceType(chatEntity);
        PreparedChatContext ctx = prepareConversation(
                chatEntity,
                authenticatedUser,
                "chat",
                sourceType
        );

        List<Document> ragContext = loadRagContext(chatEntity, authenticatedUser);
        List<SearchResult> searchResults = searXngService.search(chatEntity.getMessage());
        Prompt prompt = new Prompt(buildHybridPrompt(chatEntity.getMessage(), ragContext, searchResults));
        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();

        Object sources = mergeSources(
                buildRagSources(ragContext),
                buildInternetSources(searchResults)
        );

        return streamAndSend(
                stringFlux,
                authenticatedUser,
                chatEntity.getBotMsgId(),
                ctx.getSession().getId(),
                ctx.getSession().getSessionCode(),
                ctx.getAssistantMessageId(),
                "chat",
                sourceType,
                sources
        );
    }

    /**
     * 预留的 agent hybrid 组合分支。
     * <p>
     * 当前未接入正式主链路；现阶段 Agent 执行在 RAG 与联网同时开启时会直接抛错。
     */
    private ChatResponseEntity doAgentRagInternetSearch(
            ChatEntity chatEntity,
            AuthenticatedUserContext authenticatedUser
    ) {
        String sourceType = resolveSourceType(chatEntity);
        PreparedChatContext ctx = prepareConversation(
                chatEntity,
                authenticatedUser,
                "agent",
                sourceType
        );

        List<Document> ragContext = loadRagContext(chatEntity, authenticatedUser);
        List<SearchResult> searchResults = searXngService.search(chatEntity.getMessage());
        Prompt prompt = new Prompt(buildHybridPrompt(chatEntity.getMessage(), ragContext, searchResults));
        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();

        Object sources = mergeSources(
                buildRagSources(ragContext),
                buildInternetSources(searchResults)
        );

        return streamAndSend(
                stringFlux,
                authenticatedUser,
                chatEntity.getBotMsgId(),
                ctx.getSession().getId(),
                ctx.getSession().getSessionCode(),
                ctx.getAssistantMessageId(),
                "agent",
                sourceType,
                sources
        );
    }


    /**
     * 预留的 hybrid 来源合并方法。
     * <p>
     * 当前仅供未接入主链路的组合分支复用，不代表正式聊天流程已经启用双增强合并。
     */
    private Object mergeSources(Object ragSources, Object internetSources) {
        List<Object> merged = new java.util.ArrayList<>();
        if (ragSources instanceof java.util.List<?>) {
            merged.addAll((java.util.List<?>) ragSources);
        }
        if (internetSources instanceof java.util.List<?>) {
            merged.addAll((java.util.List<?>) internetSources);
        }
        return merged;
    }

    /**
     * 预留的 hybrid 提示词组装方法。
     * <p>
     * 当前未接入正式主链路，仅为后续可能的知识库 + 联网组合能力保留。
     */
    private String buildHybridPrompt(
            String question,
            List<Document> ragContext,
            List<SearchResult> searchResults
    ) {
        String ragText = extractRagText(ragContext);
        String internetText = extractInternetText(searchResults);
        return promptTemplateManager.buildHybridPrompt(ragText, internetText, question);
    }
}
