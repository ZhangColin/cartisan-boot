package com.cartisan.openapi.provider;

/**
 * API Key 信息（缓存对象）。
 *
 * <p>签名 facet 只做机机<strong>认证</strong>（验签 = 调用方是已登记的应用），
 * 不做 per-key ACL——权限粒度的机机鉴权属业务策略，框架不预留（YAGNI）。</p>
 *
 * @param appKey    应用 Key
 * @param appName   应用名称
 * @param apiSecret API 密钥
 * @param status    状态
 */
public record ApiKeyInfo(
        String appKey,
        String appName,
        String apiSecret,
        String status
) {
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
