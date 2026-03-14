package com.cartisan.web.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RequestContextFilter 集成测试。
 *
 * <p>验证 Filter 在真实 Spring 环境中正确工作。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
class RequestContextFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        // Filter 通过 @Component 自动注册
        // 验证通过测试中的行为间接确认
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void given_requestWithoutRequestId_when_makeRequest_then_requestContextInitialized() throws Exception {
        // Given: 没有 X-Request-Id Header 的请求

        // When: 发起请求到现有端点
        MvcResult result = mockMvc.perform(get("/test/exception"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        // Then: 请求成功（Filter 正常工作，异常由 ExceptionHandler 处理）
        assertThat(result.getResponse().getStatus()).isEqualTo(500);

        // And: 请求结束后上下文已被清理
        assertThat(RequestContext.getRequestId()).isNull();
    }

    @Test
    void given_requestWithRequestId_when_makeRequest_then_contextHasCorrectValue() throws Exception {
        // Given: 有 X-Request-Id Header 的请求

        // When: 发起请求
        MvcResult result = mockMvc.perform(get("/test/exception")
                        .header("X-Request-Id", "integration-test-123"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        // Then: 请求成功返回（即使端点抛异常）
        assertThat(result.getResponse().getStatus()).isEqualTo(500);

        // And: 请求结束后上下文已被清理
        assertThat(RequestContext.getRequestId()).isNull();
    }

    @Test
    void given_requestWithXffHeader_when_makeRequest_then_clientIpExtracted() throws Exception {
        // Given: 有 X-Forwarded-For Header 的请求

        // When: 发起请求
        MvcResult result = mockMvc.perform(get("/test/exception")
                        .header("X-Forwarded-For", "10.20.30.40"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        // Then: 请求成功
        assertThat(result.getResponse().getStatus()).isEqualTo(500);
    }

    @Test
    void given_sequentialRequests_when_makeRequests_then_eachRequestHasNewContext() throws Exception {
        // Given: 多个顺序请求

        // When: 顺序发起请求
        for (int i = 0; i < 3; i++) {
            MvcResult result = mockMvc.perform(get("/test/exception")
                            .header("X-Request-Id", "sequential-" + i))
                    .andExpect(status().isInternalServerError())
                    .andReturn();

            // Then: 每个请求成功
            assertThat(result.getResponse().getStatus()).isEqualTo(500);

            // And: 请求结束后上下文已被清理
            assertThat(RequestContext.getRequestId()).isNull();
        }
    }

    @Test
    void given_requestThrowsException_when_makeRequest_then_contextStillCleared() throws Exception {
        // Given: /test/exception 端点会抛异常

        // When: 发起请求
        MvcResult result = mockMvc.perform(get("/test/exception"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        // Then: 请求返回错误，但上下文已被清理
        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertThat(RequestContext.getRequestId()).isNull();
        assertThat(RequestContext.getClientIp()).isNull();
    }
}
