package com.itgeo.controller;

import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.auth.UserContextHolder;
import com.itgeo.enums.SSEMsgType;
import com.itgeo.service.SseTicketService;
import com.itgeo.utils.SSEServer;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sse")
public class SSEController {

    @Resource
    private SseTicketService sseTicketService;

    @GetMapping(path = "/connect", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public SseEmitter connect(@RequestParam("ticket") String ticket) {
        AuthenticatedUserContext authenticatedUser = sseTicketService.consumeTicket(ticket);
        return SSEServer.connect(authenticatedUser.getSseClientId());
    }

    @GetMapping("/sendMessage")
    public Object sendMessage(@RequestParam String message) {
        SSEServer.sendMsg(UserContextHolder.getRequired().getSseClientId(), message, SSEMsgType.MESSAGE);
        return "success";
    }

    @GetMapping("/sendMessageAdd")
    public Object sendMessageAdd(@RequestParam String message) {
        String sseClientId = UserContextHolder.getRequired().getSseClientId();
        for (int i = 0; i < 10; i++) {
            SSEServer.sendMsg(sseClientId, message, SSEMsgType.ADD);
        }
        return "success";
    }

    @GetMapping("/sendMessageAll")
    public Object sendMessageAll(@RequestParam String message) {
        SSEServer.sendMsgToAllUsers(message);
        return "success";
    }
}
