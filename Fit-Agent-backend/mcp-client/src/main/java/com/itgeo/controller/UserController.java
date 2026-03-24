package com.itgeo.controller;

import com.itgeo.auth.UserContextHolder;
import com.itgeo.bean.UserCodeRequest;
import com.itgeo.bean.UserLoginRequest;
import com.itgeo.service.SseTicketService;
import com.itgeo.service.UserService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private SseTicketService sseTicketService;

    @PostMapping("/code")
    public LeeResult sendCode(@RequestBody UserCodeRequest request) {
        try {
            userService.sendCode(request == null ? null : request.getPhone());
            return LeeResult.ok();
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("发送验证码失败", e);
            return LeeResult.errorException("发送验证码失败");
        }
    }

    @PostMapping("/login")
    public LeeResult login(@RequestBody UserLoginRequest request, HttpServletRequest httpServletRequest) {
        try {
            return LeeResult.ok(userService.login(
                    request == null ? null : request.getPhone(),
                    request == null ? null : request.getCode(),
                    resolveClientIp(httpServletRequest),
                    httpServletRequest.getHeader("User-Agent")
            ));
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("手机号登录失败", e);
            return LeeResult.errorException("登录失败");
        }
    }

    @PostMapping("/logout")
    public LeeResult logout(HttpServletRequest httpServletRequest) {
        try {
            userService.logout(httpServletRequest.getHeader("headerUserToken"));
            return LeeResult.ok();
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("退出登录失败", e);
            return LeeResult.errorException("退出登录失败");
        }
    }

    @PostMapping("/sse-ticket")
    public LeeResult createSseTicket() {
        try {
            return LeeResult.ok(sseTicketService.createTicket(UserContextHolder.getRequired()));
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("创建 SSE 连接票据失败", e);
            return LeeResult.errorException("创建 SSE 连接票据失败");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
