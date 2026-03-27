package com.itgeo.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.auth.UserContextHolder;
import com.itgeo.bean.ChatEntity;
import com.itgeo.bean.ChatResponseEntity;
import com.itgeo.bean.SearchResult;
import com.itgeo.enums.SSEMsgType;
import com.itgeo.service.ChatService;
import com.itgeo.service.SearXngService;
import com.itgeo.utils.SSEServer;
import jakarta.annotation.Resource;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;

    @Resource
    private SearXngService searXngService;

    private ChatMemory chatMemory;

    private String systemPrompt =
            "你是一个非常聪明的智能助手，你可以帮我解决很多问题，我为你取一个名字，你的名字是'GoGo'";
    // TODO 规范化上下文内容，集成到系统提示词文件中

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools, ChatMemory chatMemory) {
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

    @Override
    public void doChat(ChatEntity chatEntity){
        doChat(chatEntity, UserContextHolder.getRequired());
    }

    @Override
    public ChatResponseEntity doChat(ChatEntity chatEntity, AuthenticatedUserContext authenticatedUser) {
        String prompt = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();

        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();
        return streamAndSend(stringFlux, authenticatedUser, botMsgId);
    }


    private static final String RAG_PROMPT_TEMPLATE = """
            基于上下文的知识库内容回答问题：
            【上下文】
            {context}
            
            【问题】
            {question}
            
            【输出】
            如果没有查到相关信息，你只能回答“我没有查到相关信息”。
            如果查到相关信息，你只能根据上下文的信息，来回答用户的问题，不相关的近似内容不必提到。
            """;
    // TODO 规范化上下文内容，集成到系统提示词文件中

    @Override
    public void doChatRagSearch(ChatEntity chatEntity, List<Document> ragContext){
        doChatRagSearch(chatEntity, ragContext, UserContextHolder.getRequired());
    }

    @Override
    public ChatResponseEntity doChatRagSearch(ChatEntity chatEntity, List<Document> ragContext, AuthenticatedUserContext authenticatedUser) {
        String question = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();
        String context = "";
        if (ragContext != null && !ragContext.isEmpty()) {
            context = ragContext.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));
        }
        Prompt prompt = new Prompt(RAG_PROMPT_TEMPLATE
                .replace("{context}", context)
                .replace("{question}", question));

        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();
        return streamAndSend(stringFlux, authenticatedUser, botMsgId);
    }


    private static final String INTERNET_PROMPT_TEMPLATE = """
            你是一个互联网搜索大师，请基于以下联网搜索结果作为上下文，根据你的理解结合用户提问，综合后生成并输出专业的回答。
            基于上下文的知识库内容回答问题：
            【上下文】
            {context}
            
            【问题】
            {question}
            
            【输出】
            如果没有查到相关信息，结合你的理解做简要的阐述（需说明未查询到相关实际来源信息）。
            如果查到相关信息，请回复具体的内容。
            """;
    // TODO 规范化上下文内容，集成到系统提示词文件中

    @Override
    public void doInternetSearch(ChatEntity chatEntity){
        doInternetSearch(chatEntity, UserContextHolder.getRequired());
    }

    @Override
    public ChatResponseEntity doInternetSearch(ChatEntity chatEntity, AuthenticatedUserContext authenticatedUser) {
        String question = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();

        List<SearchResult> searchResults = searXngService.search(question);
        String finalPrompt = buildInternetPrompt(question, searchResults);
        Prompt prompt = new Prompt(finalPrompt);

        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();
        return streamAndSend(stringFlux, authenticatedUser, botMsgId);
    }


    private static String buildInternetPrompt(String question, List<SearchResult> searchResults) {
        StringBuilder context = new StringBuilder();
        searchResults.forEach(searchResult -> {
            context.append(
                    String.format("<context>\n [来源] %s \n [摘要] %s \n </context>\n",
                            searchResult.getUrl(),
                            searchResult.getContent()));
        });

        return INTERNET_PROMPT_TEMPLATE
                .replace("{context}", context)
                .replace("{question}", question);
    }

    private ChatResponseEntity streamAndSend(
            Flux<String> stringFlux,
            AuthenticatedUserContext authenticatedUser,
            String botMsgId){

        String sseClientId = authenticatedUser == null ? null : authenticatedUser.getSseClientId();

        List<String> list = stringFlux.toStream().map(chunk -> {
            String content = chunk == null ? "" : chunk;
            if (StrUtil.isNotBlank(sseClientId) && !content.isEmpty()) {
                SSEServer.sendMsg(sseClientId, content, SSEMsgType.ADD);
            }
            log.info("content: {}", content);
            return content;
        }).collect(Collectors.toList());

        String fullContent = String.join("", list);
        ChatResponseEntity chatResponseEntity = new ChatResponseEntity(fullContent, botMsgId);

        if (StrUtil.isNotBlank(sseClientId)) {
            SSEServer.sendMsg(
                    sseClientId,
                    JSONUtil.toJsonStr(chatResponseEntity),
                    SSEMsgType.FINISH
            );
        }

        return chatResponseEntity;
    }
}
