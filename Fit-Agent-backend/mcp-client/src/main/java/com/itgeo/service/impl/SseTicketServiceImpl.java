package com.itgeo.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.auth.TokenAuthService;
import com.itgeo.bean.SseTicketResponse;
import com.itgeo.service.SseTicketService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SseTicketServiceImpl implements SseTicketService {

    private static final String SSE_TICKET_KEY_PREFIX = "fit-agent:sse:ticket:";
    private static final long SSE_TICKET_TTL_SECONDS = 60L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private TokenAuthService tokenAuthService;

    @Override
    public SseTicketResponse createTicket(AuthenticatedUserContext authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getSessionId() == null) {
            throw new IllegalArgumentException("登录状态已失效，请重新登录");
        }
        String ticket = IdUtil.fastSimpleUUID();
        stringRedisTemplate.opsForValue().set(
                buildTicketKey(ticket),
                String.valueOf(authenticatedUser.getSessionId()),
                SSE_TICKET_TTL_SECONDS,
                TimeUnit.SECONDS
        );
        return new SseTicketResponse(ticket, SSE_TICKET_TTL_SECONDS);
    }

    @Override
    public AuthenticatedUserContext consumeTicket(String ticket) {
        if (StrUtil.isBlank(ticket)) {
            throw new IllegalArgumentException("SSE 连接票据不能为空");
        }
        String ticketKey = buildTicketKey(ticket);
        String sessionIdValue = stringRedisTemplate.opsForValue().get(ticketKey);
        if (StrUtil.isBlank(sessionIdValue)) {
            throw new IllegalArgumentException("SSE 连接票据无效或已过期，请重新获取");
        }
        stringRedisTemplate.delete(ticketKey);
        try {
            return tokenAuthService.authenticateSessionId(Long.valueOf(sessionIdValue));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("SSE 连接票据数据异常，请重新获取");
        }
    }

    private String buildTicketKey(String ticket) {
        return SSE_TICKET_KEY_PREFIX + ticket;
    }
}
