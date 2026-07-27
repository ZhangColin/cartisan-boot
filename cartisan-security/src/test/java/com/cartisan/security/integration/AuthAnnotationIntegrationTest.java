package com.cartisan.security.integration;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 注解鉴权集成测试。
 * <p>
 * 验证 @RequireAuth、@RequireRole、@RequirePermission 注解的正确行为。
 * </p>
 */
@DisplayName("注解鉴权集成测试")
class AuthAnnotationIntegrationTest extends AbstractSecurityIntegrationTest {

    @Test
    @DisplayName("未登录访问 @RequireAuth 接口应返回 401")
    void given_noAuth_when_getRequireAuth_then_401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/require-auth"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已登录访问 @RequireAuth 接口应返回 200")
    void given_loggedIn_when_getRequireAuth_then_200() throws Exception {
        // 先登录获取 token
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/100"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            mvc.perform(MockMvcRequestBuilders.get("/test/auth/require-auth")
                            .header("satoken", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("authenticated"));
        } finally {
            StpUtil.logout(100L);
        }
    }

    @Test
    @DisplayName("无 ADMIN 角色访问 @RequireRole 接口应返回 403")
    void given_userWithoutRole_when_getRequireAdmin_then_403() throws Exception {
        // 先登录获取 token（无角色）
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/100"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            mvc.perform(MockMvcRequestBuilders.get("/test/auth/require-admin")
                            .header("satoken", token))
                    .andExpect(status().isForbidden());
        } finally {
            StpUtil.logout(100L);
        }
    }

    @Test
    @DisplayName("有 ADMIN 角色访问 @RequireRole 接口应返回 200")
    void given_userWithRole_when_getRequireAdmin_then_200() throws Exception {
        // 先登录并设置角色，获取 token
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/100/role/ADMIN"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            mvc.perform(MockMvcRequestBuilders.get("/test/auth/require-admin")
                            .header("satoken", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("admin access"));
        } finally {
            StpUtil.logout(100L);
        }
    }

    @Test
    @DisplayName("无 user:create 权限访问 @RequirePermission 接口应返回 403")
    void given_userWithoutPermission_when_getRequirePermission_then_403() throws Exception {
        // 先登录获取 token（无权限）
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/100"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            mvc.perform(MockMvcRequestBuilders.get("/test/auth/require-permission")
                            .header("satoken", token))
                    .andExpect(status().isForbidden());
        } finally {
            StpUtil.logout(100L);
        }
    }

    @Test
    @DisplayName("有 user:create 权限访问 @RequirePermission 接口应返回 200")
    void given_userWithPermission_when_getRequirePermission_then_200() throws Exception {
        // 先登录并设置权限，获取 token
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/100/permission/user:create"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            mvc.perform(MockMvcRequestBuilders.get("/test/auth/require-permission")
                            .header("satoken", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("permission granted"));
        } finally {
            StpUtil.logout(100L);
        }
    }

    @Test
    @DisplayName("超管（bypass）无权限访问 @RequirePermission 接口应返回 200")
    void shouldBypassPermissionCheck_whenSuperAdminRequests() throws Exception {
        // 超管登录（不授予任何权限，依赖 bypass 放行）
        Long superAdminId = TestAuthorizationBypassResolver.SUPER_ADMIN_ID;
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/" + superAdminId))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            mvc.perform(MockMvcRequestBuilders.get("/test/auth/require-permission")
                            .header("satoken", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("permission granted"));
        } finally {
            StpUtil.logout(superAdminId);
        }
    }

    @Test
    @DisplayName("超管（bypass）无角色访问 @RequireRole 接口应返回 200")
    void shouldBypassRoleCheck_whenSuperAdminRequests() throws Exception {
        // 超管登录（不授予任何角色，依赖 bypass 放行）
        Long superAdminId = TestAuthorizationBypassResolver.SUPER_ADMIN_ID;
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/" + superAdminId))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            mvc.perform(MockMvcRequestBuilders.get("/test/auth/require-admin")
                            .header("satoken", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("admin access"));
        } finally {
            StpUtil.logout(superAdminId);
        }
    }
}
