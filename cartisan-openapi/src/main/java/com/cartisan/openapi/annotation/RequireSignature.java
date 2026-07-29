package com.cartisan.openapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 验签标记注解，标注在 Controller 类或方法上表示该端点<strong>必须通过机机签名认证</strong>。
 *
 * <p>签名 = 认证（调用方是已登记的应用），不做 per-key 权限 ACL。
 * 若未来需要更细粒度的机机鉴权，由应用层另行实现——框架不为此预留扩展点（YAGNI）。</p>
 *
 * <p>实际验签由 {@link com.cartisan.openapi.filter.SignatureVerificationFilter} 完成；
 * {@link com.cartisan.openapi.interceptor.SignatureVerificationInterceptor} 据本注解
 * 强制"无有效签名则 401"。</p>
 *
 * @see com.cartisan.openapi.annotation.NoSignature
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSignature {
}
