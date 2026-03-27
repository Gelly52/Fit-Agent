package com.itgeo.service;

import com.itgeo.pojo.ChatSession;

public interface ChatSessionService {
    /**
     * 创建智能会话
     * @param userId 用户ID
     * @param firstMessage 第一条消息
     * @param botMsgId 机器人消息ID
     * @return 会话对象
     */
    ChatSession createAgentSession(Long userId, String firstMessage, String botMsgId);

    /**
     * 追加用户消息
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param sourceType 消息来源类型
     * @return 消息ID
     */
    Long appendUserMessage(Long sessionId, String content, String sourceType);

    /**
     * 创建智能助手占位符
     * @param sessionId 会话ID
     * @param botMsgId 机器人消息ID
     * @param sourceType 消息来源类型
     * @return 消息ID
     */
    Long createAssistantPlaceholder(Long sessionId, String botMsgId, String sourceType);

    /**
     * 完成智能助手消息
     * @param messageId 消息ID
     * @param content 消息内容
     * @param sourcesJson 消息来源JSON字符串
     */
    void finishAssistantMessage(Long messageId, String content, String sourcesJson);

    /**
     * 检查是否存在指定机器人消息ID的会话
     * @param botMsgId 机器人消息ID
     * @return 是否存在
     */
    boolean existsByBotMsgId(String botMsgId);
}
