package com.cartisan.test.context;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit 5 注解，自动设置 RequestContext。
 *
 * <p>支持 class 级和方法级。方法级注解优先级更高。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @WithRequestContext(userId = 1L, userName = "test", tenantId = 100L)
 * class MyTest {
 *     @Test
 *     void shouldHaveContext() {
 *         assertThat(RequestContext.getUserId()).isEqualTo(1L);
 *     }
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(RequestContextExtension.class)
public @interface WithRequestContext {
    String requestId() default "test-request-id";
    String clientIp() default "127.0.0.1";
    long userId() default 0L;
    String userName() default "";
    long tenantId() default 0L;
    String tenantName() default "";
}
