package com.cartisan.openapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 排除验签注解，标注在方法上表示该接口不需要验签。
 *
 * <p>用于类级别 @RequireSignature 下的个别方法排除。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSignature {
}
