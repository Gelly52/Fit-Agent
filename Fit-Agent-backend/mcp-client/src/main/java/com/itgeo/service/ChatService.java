package com.itgeo.service;

import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.bean.ChatEntity;
import com.itgeo.bean.ChatResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天能力服务契约。
 * <p>
 * 约定：
 * 1. 正式聊天入口必须显式接收 {@link AuthenticatedUserContext}；
 * 2. 接口仅描述能力边界与输入输出约束，不包含具体提示词、SSE 推送或持久化实现细节；
 * 3. 调用方应根据场景选择普通聊天、RAG 增强、联网增强或 Agent 分支入口。
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
     * 测试流式聊天，返回完整 {@link ChatResponse} 分片流。
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
     * 执行普通流式聊天。
     *
     * @param chatEntity 聊天请求体
     * @param authenticatedUser 当前登录用户上下文
     * @return 最终聊天结果
     */
    ChatResponseEntity doChat(ChatEntity chatEntity, AuthenticatedUserContext authenticatedUser);

    /**
     * 基于调用方提供的 RAG 上下文执行流式聊天。
     *
     * @param chatEntity 聊天请求体
     * @param ragContext 已检索出的知识库片段
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

    /**
     * 执行 Agent 场景下的纯模型问答。
     *
     * @param chatEntity 聊天请求体
     * @param chatSessionId 已存在的会话ID
     * @param assistantMessageId 已创建的 assistant 占位消息ID
     * @param runId Agent 运行ID
     * @param authenticatedUser 当前登录用户上下文
     * @return 最终聊天结果
     */
    public ChatResponseEntity doAgentChat(
            ChatEntity chatEntity,
            Long chatSessionId,
            Long assistantMessageId,
            Long runId,
            AuthenticatedUserContext authenticatedUser
    );

    /**
     * 执行 Agent 场景下的联网增强问答。
     *
     * @param chatEntity 聊天请求体
     * @param chatSessionId 已存在的会话ID
     * @param assistantMessageId 已创建的 assistant 占位消息ID
     * @param runId Agent 运行ID
     * @param authenticatedUser 当前登录用户上下文
     * @return 最终聊天结果
     */
    public ChatResponseEntity doAgentInternetSearch(
            ChatEntity chatEntity,
            Long chatSessionId,
            Long assistantMessageId,
            Long runId,
            AuthenticatedUserContext authenticatedUser
    );

    /**
     * 根据请求中的增强开关自动分发正式聊天能力。
     *
     * @param chatEntity 聊天请求体
     * @param authenticatedUser 当前登录用户上下文
     * @return 最终聊天结果
     */
    ChatResponseEntity doChatWithEnhancers(
            ChatEntity chatEntity,
            AuthenticatedUserContext authenticatedUser
    );

    /**
     * 根据请求中的增强开关自动分发 Agent 场景能力。
     *
     * @param chatEntity 聊天请求体
     * @param chatSessionId 已存在的会话ID
     * @param assistantMessageId 已创建的 assistant 占位消息ID
     * @param runId Agent 运行ID
     * @param authenticatedUser 当前登录用户上下文
     * @return 最终聊天结果
     */
    ChatResponseEntity doAgentWithEnhancers(
            ChatEntity chatEntity,
            Long chatSessionId,
            Long assistantMessageId,
            Long runId,
            AuthenticatedUserContext authenticatedUser
    );


}
