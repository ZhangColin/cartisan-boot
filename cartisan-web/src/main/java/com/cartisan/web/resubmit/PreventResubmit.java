package com.cartisan.web.resubmit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防止重复提交注解
 *
 * <p>用于标记需要防止重复提交的接口方法，基于 Redis 实现分布式锁机制。
 * 在指定的时间窗口内，同一用户的重复请求将被拒绝。
 *
 * <p>使用示例：
 * <pre>{@code
 * @PostMapping("/users")
 * @PreventResubmit(delaySeconds = 10)
 * public ApiResponse<Void> createUser(@RequestBody CreateUserRequest request) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * @see com.cartisan.web.resubmit
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreventResubmit {

    /**
     * 防重提交时间窗口（秒）
     *
     * <p>在该时间窗口内，相同标识的请求将被拒绝。默认值为 20 秒。
     *
     * @return 时间窗口（秒）
     */
    int delaySeconds() default 20;

    /**
     * Redis key 前缀
     *
     * <p>用于区分不同业务场景的防重提交 key，避免 key 冲突。
     * 默认为空字符串，使用全局默认前缀。
     *
     * @return Redis key 前缀
     */
    String prefix() default "";
}
