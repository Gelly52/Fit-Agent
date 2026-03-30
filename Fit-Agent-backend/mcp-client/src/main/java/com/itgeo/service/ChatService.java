package com.itgeo.service;

import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.bean.ChatEntity;
import com.itgeo.bean.ChatResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天能力服务。
 *
 * 说明：
 * 1. 正式聊天入口必须显式接收 AuthenticatedUserContext；
 * 2. 服务层禁止再从 ThreadLocal 获取用户，避免异步线程上下文丢失；
 * 3. SSE 推流由实现类在流式消费阶段统一处理。
 */
public interface ChatService {

    /**
     * 测试同步聊天。
     *
     * @param prompt 提示词
     * @return 回复内容
     */
    String chatTest(String prompt);

    /**
     * 测试流式聊天，返回完整 ChatResponse。
     *
     * @param prompt 提示词
     * @return 流式响应
     */
    Flux<ChatResponse> streamResponse(String prompt);

    /**
     * 测试流式聊天，返回字符串分片。
     *
     * @param prompt 提示词
     * @return 文本分片流
     */
    Flux<String> streamStr(String prompt);

    /**
     * 普通流式聊天。
     *
     * @param chatEntity 聊天请求体
     * @param authenticatedUser 当前登录用户上下文
     * @return 最终聊天结果
     */
    ChatResponseEntity doChat(ChatEntity chatEntity, AuthenticatedUserContext authenticatedUser);

    /**
     * 基于手动检索出的 RAG 上下文进行流式聊天。
     *
     * @param chatEntity 聊天请求体
     * @param ragContext RAG 检索结果
     * @param authenticatedUser 当前登录用户上下文
     * @return 最终聊天结果
     */
    ChatResponseEntity doChatRagSearch(
            ChatEntity chatEntity,
            List<Document> ragContext,
            AuthenticatedUserContext authenticatedUser
    );

    /**
     * 基于联网搜索结果增强回答。
     *
     * @param chatEntity 聊天请求体
     * @param authenticatedUser 当前登录用户上下文
     * @return 最终聊天结果
     */
    ChatResponseEntity doInternetSearch(
            ChatEntity chatEntity,
            AuthenticatedUserContext authenticatedUser
    );

    public ChatResponseEntity doAgentChat(
            ChatEntity chatEntity,
            Long chatSessionId,
            Long assistantMessageId,
            AuthenticatedUserContext authenticatedUser
    );

    public ChatResponseEntity doAgentInternetSearch(
            ChatEntity chatEntity,
            Long chatSessionId,
            Long assistantMessageId,
            AuthenticatedUserContext authenticatedUser
    );

    ChatResponseEntity doChatWithEnhancers(
            ChatEntity chatEntity,
            AuthenticatedUserContext authenticatedUser
    );

    ChatResponseEntity doAgentWithEnhancers(
            ChatEntity chatEntity,
            Long chatSessionId,
            Long assistantMessageId,
            AuthenticatedUserContext authenticatedUser
    );



}
