package com.cartisan.security.integration;

import org.hamcrest.Matchers;
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

    @Test
    @DisplayName("自定义超时登录成功返回有效 token")
    void given_loginIdAndTimeout_when_loginWithTimeout_then_returnToken() throws Exception {
        // When: 调用自定义超时登录端点（7天）
        // Then: 返回有效 token
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/888/timeout/604800"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("自定义超时登录后可访问需要认证的接口")
    void given_loggedInWithCustomTimeout_when_requestWithToken_then_success() throws Exception {
        // Given: 使用自定义超时登录获取 token（1小时）
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/999/timeout/3600"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        // When: 使用 token 访问受保护接口
        // Then: 请求成功
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(999));
    }

    @Test
    @DisplayName("login(loginId, userName) 登录后，后续请求 RequestContext.userName 为所传用户名")
    void shouldPopulateRequestContextUserName_whenLoginWithUserName() throws Exception {
        // Given: 经 AuthenticationService 登录，传入非空 userName（默认超时）
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login-svc/555")
                        .param("userName", "Alice"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        // When: 带 token 访问回显端点
        // Then: RequestContext.userName 为所传用户名（写端 login → Session → 读端 SecurityFilter → RequestContext 全链路）
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(555))
                .andExpect(jsonPath("$.data.username").value("Alice"));
    }

    @Test
    @DisplayName("login(loginId, timeout, userName) 自定义超时登录后，后续请求 RequestContext.userName 为所传用户名")
    void shouldPopulateRequestContextUserName_whenLoginWithTimeoutAndUserName() throws Exception {
        // Given: 经 AuthenticationService 自定义超时登录，传入非空 userName
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login-svc/666/timeout/3600")
                        .param("userName", "Bob"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        // When: 带 token 访问回显端点
        // Then: 两个重载行为一致，RequestContext.userName 为所传用户名
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(666))
                .andExpect(jsonPath("$.data.username").value("Bob"));
    }

    @Test
    @DisplayName("login 传入 null userName 时，后续请求 RequestContext.userName 为 null（等价旧行为）")
    void shouldLeaveRequestContextUserNameNull_whenLoginWithoutUserName() throws Exception {
        // Given: 经 AuthenticationService 登录，不传 userName（null，机器账号等无显示名场景）
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login-svc/777"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        // When: 带 token 访问回显端点
        // Then: RequestContext.userName 为 null（框架不写 session，等价旧行为）
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(777))
                .andExpect(jsonPath("$.data.username").value(Matchers.nullValue()));
    }
}
