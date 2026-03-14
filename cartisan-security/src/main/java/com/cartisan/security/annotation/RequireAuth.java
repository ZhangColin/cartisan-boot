package com.cartisan.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注需要登录才能访问的接口。
 * <p>
 * 可作用于类和方法级别。方法注解优先于类注解。
 * <p>
 * 示例：
 * <pre>{@code
 * @RequireAuth  // 类级别：所有方法都需要登录
 * public class UserController {
 *     @GetMapping("/me")
 *     public User getCurrentUser() { ... }
 * }
 * }</pre>
 *
 * @see RequireRole
 * @see RequirePermission
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {

    /**
     * 是否必须登录，默认为 {@code true}。
     * <p>
     * 预留扩展：支持 {@code @RequireAuth(false)} 作为类级别注解的覆盖，
     * 用于标记某些方法允许匿名访问。
     *
     * @return {@code true} 表示需要登录，{@code false} 表示允许匿名访问
     */
    boolean value() default true;
}
