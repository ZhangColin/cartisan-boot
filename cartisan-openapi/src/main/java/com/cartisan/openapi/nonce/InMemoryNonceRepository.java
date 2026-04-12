package com.cartisan.openapi.nonce;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 ConcurrentHashMap 的 NonceRepository 实现。
 *
 * <p>适用于开发/测试环境或单实例部署场景。不做定时清理，依赖每次调用时的懒淘汰。</p>
 */
public class InMemoryNonceRepository implements NonceRepository {

    private final ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String nonce, Duration ttl) {
        long expireAt = System.currentTimeMillis() + ttl.toMillis();

        Long existing = store.get(nonce);
        if (existing != null && existing > System.currentTimeMillis()) {
            return false;
        }
        if (existing != null) {
            store.remove(nonce, existing);
        }

        Long prev = store.putIfAbsent(nonce, expireAt);
        if (prev != null && prev > System.currentTimeMillis()) {
            return false;
        }
        return true;
    }
}
