package com.cartisan.security.annotation;

import java.lang.annotation.*;

/**
 * Controller 方法参数注解，用于注入当前登录用户的 ID。
 * <p>
 * 支持两种参数类型，表达不同的登录要求：
 * <ul>
 *   <li>{@code Long} - 必需登录，未登录时抛出 {@code NotLoginException}</li>
 *   <li>{@code Optional<Long>} - 可选登录，未登录时返回 {@code Optional.empty()}</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 必需登录
 * @GetMapping("/profile")
 * public ApiResponse<UserProfile> getProfile(@CurrentUser Long userId) {
 *     return ApiResponse.ok(userService.getProfile(userId));
 * }
 *
 * // 可选登录
 * @GetMapping("/preferences")
 * public ApiResponse<Preferences> getPreferences(@CurrentUser Optional<Long> userId) {
 *     if (userId.isPresent()) {
 *         return ApiResponse.ok(preferencesService.getForUser(userId.get()));
 *     }
 *     return ApiResponse.ok(preferencesService.getDefault());
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
