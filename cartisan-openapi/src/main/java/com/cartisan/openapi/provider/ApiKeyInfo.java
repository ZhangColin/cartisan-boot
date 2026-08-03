package com.cartisan.openapi.provider;

/**
 * API Key 信息（缓存对象）。
 *
 * <p>签名 facet 只做机机<strong>认证</strong>（验签 = 调用方是已登记的应用），
 * 不做 per-key ACL——权限粒度的机机鉴权属业务策略，框架不预留（YAGNI）。</p>
 *
 * <p>远端返回数据即说明 key 有效，框架不做额外状态判断。</p>
 *
 * @param apiKey    应用 Key
 * @param appName   应用名称
 * @param apiSecret API 密钥
 */
public record ApiKeyInfo(
        String apiKey,
        String appName,
        String apiSecret
) {
}
