package com.cartisan.security.integration;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = IntegrationTestApplication.class)
@AutoConfigureMockMvc
public abstract class AbstractSecurityIntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeAll
    static void initSaTokenContext() {
        SaTokenTestConfig.initSaTokenContext();
    }

    @BeforeEach
    void setUp() {
        // 清理 Sa-Token 登录状态
        try {
            StpUtil.logout();
        } catch (Exception ignored) {
        }
    }

    @AfterEach
    void tearDown() {
        // RequestContext uses ScopedValue, auto-cleaned after request
    }

    protected String extractToken(String responseContent) {
        try {
            JsonNode root = objectMapper.readTree(responseContent);
            return root.path("data").path("token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract token from response", e);
        }
    }
}
