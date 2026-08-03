package com.cartisan.openapi.provider;

/**
 * API Key 查询接口。
 */
public interface ApiKeyProvider {
    /**
     * 按 appKey 查询 API Key 信息。
     *
     * @param appKey 应用 Key
     * @return API Key 信息，不存在返回 null
     */
    ApiKeyInfo getByAppKey(String appKey);
}
