package com.cartisan.security.context;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 当前用户上下文工具类。
 * <p>
 * 提供当前登录用户信息的只读访问，隐藏 Sa-Token 实现细节。
 * 未登录时：getCurrentUserId/getCurrentUsername 返回 null，hasRole/hasPermission 返回 false。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 推荐用法：先检查是否登录
 * if (SecurityContext.isAuthenticated()) {
 *     Long userId = SecurityContext.getCurrentUserId();
 *     // 使用 userId...
 * }
 *
 * // 或者：对返回值做 null 检查
 * Long userId = SecurityContext.getCurrentUserId();
 * if (userId != null) {
 *     // 使用 userId...
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
public final class SecurityContext {

    /**
     * 防止实例化。
     */
    private SecurityContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取当前登录用户的 ID。
     *
     * @return 用户 ID，未登录时返回 {@code null}
     */
    public static Long getCurrentUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 获取当前登录用户的用户名（即登录 ID）。
     * <p>
     * 返回值取决于业务层登录时传入的 loginId。若 loginId 是 username，则返回 username；
     * 若 loginId 是 userId，则返回 userId 的字符串形式。
     * </p>
     *
     * @return 用户名，未登录时返回 {@code null}
     */
    public static String getCurrentUsername() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return StpUtil.getLoginIdAsString();
    }

    /**
     * 判断当前用户是否拥有指定角色。
     *
     * @param role 角色标识（如 "admin"）
     * @return 拥有角色返回 {@code true}，未登录或无角色返回 {@code false}
     * @throws IllegalArgumentException 如果 role 为 null 或空
     */
    public static boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or blank");
        }
        return StpUtil.hasRole(role);
    }

    /**
     * 判断当前用户是否拥有指定权限。
     *
     * @param permission 权限标识（如 "user:create"）
     * @return 拥有权限返回 {@code true}，未登录或无权限返回 {@code false}
     * @throws IllegalArgumentException 如果 permission 为 null 或空
     */
    public static boolean hasPermission(String permission) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("Permission cannot be null or blank");
        }
        return StpUtil.hasPermission(permission);
    }

    /**
     * 判断当前用户是否已登录。
     *
     * @return 已登录返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isAuthenticated() {
        return StpUtil.isLogin();
    }
}
