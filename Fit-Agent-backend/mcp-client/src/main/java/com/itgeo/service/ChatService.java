package com.itgeo.service;

import com.itgeo.bean.ChatEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {

    /**
     * 测试聊天
     *
     * @param prompt 提示词
     * @return 回复
     */
    public String chatTest(String prompt);

    /**
     * 测试流式聊天，返回JSON
     *
     * @param prompt 提示词
     * @return 回复
     */
    public Flux<ChatResponse> streamResponse(String prompt);

    /**
     * 测试流式聊天，返回String字符串
     *
     * @param prompt 提示词
     * @return 回复
     */
    public Flux<String> streamStr(String prompt);

    /**
     * 流式聊天，与大模型交互
     *
     * @param chatEntity 聊天实体
     */
    public void doChat(ChatEntity chatEntity);


    /**
     * 流式聊天，与大模型交互，同时进行RAG知识库检索，汇总给大模型输出
     *
     * @param chatEntity 聊天实体
     * @param ragContext RAG知识库上下文
     */
    public void doChatRagSearch(ChatEntity chatEntity, List<Document> ragContext);

     /**
      * 基于SearXng的实时联网搜索，将搜索结果添加到聊天实体中
      *
      * @param chatEntity 聊天实体
      * @return 搜索结果
      */
    public void doInternetSearch(ChatEntity chatEntity);

}
