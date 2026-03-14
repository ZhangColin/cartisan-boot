package com.cartisan.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注需要指定角色才能访问的接口。
 * <p>
 * 可作用于类和方法级别。方法注解优先于类注解。
 * 支持多值，满足任一角色即可（OR 逻辑）。
 * <p>
 * 示例：
 * <pre>{@code
 * // 需要 admin 或 super 角色
 * @RequireRole({"admin", "super"})
 * @PostMapping("/users")
 * public void createUser() { ... }
 * }</pre>
 *
 * @see RequireAuth
 * @see RequirePermission
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 角色标识列表，满足任一即可（OR 逻辑）。
     * <p>
     * 角色标识的具体含义由 Sa-Token 的角色体系定义。
     *
     * @return 角色标识数组
     */
    String[] value();
}
