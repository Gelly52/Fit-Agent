package com.itgeo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUserContext {

    private String token;
    private Long sessionId;
    private Long userId;
    private String userKey;
    private String username;
    private String nickname;
    private String phone;
    private String sseClientId;

    public static String buildSseClientId(Long sessionId) {
        return sessionId == null ? null : "session:" + sessionId;
    }
}
