package com.cartisan.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注需要指定权限才能访问的接口。
 * <p>
 * 仅作用于方法级别。
 * 单值设计，每个注解声明一个权限。
 * <p>
 * 示例：
 * <pre>{@code
 * @RequirePermission(
 *     value = "admin:user:read",
 *     name = "平台管理 / 用户管理 / 查看",
 *     scope = "admin"
 * )
 * @GetMapping("/users")
 * public List<User> list() { ... }
 * }</pre>
 *
 * @see RequireAuth
 * @see RequireRole
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 权限 code，格式：{context}:{module}:{action}
     * <p>示例：admin:user:read</p>
     *
     * @return 权限 code
     */
    String value();

    /**
     * 权限显示名称，用于界面展示。
     * <p>空字符串时使用 code 作为 name</p>
     *
     * @return 显示名称
     */
    String name() default "";

    /**
     * 权限作用域，用于区分不同系统/范围。
     * <p>未填时（空字符串）扫描时转为 null，表示全局权限</p>
     *
     * @return 作用域
     */
    String scope() default "";
}
