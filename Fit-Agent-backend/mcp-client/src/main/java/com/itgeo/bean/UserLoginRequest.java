package com.itgeo.bean;

import lombok.Data;

/**
 * 验证码登录请求。
 */
@Data
public class UserLoginRequest {
    /** 登录手机号。 */
    private String phone;
    /** 短信验证码。 */
    private String code;
}
