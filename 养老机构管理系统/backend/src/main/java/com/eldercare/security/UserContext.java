package com.eldercare.security;

import com.eldercare.entity.SysUser;

/**
 * 用户上下文：把当前登录用户存到 ThreadLocal，
 * 便于在拦截器之后的任意位置（Service、AOP）获取
 */
public class UserContext {

    private static final ThreadLocal<SysUser> HOLDER = new ThreadLocal<>();

    public static void set(SysUser user) {
        HOLDER.set(user);
    }

    public static SysUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户 ID（未登录返回 null）
     */
    public static Long getUserId() {
        SysUser user = HOLDER.get();
        return user == null ? null : user.getId();
    }

    /**
     * 获取当前登录用户角色
     */
    public static String getRole() {
        SysUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}