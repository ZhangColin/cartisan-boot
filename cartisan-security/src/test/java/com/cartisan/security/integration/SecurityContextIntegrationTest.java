package com.cartisan.security.integration;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 安全上下文集成测试。
 * <p>
 * 验证 SecurityContext 从 Sa-Token 读取用户信息的行为。
 * </p>
 */
@DisplayName("安全上下文集成测试")
class SecurityContextIntegrationTest extends AbstractSecurityIntegrationTest {

    @Test
    @DisplayName("未登录访问 @RequireAuth 接口应返回 401")
    void given_notLoggedIn_when_getCurrentUser_then_401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已登录时正确获取用户信息")
    void given_loggedIn_when_getCurrentUser_then_userInfo() throws Exception {
        // 先登录获取 token
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/888"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            // getCurrentUsername() 返回 loginId 的字符串形式
            mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user")
                            .header("satoken", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(888))
                    .andExpect(jsonPath("$.data.username").value("888"));
        } finally {
            StpUtil.logout(888L);
        }
    }

    @Test
    @DisplayName("登出后再次访问应返回 401")
    void given_loggedOut_when_getCurrentUser_then_401() throws Exception {
        // 1. 登录获取 token
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/999"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        // 2. 登出
        StpUtil.logout(999L);

        // 3. 再次访问应返回 401
        mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user")
                        .header("satoken", token))
                .andExpect(status().isUnauthorized());
    }
}
