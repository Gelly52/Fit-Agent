package com.itgeo.auth;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itgeo.mapper.UserLoginSessionMapper;
import com.itgeo.mapper.UserMapper;
import com.itgeo.pojo.User;
import com.itgeo.pojo.UserLoginSession;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TokenAuthService {

    @Resource
    private UserLoginSessionMapper userLoginSessionMapper;

    @Resource
    private UserMapper userMapper;

    public AuthenticatedUserContext authenticateToken(String token) {
        if (StrUtil.isBlank(token)) {
            throw new IllegalArgumentException("请先登录后再继续操作");
        }
        UserLoginSession session = userLoginSessionMapper.selectOne(new LambdaQueryWrapper<UserLoginSession>()
                .eq(UserLoginSession::getRefreshTokenHash, SecureUtil.sha256(token))
                .last("limit 1"));
        return buildAuthenticatedUserContext(token, session);
    }

    public AuthenticatedUserContext authenticateSessionId(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("SSE 连接凭证无效，请重新登录后重试");
        }
        UserLoginSession session = userLoginSessionMapper.selectById(sessionId);
        return buildAuthenticatedUserContext(null, session);
    }

    private AuthenticatedUserContext buildAuthenticatedUserContext(String token, UserLoginSession session) {
        UserLoginSession validSession = requireValidSession(session);
        User user = requireValidUser(validSession.getUserId());
        return AuthenticatedUserContext.builder()
                .token(token)
                .sessionId(validSession.getId())
                .userId(user.getId())
                .userKey(user.getUserKey())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .sseClientId(AuthenticatedUserContext.buildSseClientId(validSession.getId()))
                .build();
    }

    private UserLoginSession requireValidSession(UserLoginSession session) {
        if (session == null) {
            throw new IllegalArgumentException("登录状态已失效，请重新登录");
        }
        if (session.getRevokedAt() != null) {
            throw new IllegalArgumentException("登录状态已失效，请重新登录");
        }
        LocalDateTime expiredAt = session.getExpiredAt();
        if (expiredAt == null || !expiredAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("登录状态已过期，请重新登录");
        }
        return session;
    }

    private User requireValidUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("当前用户不存在或已删除");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("当前用户已被禁用");
        }
        return user;
    }
}
