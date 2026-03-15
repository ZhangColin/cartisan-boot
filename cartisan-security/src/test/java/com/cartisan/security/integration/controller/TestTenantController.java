package com.cartisan.security.integration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.security.context.TenantContext;
import com.cartisan.web.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用 Controller - 验证 TenantContext。
 */
@RestController
@RequestMapping("/test/tenant")
public class TestTenantController {

    /**
     * 测试登录并设置租户 ID 的端点。
     */
    @GetMapping("/login/{userId}/tenant/{tenantId}")
    public ApiResponse<Map<String, String>> loginWithTenant(@PathVariable Long userId, @PathVariable Long tenantId) {
        StpUtil.login(userId);
        StpUtil.getSession().set("tenantId", tenantId.toString());
        String token = StpUtil.getTokenValue();
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return ApiResponse.ok(result);
    }

    @GetMapping("/current")
    public ApiResponse<Map<String, Object>> getCurrentTenant() {
        Map<String, Object> result = new HashMap<>();
        // 允许 null 值
        result.put("tenantId", TenantContext.getCurrentTenantId());
        return ApiResponse.ok(result);
    }

    @PostMapping("/with-tenant")
    public ApiResponse<String> withTenant(@RequestBody Map<String, Object> body) {
        Long tenantId = TenantContext.getCurrentTenantId();
        return ApiResponse.ok("processed in tenant: " + tenantId);
    }
}
