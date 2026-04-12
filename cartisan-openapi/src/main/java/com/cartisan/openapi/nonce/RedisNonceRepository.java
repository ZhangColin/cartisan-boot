package com.cartisan.openapi.nonce;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 基于 Redis 的 NonceRepository 实现。
 *
 * <p>使用 {@code SET NX EX} 原子操作防重放，适用于生产环境。</p>
 */
public class RedisNonceRepository implements NonceRepository {

    private static final String KEY_PREFIX = "openapi:nonce:";

    private final StringRedisTemplate redisTemplate;

    public RedisNonceRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String nonce, Duration ttl) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + nonce, "1", ttl);
        return Boolean.TRUE.equals(result);
    }
}
