package com.cartisan.security.integration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.security.annotation.CurrentUser;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.security.annotation.RequireRole;
import com.cartisan.security.context.SecurityContext;
import com.cartisan.web.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 测试用 Controller - 验证注解鉴权。
 */
@RestController
@RequestMapping("/test/auth")
public class TestAuthController {

    /**
     * 测试登录端点。
     * <p>
     * 仅用于集成测试，执行登录并返回 token。
     * </p>
     *
     * @param userId 用户 ID
     * @return 包含 token 的响应
     */
    @GetMapping("/login/{userId}")
    public ApiResponse<Map<String, String>> login(@PathVariable Long userId) {
        StpUtil.login(userId);
        String token = StpUtil.getTokenValue();
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return ApiResponse.ok(result);
    }

    /**
     * 测试登录并设置超时的端点。
     */
    @GetMapping("/login/{userId}/timeout/{timeoutSeconds}")
    public ApiResponse<Map<String, String>> loginWithTimeout(@PathVariable Long userId, @PathVariable Long timeoutSeconds) {
        StpUtil.login(userId, timeoutSeconds);
        String token = StpUtil.getTokenValue();
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return ApiResponse.ok(result);
    }

    /**
     * 测试登录并设置角色的端点。
     */
    @GetMapping("/login/{userId}/role/{role}")
    public ApiResponse<Map<String, String>> loginWithRole(@PathVariable Long userId, @PathVariable String role) {
        StpUtil.login(userId);
        StpUtil.getSession().set("roles", java.util.List.of(role));
        String token = StpUtil.getTokenValue();
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return ApiResponse.ok(result);
    }

    /**
     * 测试登录并设置权限的端点。
     */
    @GetMapping("/login/{userId}/permission/{permission}")
    public ApiResponse<Map<String, String>> loginWithPermission(@PathVariable Long userId, @PathVariable String permission) {
        StpUtil.login(userId);
        StpUtil.getSession().set("permissions", java.util.List.of(permission));
        String token = StpUtil.getTokenValue();
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return ApiResponse.ok(result);
    }

    @GetMapping("/require-auth")
    @RequireAuth
    public ApiResponse<String> requireAuth() {
        return ApiResponse.ok("authenticated");
    }

    @GetMapping("/require-admin")
    @RequireRole("ADMIN")
    public ApiResponse<String> requireAdmin() {
        return ApiResponse.ok("admin access");
    }

    @GetMapping("/require-permission")
    @RequirePermission("user:create")
    public ApiResponse<String> requirePermission() {
        return ApiResponse.ok("permission granted");
    }

    @GetMapping("/current-user")
    @RequireAuth
    public ApiResponse<Map<String, Object>> getCurrentUser() {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", SecurityContext.getCurrentUserId());
        result.put("username", SecurityContext.getCurrentUsername());
        return ApiResponse.ok(result);
    }

    // ========== @CurrentUser 测试端点 ==========

    /**
     * 测试 @CurrentUser Long userId - 必需登录。
     */
    @GetMapping("/current-user-id")
    public ApiResponse<Map<String, Object>> getCurrentUserId(@CurrentUser Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        return ApiResponse.ok(result);
    }

    /**
     * 测试 @CurrentUser Optional<Long> userId - 可选登录。
     */
    @GetMapping("/current-user-id-optional")
    public ApiResponse<Map<String, Object>> getCurrentUserIdOptional(@CurrentUser Optional<Long> userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId.orElse(null));
        result.put("isPresent", userId.isPresent());
        return ApiResponse.ok(result);
    }

    /**
     * 测试 @CurrentUser Long userId + @RequireAuth 组合。
     */
    @GetMapping("/current-user-id-auth")
    @RequireAuth
    public ApiResponse<Map<String, Object>> getCurrentUserIdWithAuth(@CurrentUser Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        return ApiResponse.ok(result);
    }

    // ========== Permission 元数据测试端点 ==========

    /**
     * 测试带 name 和 scope 的权限注解。
     */
    @GetMapping("/permission-with-metadata")
    @RequirePermission(
        value = "test:admin:user:read",
        name = "测试 / 管理员 / 用户查看",
        scope = "test"
    )
    public ApiResponse<String> permissionWithMetadata() {
        return ApiResponse.ok("permission with metadata");
    }

    /**
     * 测试仅 value 的权限注解。
     */
    @GetMapping("/permission-simple")
    @RequirePermission("test:simple:action")
    public ApiResponse<String> permissionSimple() {
        return ApiResponse.ok("simple permission");
    }
}
