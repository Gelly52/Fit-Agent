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

/**
 * 用户登录服务实现，负责验证码登录与登录会话维护。
 */
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

    /**
     * 发送登录验证码。
     * 处理流程：
     * 1. 校验手机号；
     * 2. 生成验证码并写入 Redis；
     * 3. 按运行环境输出日志。
     */
    @Override
    public void sendCode(String phone) {
        // 校验手机号格式。
        validatePhone(phone);

        // 生成验证码并写入 Redis，使用固定 TTL 控制有效期。
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(
                buildLoginCodeKey(phone),
                code,
                LOGIN_CODE_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        // 开发环境打印验证码，其他环境只记录手机号，避免敏感信息泄露。
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            log.info("发送登录验证码成功，phone={}, code={}", phone, code);
        } else {
            log.info("发送登录验证码成功，phone={}", phone);
        }
    }

    /**
     * 验证码登录。
     * 处理流程：
     * 1. 校验手机号与验证码参数；
     * 2. 校验并消费缓存验证码；
     * 3. 查询或创建用户并检查账号状态；
     * 4. 更新登录时间并创建登录会话；
     * 5. 组装并返回登录结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginResponse login(String phone, String code, String clientIp, String userAgent) {
        // 校验手机号和验证码参数。
        validatePhone(phone);
        if (StrUtil.isBlank(code)) {
            throw new IllegalArgumentException("验证码不能为空");
        }

        // 校验缓存验证码并在成功后立即删除，避免重复使用。
        String loginCodeKey = buildLoginCodeKey(phone);
        String cacheCode = stringRedisTemplate.opsForValue().get(loginCodeKey);
        if (StrUtil.isBlank(cacheCode) || !StrUtil.equals(cacheCode, code)) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        stringRedisTemplate.delete(loginCodeKey);

        // 查询或创建用户，并校验账号状态是否允许登录。
        User user = queryUserByPhone(phone);
        boolean isNewUser = false;
        if (user == null) {
            user = createUserWithPhone(phone);
            isNewUser = true;
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("当前用户已被禁用");
        }

        // 更新最近登录时间，创建新的登录会话并生成返回令牌。
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

        // 组装并返回登录响应。
        return buildLoginResponse(user, token, expiredAt, isNewUser);
    }

    /**
     * 退出登录。
     * 处理流程：
     * 1. 校验 token 并查询有效会话；
     * 2. 找到会话则标记失效，未找到则直接返回。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(String token) {
        // 校验令牌是否为空。
        if (StrUtil.isBlank(token)) {
            throw new IllegalArgumentException("登录状态已失效");
        }

        // 查询当前令牌对应的有效会话，存在则标记撤销。
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

    /**
     * 按手机号查询用户。
     */
    private User queryUserByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .last("limit 1"));
    }

    /**
     * 按手机号创建用户，并在并发注册时回查已存在用户。
     */
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

    /**
     * 组装登录响应对象。
     */
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

    /**
     * 校验手机号非空且格式合法。
     */
    private void validatePhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (!Validator.isMobile(phone)) {
            throw new IllegalArgumentException("手机号格式错误");
        }
    }

    /**
     * 构建验证码缓存键。
     */
    private String buildLoginCodeKey(String phone) {
        return LOGIN_CODE_KEY_PREFIX + phone;
    }

    /**
     * 计算刷新令牌哈希。
     */
    private String buildRefreshTokenHash(String token) {
        return SecureUtil.sha256(token);
    }

    /**
     * 截取字符串尾部指定长度。
     */
    private String lastDigits(String phone, int length) {
        if (phone.length() <= length) {
            return phone;
        }
        return phone.substring(phone.length() - length);
    }

    /**
     * 空白转空并按最大长度截断。
     */
    private String trimToLength(String value, int maxLength) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
