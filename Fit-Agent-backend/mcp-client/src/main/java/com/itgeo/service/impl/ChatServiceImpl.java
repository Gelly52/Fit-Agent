package com.itgeo.service.impl;

import cn.hutool.json.JSONUtil;
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

    private ChatClient chatClient;

    @Resource
    private SearXngService searXngService;

    // 注入聊天记录内存
    private ChatMemory chatMemory;

    private String systemPrompt =
            "你是一个非常聪明的智能助手，你可以帮我解决很多问题，我为你取一个名字，你的名字是'GoGo'";
    // 你是一个非常聪明的智能助手，你可以回答用户的问题，我为你取一个名字，你的名字是'GoGo'
    //提示词三大类型
    //1. system    系统提示词
    //2. user      用户提示词
    //3. assistant 助手提示词

    //构造器注入，自动配置方式
    public ChatServiceImpl(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(tools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
//                .defaultSystem(systemPrompt)
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
    public void doChat(ChatEntity chatEntity) {
        String userId = chatEntity.getCurrentUserName();
        String prompt = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();

        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();

        List<String> list = stringFlux.toStream().map(chatResponse -> {
            String content = chatResponse.toString();
            SSEServer.sendMsg(userId, content, SSEMsgType.ADD);
            log.info("content: {}", content);
            return content;
        }).collect(Collectors.toList());

        // TODO 这里需要优化，fullContent可以保存到数据库中，用以保留交互内容
        String fullContent = list.stream().collect(Collectors.joining());

        ChatResponseEntity chatResponseEntity = new ChatResponseEntity(fullContent, botMsgId);
        SSEServer.sendMsg(userId, JSONUtil.toJsonStr(chatResponseEntity), SSEMsgType.FINISH);

    }

    // Dify智能体引擎构建平台
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

    @Override
    public void doChatRagSearch(ChatEntity chatEntity, List<Document> ragContext) {
        String userId = chatEntity.getCurrentUserName();
        String question = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();

        // 构建提示词
        String context = null;
        if (ragContext != null && ragContext.size() > 0) {
            context = ragContext.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));
        }

        //组装提示词
        Prompt prompt = new Prompt(RAG_PROMPT_TEMPLATE
                .replace("{context}", context)
                .replace("{question}", question));
        System.out.println(prompt);

        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();

        List<String> list = stringFlux.toStream().map(chatResponse -> {
            String content = chatResponse.toString();
            SSEServer.sendMsg(userId, content, SSEMsgType.ADD);
            log.info("content: {}", content);
            return content;
        }).collect(Collectors.toList());

        // TODO 这里需要优化，fullContent可以保存到数据库中，用以保留交互内容
        String fullContent = list.stream().collect(Collectors.joining());

        ChatResponseEntity chatResponseEntity = new ChatResponseEntity(fullContent, botMsgId);
        SSEServer.sendMsg(userId, JSONUtil.toJsonStr(chatResponseEntity), SSEMsgType.FINISH);

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

    @Override
    public void doInternetSearch(ChatEntity chatEntity) {
        String userId = chatEntity.getCurrentUserName();
        String question = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();

        // 联网搜索
        List<SearchResult> searchResults = searXngService.search(question);

        String finalPrompt = buildInternetPrompt(question, searchResults);

        //组装提示词
        Prompt prompt = new Prompt(finalPrompt);
        System.out.println(prompt);

        Flux<String> stringFlux = chatClient.prompt(prompt).stream().content();

        List<String> list = stringFlux.toStream().map(chatResponse -> {
            String content = chatResponse.toString();
            SSEServer.sendMsg(userId, content, SSEMsgType.ADD);
            log.info("content: {}", content);
            return content;
        }).collect(Collectors.toList());

        // TODO 这里需要优化，fullContent可以保存到数据库中，用以保留交互内容
        String fullContent = list.stream().collect(Collectors.joining());

        ChatResponseEntity chatResponseEntity = new ChatResponseEntity(fullContent, botMsgId);
        SSEServer.sendMsg(userId, JSONUtil.toJsonStr(chatResponseEntity), SSEMsgType.FINISH);

    }

    /**
     * 构建联网搜索上下文提示词
     *
     * @param question
     * @param searchResults
     * @return
     */
    private static String buildInternetPrompt(String question, List<SearchResult> searchResults) {

        // 构建上下文
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

        // format 和 replace(推荐) 方法的区别
        // format 方法是在运行时格式化字符串，而 replace 方法是在编译时替换字符串
        // format 方法可以动态地替换字符串中的占位符，而 replace 方法只能替换固定的字符串
    }
}
