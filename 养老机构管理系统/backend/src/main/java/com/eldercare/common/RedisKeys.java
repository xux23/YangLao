package com.eldercare.common;

/**
 * Redis key 统一维护：验证码、登录失败计数、令牌黑名单
 * （key 设计说明见《系统设计文档》2.2）
 */
public class RedisKeys {

    /** 图形验证码：value = 验证码文本（小写），TTL 5 分钟，校验即删 */
    public static String captchaKey(String captchaId) {
        return "captcha:" + captchaId;
    }

    /** 登录失败计数：value = 连续失败次数，首次失败起 TTL 15 分钟 */
    public static String failKey(String username) {
        return "login:fail:" + username;
    }

    /** 登出令牌黑名单：value = 1，TTL = 令牌剩余有效期 */
    public static String blacklistKey(String token) {
        return "jwt:blacklist:" + token;
    }
}
