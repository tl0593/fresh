package com.fresh.common.util;

import com.fresh.common.dto.UserContextDTO;

public final class ContextUtil {

    private static final ThreadLocal<UserContextDTO> CONTEXT = new ThreadLocal<>();

    private ContextUtil() {
    }

    public static void set(UserContextDTO context) {
        CONTEXT.set(context);
    }

    public static UserContextDTO get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        UserContextDTO ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getUserId();
    }

    public static Long getAdminId() {
        UserContextDTO ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getAdminId();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
