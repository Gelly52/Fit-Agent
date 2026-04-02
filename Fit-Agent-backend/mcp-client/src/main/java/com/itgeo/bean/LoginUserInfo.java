package com.itgeo.bean;

import lombok.Data;

/**
 * 登录用户信息。
 */
@Data
public class LoginUserInfo {
    /** 兼容字段，当前等同于 userKey。 */
    private String id;
    /** 用户主键。 */
    private Long userId;
    /** 对外稳定用户标识。 */
    private String userKey;
    /** 用户名。 */
    private String username;
    /** 用户昵称。 */
    private String nickname;
    /** 手机号。 */
    private String phone;
}
