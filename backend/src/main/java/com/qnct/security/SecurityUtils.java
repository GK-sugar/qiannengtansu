package com.qnct.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户解析：统一替代各 Controller 内重复的 SecurityContext 判断，
 * 并给演示环境保留调用方指定的兜底值。
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser lu) {
            return lu;
        }
        return null;
    }

    public static Long userId(Long fallback) {
        LoginUser lu = current();
        return lu != null && lu.getId() != null ? lu.getId() : fallback;
    }

    public static Long orgId(Long fallback) {
        LoginUser lu = current();
        return lu != null && lu.getOrgId() != null ? lu.getOrgId() : fallback;
    }

    public static String realName(String fallback) {
        LoginUser lu = current();
        return lu != null && lu.getRealName() != null ? lu.getRealName() : fallback;
    }
}