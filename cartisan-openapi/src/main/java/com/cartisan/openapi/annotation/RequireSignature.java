package com.cartisan.openapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 验签注解，标注在 Controller 类或方法上表示需要验签。
 *
 * <p>支持可选的 permission 属性用于权限检查。</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSignature {
    /**
     * 所需权限标识，空字符串表示不检查权限。
     */
    String permission() default "";
}
