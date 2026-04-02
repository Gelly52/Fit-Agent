package com.itgeo.bean;

import lombok.Data;

/**
 * 发送登录验证码请求。
 */
@Data
public class UserCodeRequest {
    /** 登录手机号。 */
    private String phone;
}
