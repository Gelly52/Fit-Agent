package com.itgeo.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itgeo.bean.ChatRecordItem;
import com.itgeo.bean.ChatRecordsResponse;
import com.itgeo.bean.ChatSessionRecordItem;
import com.itgeo.mapper.ChatMessageMapper;
import com.itgeo.mapper.ChatSessionMapper;
import com.itgeo.pojo.ChatMessage;
import com.itgeo.pojo.ChatSession;
import com.itgeo.service.ChatSessionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天会话与消息持久化服务实现。
 *
 * 职责：
 * 1. 创建或复用聊天会话；
 * 2. 追加用户消息；
 * 3. 创建 assistant 占位消息；
 * 4. 在流式输出完成后回填 assistant 最终内容；
 * 5. 为前端提供聊天历史查询能力。
 *
 * 说明：
 * - 当前实现只负责会话与消息的数据库持久化，不负责大模型调用；
 * - sceneType 当前只区分 agent 与 chat；
 * - sourceType 未传时默认使用 agent；
 * - seqNo 在单会话内按最大值递增；
 * - assistant 占位消息 content 必须写空字符串，不能写 null。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ChatSessionServiceImpl implements ChatSessionService {

    private static final String DEFAULT_SCENE_TYPE = "agent";
    private static final String DEFAULT_SOURCE_TYPE = "agent";
    private static final String DEFAULT_MESSAGE_TYPE = "text";
    private static final String USER_ROLE = "user";
    private static final String ASSISTANT_ROLE = "assistant";
    private static final int TITLE_MAX_LENGTH = 50;

    private static final int DEFAULT_QUERY_LIMIT = 20;
    private static final int MAX_QUERY_LIMIT = 50;

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Override
    public ChatSession createAgentSession(Long userId, String firstMessage, String botMsgId) {
        return createSession(userId, DEFAULT_SCENE_TYPE, firstMessage, botMsgId);
    }

    @Override
    public Long appendUserMessage(Long sessionId, String content, String sourceType) {
        // 1. 必须先校验会话存在，避免脏写消息
        validateSessionId(sessionId);
        // 2. 用户消息不能为空白
        if (StrUtil.isBlank(content)) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        // 3. 组装用户消息：role 固定为 user，messageType 固定为 text
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSeqNo(nextSeqNo(sessionId));
        message.setRole(USER_ROLE);
        message.setMessageType(DEFAULT_MESSAGE_TYPE);
        message.setSourceType(normalizeSourceType(sourceType));
        message.setContent(content.trim());

        // 4. 插入数据库后返回消息主键，供后续链路使用
        chatMessageMapper.insert(message);
        return message.getId();
    }

    @Override
    public Long createAssistantPlaceholder(Long sessionId, String botMsgId, String sourceType) {
        // 1. assistant 占位消息必须依附于已存在的会话，且必须有 botMsgId
        validateSessionId(sessionId);
        if (StrUtil.isBlank(botMsgId)) {
            throw new IllegalArgumentException("机器人消息ID不能为空");
        }

        // 2. 创建 assistant 占位消息：
        //    - role 固定为 assistant
        //    - content 先写空字符串
        //    - botMsgId 用于前端消息关联与幂等识别
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSeqNo(nextSeqNo(sessionId));
        message.setRole(ASSISTANT_ROLE);
        message.setMessageType(DEFAULT_MESSAGE_TYPE);
        message.setSourceType(normalizeSourceType(sourceType));
        message.setBotMsgId(botMsgId.trim());
        message.setContent("");

        // 3. 先写入占位消息
        chatMessageMapper.insert(message);

        // 4. 同步刷新会话表里的 lastBotMsgId，便于快速定位最近一条机器人消息
        ChatSession sessionUpdate = new ChatSession();
        sessionUpdate.setId(sessionId);
        sessionUpdate.setLastBotMsgId(botMsgId.trim());
        chatSessionMapper.updateById(sessionUpdate);

        // 5. 返回 assistant 消息主键，后续 finish 阶段通过该主键回填最终内容
        return message.getId();
    }

    @Override
    public void finishAssistantMessage(Long messageId, String content, String sourcesJson) {
        // 1. messageId 是回填最终 assistant 消息的唯一定位条件
        if (messageId == null) {
            throw new IllegalArgumentException("messageId不能为空");
        }

        // 2. 仅更新最终消息内容与来源 JSON
        ChatMessage update = new ChatMessage();
        update.setId(messageId);
        update.setContent(content == null ? "" : content);
        update.setSourcesJson(StrUtil.isBlank(sourcesJson) ? null : sourcesJson.trim());

        // 3. updateById 会触发 t_chat_message.updated_at 自动刷新
        chatMessageMapper.updateById(update);
    }

    @Override
    public boolean existsByBotMsgId(String botMsgId) {
        // 空 botMsgId 不参与去重判断
        if (StrUtil.isBlank(botMsgId)) {
            return false;
        }

        // 注意：这里查的是消息表，而不是 session.lastBotMsgId，
        // 否则无法覆盖历史消息记录，只能判断“最近一条”。
        ChatMessage existing = chatMessageMapper.selectOne(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getBotMsgId, botMsgId.trim())
                        .last("limit 1")
        );
        return existing != null;
    }

    @Override
    public List<ChatSession> listRecentSessions(Long userId, Integer limit) {
        // 1. userId 不能为空
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        // 2. limit 调用私有方法：
        int safeLimit = normalizeQueryLimit(limit);

        // 3. 查询当前用户最近会话
        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdatedAt)
                        .last("limit " + safeLimit)
        );
        return sessions;
    }

    @Override
    public List<ChatMessage> listMessagesBySessionId(Long userId, Long sessionId) {
        // 1.校验 userId
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }

        // 2.校验 sessionId
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId不能为空");
        }

        // 3. 先查询 session 是否存在且属于当前用户
        ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getId, sessionId)
                        .eq(ChatSession::getUserId, userId)
                        .last("limit 1")
        );

        // 4. 如果查不到，返回空列表
        if (session == null) {
            return List.of();
        }

        // 5. 查询消息列表
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getSeqNo)
        );
        // 6. 返回消息列表
        return messages;
    }

    @Override
    public ChatRecordsResponse getChatRecords(Long userId, Long sessionId, Integer limit) {
        // 1. 校验 userId
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        // 2. 创建响应对象
        ChatRecordsResponse response = new ChatRecordsResponse();
        response.setUserId(userId);
        // 3. 如果传了 sessionId，只返回这个会话
        if (sessionId != null) {
            ChatSession session = chatSessionMapper.selectOne(
                    new LambdaQueryWrapper<ChatSession>()
                            .eq(ChatSession::getId, sessionId)
                            .eq(ChatSession::getUserId, userId)
                            .last("limit 1")
            );

            // 3.1 当前用户查不到这个会话，直接返回空
            if (session == null) {
                response.setTotalSessions(0);
                response.setSessions(List.of());
                return response;
            }

            // 3.2 查询这个会话下的消息
            List<ChatMessage> messages = listMessagesBySessionId(userId, sessionId);

            // 3.3 组装单个会话响应
            ChatSessionRecordItem sessionItem = buildSessionRecordItem(session, messages);

            response.setTotalSessions(1);
            response.setSessions(List.of(sessionItem));
            return response;
        }

        // 4. 如果没传 sessionId，查询最近会话列表
        List<ChatSession> sessions = listRecentSessions(userId, limit);

        // 4.1 没有任何会话，返回空
        if (sessions == null || sessions.isEmpty()) {
            response.setTotalSessions(0);
            response.setSessions(List.of());
            return response;
        }

        // 4.2 逐个会话查消息并组装
        List<ChatSessionRecordItem> sessionItems = sessions.stream()
                .map(session -> buildSessionRecordItem(
                        session,
                        listMessagesBySessionId(userId, session.getId())
                ))
                .collect(Collectors.toList());

        // 4.3 返回结果
        response.setTotalSessions(sessionItems.size());
        response.setSessions(sessionItems);
        return response;
    }

    @Override
    public ChatSession findByUserIdAndSessionCode(Long userId, String sessionCode) {
        if (userId == null || StrUtil.isBlank(sessionCode)) {
            return null;
        }

        return chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .eq(ChatSession::getSessionCode, sessionCode.trim())
                        .last("limit 1")
        );
    }

    @Override
    public ChatSession createSession(Long userId, String sceneType, String firstMessage, String botMsgId) {
        // 1. 基础参数校验：创建会话时，用户、首条消息、botMsgId 都不能为空
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StrUtil.isBlank(firstMessage)) {
            throw new IllegalArgumentException("首条消息不能为空");
        }
        if (StrUtil.isBlank(botMsgId)) {
            throw new IllegalArgumentException("botMsgId不能为空");
        }

        // 2. 构建会话对象：sceneType 只允许 agent / chat 两种规范值
        ChatSession session = new ChatSession();
        session.setSessionCode("cs_" + IdUtil.fastSimpleUUID());
        session.setUserId(userId);
        session.setSceneType(normalizeSessionSceneType(sceneType));

        // 3. 使用首条消息生成标题，并记录最近一次机器人消息ID
        session.setTitle(buildTitle(firstMessage));
        session.setLastBotMsgId(botMsgId.trim());

        // 4. 插入数据库并返回带主键的会话对象
        chatSessionMapper.insert(session);
        return session;
    }

    @Override
    public ChatSession resolveOrCreateSession(Long userId, String sessionCode, String sceneType, String firstMessage, String botMsgId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }

        if (StrUtil.isNotBlank(sessionCode)) {
            ChatSession existing = findByUserIdAndSessionCode(userId, sessionCode);
            if (existing != null) {
                if (isSceneCompatible(sceneType, existing.getSceneType())) {
                    return existing;
                }
                // 模式不兼容：新建一个新会话
                return createSession(userId, sceneType, firstMessage, botMsgId);
            }
        }

        return createSession(userId, sceneType, firstMessage, botMsgId);
    }

    @Override
    public ChatSession findByIdAndUserId(Long sessionId, Long userId) {
        if (sessionId == null || userId == null) {
            return null;
        }
        return chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getId, sessionId)
                        .eq(ChatSession::getUserId, userId)
                        .last("limit 1")
        );
    }

    private void validateSessionId(Long sessionId) {
        // 1. sessionId 不能为空
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId不能为空");
        }
        // 2. 必须保证会话真实存在，避免向不存在的 session 写消息
        if (chatSessionMapper.selectById(sessionId) == null) {
            throw new IllegalArgumentException("会话不存在");
        }
    }

    /**
     * 计算当前会话的下一条消息序号。
     * 规则：
     * - 如果当前会话还没有任何消息，则从 1 开始；
     * - 否则取当前最大 seqNo + 1。
     * 说明：
     * - 当前实现适用于 Phase 1 串行写消息场景；
     * - 如果后续同一会话出现高并发写入，需要进一步考虑 seqNo 并发控制。
     *
     * @param sessionId 会话ID
     * @return 下一条消息序号
     */
    private Integer nextSeqNo(Long sessionId) {
        // 查询当前会话下 seqNo 最大的一条消息
        ChatMessage lastMessage = chatMessageMapper.selectOne(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByDesc(ChatMessage::getSeqNo)
                        .last("limit 1")
        );

        // 如果当前没有历史消息，则从 1 开始；否则在最大序号基础上加 1
        return lastMessage == null || lastMessage.getSeqNo() == null
                ? 1
                : lastMessage.getSeqNo() + 1;
    }

    /**
     * 规范化消息来源类型。
     * 规则：
     * - 如果未传 sourceType，则默认使用 agent；
     * - 如果有值，则去掉首尾空格后返回。
     * @param sourceType 原始来源类型
     * @return 规范化后的来源类型
     */
    private String normalizeSourceType(String sourceType) {
        return StrUtil.isBlank(sourceType) ? DEFAULT_SOURCE_TYPE : sourceType.trim();
    }

    /**
     * 根据首条消息生成会话标题。
     * 规则：
     * - 去掉首尾空格；
     * - 超过最大长度时截断；
     * - 当前用于生成 t_chat_session.title。
     * @param firstMessage 第一条消息内容
     * @return 规范化后的标题
     */
    private String buildTitle(String firstMessage) {
        String normalized = firstMessage.trim();
        return normalized.length() <= TITLE_MAX_LENGTH
                ? normalized
                : normalized.substring(0, TITLE_MAX_LENGTH);
    }

    private int normalizeQueryLimit(Integer limit){
        if (limit == null || limit <= 0) {
            return DEFAULT_QUERY_LIMIT;
        }
        return Math.min(limit, MAX_QUERY_LIMIT);
    }

    private ChatSessionRecordItem buildSessionRecordItem(ChatSession session, List<ChatMessage> messages){
        ChatSessionRecordItem item = new ChatSessionRecordItem();
        item.setSessionId(session.getId());
        item.setSessionCode(session.getSessionCode());
        item.setSceneType(session.getSceneType());
        item.setTitle(session.getTitle());
        item.setLastBotMsgId(session.getLastBotMsgId());
        item.setCreatedAt(session.getCreatedAt());
        item.setUpdatedAt(session.getUpdatedAt());

        item.setMessages(messages.stream().map(message -> buildChatRecordItem(session, message))
                .collect(Collectors.toList()));

        return item;
    }

    private ChatRecordItem buildChatRecordItem(ChatSession session, ChatMessage message){
        ChatRecordItem item = new ChatRecordItem();
        item.setSessionId(session.getId());
        item.setSessionCode(session.getSessionCode());
        item.setSceneType(session.getSceneType());
        item.setMessageId(message.getId());
        item.setSeqNo(message.getSeqNo());
        item.setRole(message.getRole());
        item.setMessageType(message.getMessageType());
        item.setSourceType(message.getSourceType());
        item.setContent(message.getContent());
        item.setBotMsgId(message.getBotMsgId());
        item.setSourcesJson(message.getSourcesJson());
        item.setCreatedAt(message.getCreatedAt());
        return item;
    }

    /**
     * 规范化会话场景类型。
     * 规则：
     * - 如果未传 sceneType，则默认使用 chat；
     * - 如果有值，则去掉首尾空格后返回。
     * @param sceneType 原始场景类型
     * @return 规范化后的场景类型
     */
    private String normalizeSessionSceneType(String sceneType) {
        if ("agent".equalsIgnoreCase(sceneType)) {
            return "agent";
        }
        return "chat";
    }

    /**
     * 检查会话场景类型是否兼容。
     * 规则：
     * - 如果请求场景类型为 agent，则仅兼容 agent；
     * - 如果请求场景类型为 chat，则兼容 chat。
     * @param requestSceneType 请求场景类型
     * @param existingSceneType 存在的场景类型
     * @return 是否兼容
     */
    private boolean isSceneCompatible(String requestSceneType, String existingSceneType) {
        String req = normalizeSessionSceneType(requestSceneType);
        String existing = normalizeSessionSceneType(existingSceneType);

        if ("agent".equals(req)) {
            return "agent".equals(existing);
        }
        return "chat".equals(existing);
    }
}
