package com.itgeo.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itgeo.mapper.ChatMessageMapper;
import com.itgeo.mapper.ChatSessionMapper;
import com.itgeo.pojo.ChatMessage;
import com.itgeo.pojo.ChatSession;
import com.itgeo.service.ChatSessionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 智能体会话与消息持久化服务实现。
 *
 * 职责：
 * 1. 创建 agent 场景聊天会话；
 * 2. 追加用户消息；
 * 3. 创建 assistant 占位消息；
 * 4. 在流式输出完成后回填 assistant 最终内容；
 * 5. 基于 botMsgId 查询消息是否已存在，供后续防重复执行使用。
 *
 * 说明：
 * - 当前实现只负责会话与消息的数据库持久化，不负责大模型调用；
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

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Override
    public ChatSession createAgentSession(Long userId, String firstMessage, String botMsgId) {
        // 1. 基础参数校验：创建 agent 会话时，用户、首条消息、botMsgId 都不能为空
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StrUtil.isBlank(firstMessage)) {
            throw new IllegalArgumentException("首条消息不能为空");
        }
        if (StrUtil.isBlank(botMsgId)) {
            throw new IllegalArgumentException("botMsgId不能为空");
        }

        // 2. 构建会话对象：当前阶段固定落为 agent 场景
        ChatSession session = new ChatSession();
        session.setSessionCode("cs_" + IdUtil.fastSimpleUUID());
        session.setUserId(userId);
        session.setSceneType(DEFAULT_SCENE_TYPE);

        // 3. 使用首条消息生成标题，并记录最近一次机器人消息ID
        session.setTitle(buildTitle(firstMessage));
        session.setLastBotMsgId(botMsgId.trim());

        // 4. 插入数据库并返回带主键的会话对象
        chatSessionMapper.insert(session);
        return session;
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
     *
     * 规则：
     * - 如果当前会话还没有任何消息，则从 1 开始；
     * - 否则取当前最大 seqNo + 1。
     *
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
     *
     * 规则：
     * - 如果未传 sourceType，则默认使用 agent；
     * - 如果有值，则去掉首尾空格后返回。
     *
     * @param sourceType 原始来源类型
     * @return 规范化后的来源类型
     */
    private String normalizeSourceType(String sourceType) {
        return StrUtil.isBlank(sourceType) ? DEFAULT_SOURCE_TYPE : sourceType.trim();
    }

    /**
     * 根据首条消息生成会话标题。
     *
     * 规则：
     * - 去掉首尾空格；
     * - 超过最大长度时截断；
     * - 当前用于生成 t_chat_session.title。
     *
     * @param firstMessage 第一条消息内容
     * @return 规范化后的标题
     */
    private String buildTitle(String firstMessage) {
        String normalized = firstMessage.trim();
        return normalized.length() <= TITLE_MAX_LENGTH
                ? normalized
                : normalized.substring(0, TITLE_MAX_LENGTH);
    }
}
