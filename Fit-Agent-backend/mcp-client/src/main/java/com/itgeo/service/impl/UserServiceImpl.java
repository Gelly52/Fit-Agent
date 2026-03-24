package com.itgeo.service.impl;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itgeo.bean.LoginUserInfo;
import com.itgeo.bean.UserLoginResponse;
import com.itgeo.mapper.UserLoginSessionMapper;
import com.itgeo.mapper.UserMapper;
import com.itgeo.pojo.User;
import com.itgeo.pojo.UserLoginSession;
import com.itgeo.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final String LOGIN_CODE_KEY_PREFIX = "fit-agent:login:code:";
    private static final long LOGIN_CODE_TTL_MINUTES = 5L;
    private static final long LOGIN_SESSION_TTL_DAYS = 7L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserLoginSessionMapper userLoginSessionMapper;
    @Resource
    private Environment environment;

    @Override
    public void sendCode(String phone) {
        validatePhone(phone);
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(
                buildLoginCodeKey(phone),
                code,
                LOGIN_CODE_TTL_MINUTES,
                TimeUnit.MINUTES
        );
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            log.info("发送登录验证码成功，phone={}, code={}", phone, code);
        } else {
            log.info("发送登录验证码成功，phone={}", phone);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginResponse login(String phone, String code, String clientIp, String userAgent) {
        validatePhone(phone);
        if (StrUtil.isBlank(code)) {
            throw new IllegalArgumentException("验证码不能为空");
        }

        String loginCodeKey = buildLoginCodeKey(phone);
        String cacheCode = stringRedisTemplate.opsForValue().get(loginCodeKey);
        if (StrUtil.isBlank(cacheCode) || !StrUtil.equals(cacheCode, code)) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        stringRedisTemplate.delete(loginCodeKey);

        User user = queryUserByPhone(phone);
        boolean isNewUser = false;
        if (user == null) {
            user = createUserWithPhone(phone);
            isNewUser = true;
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("当前用户已被禁用");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);
        userMapper.updateById(user);

        String token = IdUtil.fastSimpleUUID();
        LocalDateTime expiredAt = now.plusDays(LOGIN_SESSION_TTL_DAYS);

        UserLoginSession session = new UserLoginSession();
        session.setUserId(user.getId());
        session.setRefreshTokenHash(SecureUtil.sha256(token));
        session.setClientIp(trimToLength(clientIp, 64));
        session.setUserAgent(trimToLength(userAgent, 255));
        session.setExpiredAt(expiredAt);
        userLoginSessionMapper.insert(session);

        return buildLoginResponse(user, token, expiredAt, isNewUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(String token) {
        if (StrUtil.isBlank(token)) {
            throw new IllegalArgumentException("登录状态已失效");
        }

        UserLoginSession session = userLoginSessionMapper.selectOne(new LambdaQueryWrapper<UserLoginSession>()
                .eq(UserLoginSession::getRefreshTokenHash, buildRefreshTokenHash(token))
                .isNull(UserLoginSession::getRevokedAt)
                .last("limit 1"));
        if (session == null) {
            log.info("退出登录时未匹配到有效会话");
            return;
        }

        session.setRevokedAt(LocalDateTime.now());
        userLoginSessionMapper.updateById(session);
    }

    private User queryUserByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .last("limit 1"));
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setUserKey("u_" + IdUtil.fastSimpleUUID());
        user.setUsername(phone);
        user.setNickname("用户" + lastDigits(phone, 4));
        user.setPhone(phone);
        user.setStatus(1);
        user.setLastLoginAt(LocalDateTime.now());
        try {
            userMapper.insert(user);
            return user;
        } catch (DuplicateKeyException e) {
            log.warn("检测到并发注册，回查已存在用户，phone={}", phone);
            User existingUser = queryUserByPhone(phone);
            if (existingUser != null) {
                return existingUser;
            }
            throw e;
        }
    }

    private UserLoginResponse buildLoginResponse(User user, String token, LocalDateTime expiredAt, boolean isNewUser) {
        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setId(user.getUserKey());
        loginUserInfo.setUserId(user.getId());
        loginUserInfo.setUserKey(user.getUserKey());
        loginUserInfo.setUsername(user.getUsername());
        loginUserInfo.setNickname(user.getNickname());
        loginUserInfo.setPhone(user.getPhone());

        UserLoginResponse response = new UserLoginResponse();
        response.setToken(token);
        response.setExpiresAt(expiredAt.format(DATE_TIME_FORMATTER));
        response.setNewUser(isNewUser);
        response.setUserInfo(loginUserInfo);
        return response;
    }

    private void validatePhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (!Validator.isMobile(phone)) {
            throw new IllegalArgumentException("手机号格式错误");
        }
    }

    private String buildLoginCodeKey(String phone) {
        return LOGIN_CODE_KEY_PREFIX + phone;
    }

    private String buildRefreshTokenHash(String token) {
        return SecureUtil.sha256(token);
    }

    private String lastDigits(String phone, int length) {
        if (phone.length() <= length) {
            return phone;
        }
        return phone.substring(phone.length() - length);
    }

    private String trimToLength(String value, int maxLength) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
