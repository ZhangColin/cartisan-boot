/**
 * 防重复提交功能包
 *
 * <p>提供基于 Redis 的分布式防重复提交机制，用于防止用户短时间内重复提交表单或请求。
 *
 * <p>核心组件：
 * <ul>
 *   <li>{@link com.cartisan.web.resubmit.PreventResubmit} - 防重复提交注解</li>
 * </ul>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * @PostMapping("/users")
 * @PreventResubmit(delaySeconds = 10, prefix = "user:create")
 * public ApiResponse<Void> createUser(@RequestBody CreateUserRequest request) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
package com.cartisan.web.resubmit;
