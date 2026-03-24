package com.itgeo.service;

import com.itgeo.bean.UserLoginResponse;

public interface UserService {

    void sendCode(String phone);

    UserLoginResponse login(String phone, String code, String clientIp, String userAgent);

    void logout(String token);
}
