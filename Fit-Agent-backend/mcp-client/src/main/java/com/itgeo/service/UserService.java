package com.itgeo.service;

import com.itgeo.bean.UserLoginResponse;

/**
 * 用户登录服务接口。
 */
public interface UserService {

    /**
     * 发送手机号登录验证码。
     *
     * @param phone 手机号
     */
    void sendCode(String phone);

    /**
     * 使用手机号和验证码完成登录。
     *
     * @param phone 手机号
     * @param code 验证码
     * @param clientIp 客户端 IP
     * @param userAgent 客户端 User-Agent
     * @return 登录响应
     */
    UserLoginResponse login(String phone, String code, String clientIp, String userAgent);

    /**
     * 注销指定令牌对应的登录会话。
     *
     * @param token 登录令牌
     */
    void logout(String token);
}
