package com.cartisan.openapi.provider;

/**
 * API Key 查询接口。
 */
public interface ApiKeyProvider {
    /**
     * 按 appId 查询 API Key 信息。
     *
     * @param appId 应用 ID
     * @return API Key 信息，不存在返回 null
     */
    ApiKeyInfo getByAppId(String appId);
}
