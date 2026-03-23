package com.itgeo.bean;

import lombok.Data;

@Data
public class UserLoginRequest {
    private String phone;
    private String code;
}
