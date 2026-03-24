package com.itgeo.auth;

public final class UserContextHolder {

    private static final ThreadLocal<AuthenticatedUserContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(AuthenticatedUserContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static AuthenticatedUserContext get() {
        return CONTEXT_HOLDER.get();
    }

    public static AuthenticatedUserContext getRequired() {
        AuthenticatedUserContext context = get();
        if (context == null) {
            throw new IllegalStateException("当前请求缺少鉴权上下文");
        }
        return context;
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
