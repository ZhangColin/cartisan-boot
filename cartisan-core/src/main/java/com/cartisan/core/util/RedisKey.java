package com.cartisan.core.util;

/**
 * Redis Key 工具类，统一管理 Redis Key 的前缀和过期时间。
 *
 * <p>此类是不可变的，所有方法都返回新的实例或不可变结果。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 定义带过期时间的 Key
 * private static final RedisKey USER_CACHE_KEY = RedisKey.of("user:cache", 3600);
 *
 * // 使用
 * String key = USER_CACHE_KEY.key(userId);
 * redisTemplate.opsForValue().set(key, value, USER_CACHE_KEY.expireSeconds(), TimeUnit.SECONDS);
 *
 * // 定义永不过期的 Key
 * private static final RedisKey SYSTEM_CONFIG_KEY = RedisKey.permanent("system:config");
 * }</pre>
 */
public final class RedisKey {

    private final String prefix;
    private final long expireSeconds;

    private RedisKey(String prefix, long expireSeconds) {
        this.prefix = prefix;
        this.expireSeconds = expireSeconds;
    }

    /**
     * 生成完整的 Redis Key。
     *
     * @param suffix Key 后缀，通常是业务 ID
     * @return 完整的 Redis Key，格式为 {@code prefix:suffix}
     */
    public String key(String suffix) {
        return prefix + ":" + suffix;
    }

    /**
     * 获取过期时间（秒）。
     *
     * @return 过期时间（秒），0 表示永不过期
     */
    public long expireSeconds() {
        return expireSeconds;
    }

    /**
     * 判断是否为永久 Key。
     *
     * @return true 表示永不过期，false 表示有过期时间
     */
    public boolean isPermanent() {
        return expireSeconds == 0;
    }

    /**
     * 创建带过期时间的 Redis Key。
     *
     * @param prefix Key 前缀
     * @param expireSeconds 过期时间（秒），必须大于 0
     * @return RedisKey 实例
     */
    public static RedisKey of(String prefix, long expireSeconds) {
        return new RedisKey(prefix, expireSeconds);
    }

    /**
     *创建永不过期的 Redis Key。
     *
     * @param prefix Key 前缀
     * @return RedisKey 实例，过期时间为 0
     */
    public static RedisKey permanent(String prefix) {
        return new RedisKey(prefix, 0);
    }
}
