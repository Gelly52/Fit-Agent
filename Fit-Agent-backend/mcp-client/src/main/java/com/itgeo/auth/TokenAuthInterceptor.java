package com.itgeo.auth;

import cn.hutool.json.JSONUtil;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TokenAuthInterceptor implements HandlerInterceptor {

    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";

    @Resource
    private TokenAuthService tokenAuthService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        try {
            AuthenticatedUserContext authenticatedUser = tokenAuthService.authenticateToken(request.getHeader("headerUserToken"));
            request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, authenticatedUser);
            UserContextHolder.set(authenticatedUser);
            return true;
        } catch (IllegalArgumentException e) {
            UserContextHolder.clear();
            writeTokenError(response, e.getMessage());
            return false;
        } catch (Exception e) {
            UserContextHolder.clear();
            log.error("统一 token 鉴权失败", e);
            writeTokenError(response, "登录状态校验失败，请重新登录");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    private void writeTokenError(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSONUtil.toJsonStr(LeeResult.errorTokenMsg(message)));
        response.getWriter().flush();
    }
}
