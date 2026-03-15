package com.cartisan.security.integration;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 租户上下文集成测试。
 * <p>
 * 验证 TenantContext 从 Header 或 Session 解析租户 ID 的行为。
 * </p>
 */
@DisplayName("租户上下文集成测试")
class TenantContextIntegrationTest extends AbstractSecurityIntegrationTest {

    /**
     * 从登录响应中提取 token。
     */
    private String extractToken(String responseContent) {
        try {
            JsonNode root = objectMapper.readTree(responseContent);
            return root.path("data").path("token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract token from response", e);
        }
    }

    @Test
    @DisplayName("无租户信息时 getCurrentTenantId 应返回 null")
    void given_noTenant_when_getCurrentTenant_then_null() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/test/tenant/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").isEmpty());
    }

    @Test
    @DisplayName("Header 传租户 ID 时应正确解析")
    void given_tenantHeader_when_getCurrentTenant_then_tenantId() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/test/tenant/current")
                        .header("X-Tenant-Id", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(123));
    }

    @Test
    @DisplayName("Session 中有租户 ID 时应正确解析")
    void given_sessionWithTenant_when_getCurrentTenant_then_tenantId() throws Exception {
        // 通过登录端点获取 token（Session 中有 tenantId=456）
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/tenant/login/100/tenant/456"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            mvc.perform(MockMvcRequestBuilders.get("/test/tenant/current")
                            .header("satoken", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tenantId").isNumber());
        } finally {
            StpUtil.logout(100L);
        }
    }

    @Test
    @DisplayName("Header 优先级高于 Session")
    void given_bothHeaderAndSession_when_getCurrentTenant_then_headerPriority() throws Exception {
        // 通过登录端点获取 token，Session 中有 tenantId=456
        String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/tenant/login/100/tenant/456"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        try {
            // Header 中有 tenantId=789，应优先使用 Header 的值
            mvc.perform(MockMvcRequestBuilders.get("/test/tenant/current")
                            .header("satoken", token)
                            .header("X-Tenant-Id", "789"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tenantId").value(789));
        } finally {
            StpUtil.logout(100L);
        }
    }

    @Test
    @DisplayName("请求结束后租户上下文应清理")
    void given_tenantInFirstRequest_when_secondRequest_then_null() throws Exception {
        // 第一个请求带租户 Header
        mvc.perform(MockMvcRequestBuilders.get("/test/tenant/current")
                        .header("X-Tenant-Id", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(123));

        // 第二个请求不带租户信息，租户 ID 应为 null
        mvc.perform(MockMvcRequestBuilders.get("/test/tenant/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").isEmpty());
    }
}
