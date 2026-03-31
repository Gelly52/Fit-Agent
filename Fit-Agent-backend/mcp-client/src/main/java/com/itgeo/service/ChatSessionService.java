package com.itgeo.service;

import com.itgeo.bean.ChatRecordsResponse;
import com.itgeo.pojo.ChatMessage;
import com.itgeo.pojo.ChatSession;

import java.util.List;

/**
 * 聊天会话与消息持久化服务。
 *
 * 说明：
 * 1. 负责会话创建/复用、消息追加、assistant 占位回填；
 * 2. 负责为前端提供聊天历史查询能力；
 * 3. 不负责大模型调用，只处理数据库持久化。
 */
public interface ChatSessionService {
    /**
     * 创建 agent 场景新会话。
     *
     * @param userId 用户ID
     * @param firstMessage 第一条消息
     * @param botMsgId 机器人消息ID
     * @return 会话对象
     */
    ChatSession createAgentSession(Long userId, String firstMessage, String botMsgId);

    /**
     * 追加一条用户消息。
     *
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param sourceType 消息来源类型
     * @return 消息ID
     */
    Long appendUserMessage(Long sessionId, String content, String sourceType);

    /**
     * 创建 assistant 占位消息。
     *
     * @param sessionId 会话ID
     * @param botMsgId 机器人消息ID
     * @param sourceType 消息来源类型
     * @return 消息ID
     */
    Long createAssistantPlaceholder(Long sessionId, String botMsgId, String sourceType);

    /**
     * 回填 assistant 最终消息内容与来源。
     *
     * @param messageId 消息ID
     * @param content 消息内容
     * @param sourcesJson 消息来源 JSON 字符串
     */
    void finishAssistantMessage(Long messageId, String content, String sourcesJson);

    /**
     * 检查是否存在指定 botMsgId 的消息记录。
     *
     * @param botMsgId 机器人消息ID
     * @return 是否存在
     */
    boolean existsByBotMsgId(String botMsgId);

    /**
     * 查询当前用户最近会话列表。
     */
    List<ChatSession> listRecentSessions(Long userId, Integer limit);

    /**
     * 查询当前用户指定会话下的消息列表。
     */
    List<ChatMessage> listMessagesBySessionId(Long userId, Long sessionId);

    /**
     * 查询当前用户聊天历史。
     */
    ChatRecordsResponse getChatRecords(Long userId, Long sessionId, Integer limit);

    /**
     * 按 userId + sessionCode 查找会话。
     */
    ChatSession findByUserIdAndSessionCode(Long userId, String sessionCode);

    /**
     * 创建新会话。
     *
     * @param sceneType 会话场景类型，当前仅区分 agent / chat
     */
    ChatSession createSession(
            Long userId,
            String sceneType,
            String firstMessage,
            String botMsgId
    );

    /**
     * 按 sessionCode 复用会话，不存在或场景不兼容时新建。
     */
    ChatSession resolveOrCreateSession(
            Long userId,
            String sessionCode,
            String sceneType,
            String firstMessage,
            String botMsgId
    );

    /**
     * 按会话主键和用户主键查询会话。
     */
    ChatSession findByIdAndUserId(Long sessionId, Long userId);

    /**
     * 检查当前用户是否存在指定 botMsgId 的消息记录。
     * @param userId 用户ID
     * @param botMsgId 机器人消息ID
     * @return 是否存在
     */
    boolean existsByUserIdAndBotMsgId(Long userId, String botMsgId);
}
