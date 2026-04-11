package com.cartisan.openapi.provider;

import java.util.Set;

/**
 * API Key 信息（缓存对象）。
 *
 * @param appId        应用 ID
 * @param appName      应用名称
 * @param apiSecret    API 密钥
 * @param permissions  权限集合
 * @param status       状态
 */
public record ApiKeyInfo(
        String appId,
        String appName,
        String apiSecret,
        Set<String> permissions,
        String status
) {
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean hasPermission(String requiredPermission) {
        return permissions != null && permissions.contains(requiredPermission);
    }
}
