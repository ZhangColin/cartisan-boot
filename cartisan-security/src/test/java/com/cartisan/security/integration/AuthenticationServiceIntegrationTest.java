package com.cartisan.security.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证服务集成测试。
 * <p>
 * 验证 AuthenticationService 的 login/logout/getTokenInfo 方法在 Web 环境下的行为。
 * 通过 TestAuthController 的端点间接测试认证服务。
 * </p>
 */
@DisplayName("认证服务集成测试")
class AuthenticationServiceIntegrationTest extends AbstractSecurityIntegrationTest {

    @Test
    @DisplayName("登录成功返回有效 token")
    void given_loginId_when_login_then_returnToken() throws Exception {
        // When: 调用登录端点
        // Then: 返回有效 token
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("登录后可通过 Token 访问需要认证的接口")
    void given_loggedIn_when_requestWithToken_then_success() throws Exception {
        // Given: 登录获取 token
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/555"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        // When: 使用 token 访问受保护接口
        // Then: 请求成功
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(555));
    }

    @Test
    @DisplayName("有效 token 可查询用户信息")
    void given_validToken_when_getUserInfo_then_success() throws Exception {
        // Given: 有效 token
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/333"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        // When: 使用 token 查询用户信息
        // Then: 返回正确信息
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(333));
    }

    @Test
    @DisplayName("无效 token 查询用户信息返回 401")
    void given_invalidToken_when_getUserInfo_then_401() throws Exception {
        // Given: 无效 token
        String invalidToken = "invalid-token-12345";

        // When: 使用无效 token 查询用户信息
        // Then: 返回 401
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user")
                        .header("satoken", invalidToken))
                .andExpect(status().isUnauthorized());
    }
}
