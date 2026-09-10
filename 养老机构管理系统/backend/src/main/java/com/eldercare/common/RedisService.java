package com.eldercare.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 操作封装：统一使用 String 序列化，屏蔽 RedisTemplate 细节。
 *
 * 设计约定：本系统只把"短生命周期、可容忍丢失"的数据放入 Redis
 * （验证码、登录失败计数、登出令牌黑名单），key 均带 TTL，无需清理任务。
 */
@Component
public class RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 写入 key 并设置过期时间
     */
    public void set(String key, String value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 读取 key，不存在返回 null
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除 key
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 判断 key 是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 自增 1：若 key 是新计数（返回 1，即首次失败），同时设置过期时间。
     * 用于登录失败计数——第一次错误开始计时，之后的错误只是累加。
     *
     * @return 自增后的计数值
     */
    public long increment(String key, Duration timeout) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, timeout);
        }
        return count == null ? 0 : count;
    }
}
