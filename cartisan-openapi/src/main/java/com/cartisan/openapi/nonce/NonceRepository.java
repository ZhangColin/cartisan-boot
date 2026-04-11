package com.cartisan.openapi.nonce;

import java.time.Duration;

/**
 * Nonce 防重放存储接口。
 *
 * <p>业务方需提供实现（如基于 Redis）。</p>
 */
public interface NonceRepository {
    /**
     * 尝试获取 nonce。若已存在返回 false（重复），否则存储并返回 true。
     *
     * @param nonce nonce 值
     * @param ttl   过期时间
     * @return true 表示获取成功（首次），false 表示已存在（重复）
     */
    boolean tryAcquire(String nonce, Duration ttl);
}
