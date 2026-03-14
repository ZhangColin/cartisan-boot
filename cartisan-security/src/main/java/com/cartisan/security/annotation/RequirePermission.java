package com.cartisan.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注需要指定权限才能访问的接口。
 * <p>
 * 可作用于类和方法级别。方法注解优先于类注解。
 * 支持多值，满足任一权限即可（OR 逻辑）。
 * <p>
 * 示例：
 * <pre>{@code
 * // 需要 user:create 或 user:update 权限
 * @RequirePermission({"user:create", "user:update"})
 * @PostMapping("/users")
 * public void createUser() { ... }
 * }</pre>
 *
 * @see RequireAuth
 * @see RequireRole
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 权限标识列表，满足任一即可（OR 逻辑）。
     * <p>
     * 权限标识的具体含义由 Sa-Token 的权限体系定义。
     *
     * @return 权限标识数组
     */
    String[] value();
}
