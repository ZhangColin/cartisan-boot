package com.cartisan.security.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @CurrentUser 注解集成测试。
 * <p>
 * 验证 @CurrentUser 参数注入的行为。
 * </p>
 */
@DisplayName("@CurrentUser 注解集成测试")
class CurrentUserIntegrationTest extends AbstractSecurityIntegrationTest {

    @Test
    @DisplayName("已登录访问 @CurrentUser Long userId 应返回 200")
    void given_authenticatedUser_when_getCurrentUserId_then_200() throws Exception {
        // 先登录获取 token
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/123"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            // 访问 @CurrentUser Long userId 接口
            mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user-id")
                            .header("satoken", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(123));
        } finally {
            cn.dev33.satoken.stp.StpUtil.logout(123L);
        }
    }

    @Test
    @DisplayName("未登录访问 @CurrentUser Long userId 应返回 401")
    void given_unauthenticatedUser_when_getCurrentUserId_then_401() throws Exception {
        // 未登录访问 @CurrentUser Long userId 接口
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已登录访问 @CurrentUser Optional<Long> userId 应返回 isPresent=true")
    void given_authenticatedUser_when_getCurrentUserIdOptional_then_isPresentTrue() throws Exception {
        // 先登录获取 token
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/456"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            // 访问 @CurrentUser Optional<Long> userId 接口
            mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user-id-optional")
                            .header("satoken", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(456))
                    .andExpect(jsonPath("$.data.isPresent").value(true));
        } finally {
            cn.dev33.satoken.stp.StpUtil.logout(456L);
        }
    }

    @Test
    @DisplayName("未登录访问 @CurrentUser Optional<Long> userId 应返回 isPresent=false")
    void given_unauthenticatedUser_when_getCurrentUserIdOptional_then_isPresentFalse() throws Exception {
        // 未登录访问 @CurrentUser Optional<Long> userId 接口
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user-id-optional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").isEmpty())
                .andExpect(jsonPath("$.data.isPresent").value(false));
    }

    @Test
    @DisplayName("@RequireAuth + @CurrentUser Long 组合：已登录应返回 200")
    void given_authenticatedUser_when_getCurrentUserIdWithAuth_then_200() throws Exception {
        // 先登录获取 token
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/789"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            // 访问 @RequireAuth + @CurrentUser Long userId 接口
            mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user-id-auth")
                            .header("satoken", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(789));
        } finally {
            cn.dev33.satoken.stp.StpUtil.logout(789L);
        }
    }

    @Test
    @DisplayName("@RequireAuth + @CurrentUser Long 组合：未登录应返回 401")
    void given_unauthenticatedUser_when_getCurrentUserIdWithAuth_then_401() throws Exception {
        // 未登录访问 @RequireAuth + @CurrentUser Long userId 接口
        // 应该在 @RequireAuth 拦截器阶段返回 401，不会到达参数解析
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user-id-auth"))
                .andExpect(status().isUnauthorized());
    }
}
