package com.cartisan.web.resubmit;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的防重复提交锁。
 *
 * <p>使用 Redis 的 SETNX（setIfAbsent）命令实现分布式锁，防止在指定时间窗口内的重复请求。
 * 锁会自动过期，无需手动释放。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * ResubmitLock lock = new ResubmitLock(redisTemplate);
 * String key = lock.generateKey("createUser", "abc123");
 * if (lock.lock(key, 10)) {
 *     // 第一次请求，执行业务逻辑
 * } else {
 *     // 重复请求，拒绝处理
 * }
 * }</pre>
 */
public class ResubmitLock {

    private final StringRedisTemplate redisTemplate;

    /**
     * 创建 ResubmitLock 实例。
     *
     * @param redisTemplate Redis 模板，用于执行分布式锁操作
     */
    public ResubmitLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成 Redis 锁的 key。
     *
     * <p>key 格式为：resubmit:{prefix}:{argsHash}</p>
     *
     * @param prefix 业务前缀，用于区分不同场景
     * @param argsHash 参数哈希，用于标识唯一请求
     * @return Redis key
     */
    public String generateKey(String prefix, String argsHash) {
        return "resubmit:" + prefix + ":" + argsHash;
    }

    /**
     * 尝试获取锁。
     *
     * <p>使用 Redis SETNX + EXPIRE 实现，如果 key 不存在则设置并返回 true，
     * 如果 key 已存在则返回 false。key 会自动过期，无需手动释放。</p>
     *
     * @param key 锁的 key
     * @param delaySeconds 锁的过期时间（秒）
     * @return true 表示加锁成功（第一次请求），false 表示锁已存在（重复请求）
     */
    public boolean lock(String key, int delaySeconds) {
        Boolean absent = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", delaySeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(absent);
    }
}
