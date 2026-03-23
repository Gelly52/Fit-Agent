package com.itgeo.controller;

import com.itgeo.bean.ChatEntity;
import com.itgeo.service.ChatService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    /**
     * 测试聊天
     *
     * @param chatEntity 聊天实体
     * @return 回复
     */
    @PostMapping("/chatTest")
    public String chatTest(@RequestBody ChatEntity chatEntity) {
        return chatService.chatTest(chatEntity.getMessage());
    }

    @PostMapping("/doChat")
    public LeeResult doChat(@RequestBody ChatEntity chatEntity, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        chatService.doChat(chatEntity);  // 调用 SSE 版本的 doChat
        return LeeResult.ok();
    }


}
