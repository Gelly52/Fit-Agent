package com.itgeo.controller;

import com.itgeo.enums.SSEMsgType;
import com.itgeo.service.ChatService;
import com.itgeo.utils.SSEServer;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.awt.*;

@RestController
@RequestMapping("/sse")
public class SSEController {


    /**
     * 前端发送连接的请求，连接SSE服务
     *
     * @return
     */
    @GetMapping(path = "/connect", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public SseEmitter connect(@RequestParam("userId") String userId) {
        return SSEServer.connect(userId);
    }

    /**
     * SSE发送单个消息
     *
     * @return
     */
    @GetMapping("/sendMessage")
    public Object sendMessage(@RequestParam("userId") String userId, @RequestParam String message) {
        SSEServer.sendMsg(userId, message, SSEMsgType.MESSAGE);
        return "success";
    }

    /**
     * SSE发送添加消息
     *
     * @return
     */
    @GetMapping("/sendMessageAdd")
    public Object sendMessageAdd(@RequestParam("userId") String userId, @RequestParam String message) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            SSEServer.sendMsg(userId, message, SSEMsgType.ADD);
        }
        return "success";
    }

    /**
     * SSE群发消息
     *
     * @return
     */
    @GetMapping("/sendMessageAll")
    public Object sendMessageAll(@RequestParam String message) {
        SSEServer.sendMsgToAllUsers(message);
        return "success";
    }


}


