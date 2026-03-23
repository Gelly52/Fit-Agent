package com.itgeo.bean;

import lombok.Data;

@Data
public class LoginUserInfo {
    private String id;
    private Long userId;
    private String userKey;
    private String username;
    private String nickname;
    private String phone;
}
