package com.cartisan.security.permission;

import java.util.List;

/**
 * 权限扫描器接口。
 * <p>
 * 扫描代码中 {@link com.cartisan.security.annotation.RequirePermission} 注解，
 * 返回权限定义列表。
 * </p>
 */
public interface PermissionScanner {

    /**
     * 按 scope 过滤扫描。
     *
     * @param scope 作用域，null 表示只扫描未设置 scope 的权限
     * @return 匹配的权限列表
     */
    List<Permission> scanByScope(String scope);

    /**
     * 扫描全部权限。
     *
     * @return 所有权限列表
     */
    List<Permission> scanAll();
}
