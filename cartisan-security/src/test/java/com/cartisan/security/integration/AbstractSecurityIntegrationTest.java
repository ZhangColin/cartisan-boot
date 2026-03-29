package com.cartisan.security.integration;

import com.cartisan.security.integration.support.SecurityTestHelpers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 集成测试基类。
 * <p>
 * 提供统一的测试上下文和 MockMvc 配置。
 * </p>
 */
@SpringBootTest(classes = IntegrationTestApplication.class)
@AutoConfigureMockMvc
public abstract class AbstractSecurityIntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeAll
    static void initSaTokenContext() {
        // 初始化 Sa-Token 上下文（测试环境需要手动初始化）
        SaTokenTestConfig.initSaTokenContext();
    }

    @BeforeEach
    void setUp() {
        // 清理 Sa-Token 登录状态，确保每个测试从干净状态开始
        SecurityTestHelpers.cleanup();
    }

    @AfterEach
    void tearDown() {
        // TenantContext 使用 ScopedValue，请求结束后自动清理
        // 若测试中直接调用了 runWithTenant，可在此补充清理逻辑
    }

    /**
     * 从登录响应中提取 token。
     *
     * @param responseContent 响应内容
     * @return token 值
     */
    protected String extractToken(String responseContent) {
        try {
            JsonNode root = objectMapper.readTree(responseContent);
            return root.path("data").path("token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract token from response", e);
        }
    }
}
