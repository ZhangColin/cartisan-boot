package com.cartisan.security.permission;

import java.util.Objects;

/**
 * 权限定义数据类。
 * <p>
 * 由 {@link com.cartisan.security.annotation.RequirePermission} 注解扫描产生。
 * </p>
 *
 * @param code 权限 code，格式：{context}:{module}:{action}
 * @param name 权限显示名称，未填时同 code
 * @param scope 权限作用域，未填时为 null
 */
public record Permission(
    String code,
    String name,
    String scope
) {
    /**
     * 创建 Permission 实例，处理默认值。
     * <p>
     * 空字符串 name → 使用 code；空字符串 scope → 转为 null
     * </p>
     */
    public static Permission of(String code, String name, String scope) {
        String actualName = name == null || name.isBlank() ? code : name;
        String actualScope = (scope == null || scope.isBlank()) ? null : scope;
        return new Permission(code, actualName, actualScope);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
