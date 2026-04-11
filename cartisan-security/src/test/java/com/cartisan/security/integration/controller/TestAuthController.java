package com.cartisan.security.integration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.core.context.RequestContext;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.security.annotation.RequireRole;
import com.cartisan.web.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用 Controller - 验证注解鉴权。
 */
@RestController
@RequestMapping("/test/auth")
public class TestAuthController {

    @GetMapping("/login/{userId}")
    public ApiResponse<Map<String, String>> login(@PathVariable Long userId) {
        StpUtil.login(userId);
        String token = StpUtil.getTokenValue();
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return ApiResponse.ok(result);
    }

    @GetMapping("/login/{userId}/timeout/{timeoutSeconds}")
    public ApiResponse<Map<String, String>> loginWithTimeout(@PathVariable Long userId, @PathVariable Long timeoutSeconds) {
        StpUtil.login(userId, timeoutSeconds);
        String token = StpUtil.getTokenValue();
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return ApiResponse.ok(result);
    }

    @GetMapping("/login/{userId}/role/{role}")
    public ApiResponse<Map<String, String>> loginWithRole(@PathVariable Long userId, @PathVariable String role) {
        StpUtil.login(userId);
        StpUtil.getSession().set("roles", java.util.List.of(role));
        String token = StpUtil.getTokenValue();
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return ApiResponse.ok(result);
    }

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
        result.put("userId", RequestContext.getUserId());
        result.put("username", RequestContext.getUserName());
        return ApiResponse.ok(result);
    }

    // ========== Permission 元数据测试端点 ==========

    @GetMapping("/permission-with-metadata")
    @RequirePermission(
        value = "test:admin:user:read",
        name = "测试 / 管理员 / 用户查看",
        scope = "test"
    )
    public ApiResponse<String> permissionWithMetadata() {
        return ApiResponse.ok("permission with metadata");
    }

    @GetMapping("/permission-simple")
    @RequirePermission("test:simple:action")
    public ApiResponse<String> permissionSimple() {
        return ApiResponse.ok("simple permission");
    }
}
