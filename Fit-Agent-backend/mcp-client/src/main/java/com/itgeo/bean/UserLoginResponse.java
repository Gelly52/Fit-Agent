package com.itgeo.bean;

import lombok.Data;

@Data
public class UserLoginResponse {
    private String token;
    private String expiresAt;
    private Boolean newUser;
    private LoginUserInfo userInfo;
}
