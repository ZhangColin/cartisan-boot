package com.cartisan.test.base;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link ApiTestAssertions} 单元测试。
 *
 * @since 0.1.0
 */
@WebMvcTest(controllers = TestController.class)
public class ApiTestAssertionsTest {

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = TestController.class)
    static class TestConfig {
    }

    @Autowired
    private MockMvc mvc;

    /**
     * 测试 assertOk 方法。
     */
    @Test
    void shouldAssertOk_whenResponseIsSuccess() throws Exception {
        mvc.perform(get("/test/success"))
            .andExpect(status().isOk())
            .andExpect(ApiTestAssertions.assertOk());
    }

    /**
     * 测试 assertError 方法。
     */
    @Test
    void shouldAssertError_whenResponseHasErrorCode() throws Exception {
        mvc.perform(get("/test/error"))
            .andExpect(status().isOk())
            .andExpect(ApiTestAssertions.assertError(500));
    }

    /**
     * 测试 assertData 方法 - 简单值。
     */
    @Test
    void shouldAssertData_whenDataFieldMatches() throws Exception {
        mvc.perform(get("/test/success"))
            .andExpect(status().isOk())
            .andExpect(ApiTestAssertions.assertData("id", 1));
    }

    /**
     * 测试 assertData 方法 - 字符串值。
     */
    @Test
    void shouldAssertData_whenDataFieldIsString() throws Exception {
        mvc.perform(get("/test/success"))
            .andExpect(status().isOk())
            .andExpect(ApiTestAssertions.assertData("name", "Test Order"));
    }

    /**
     * 测试 assertNotFound 方法。
     */
    @Test
    void shouldAssertNotFound_whenCodeIs404() throws Exception {
        mvc.perform(get("/test/notFound"))
            .andExpect(status().isOk())
            .andExpect(ApiTestAssertions.assertNotFound());
    }

    /**
     * 测试 assertBadRequest 方法。
     */
    @Test
    void shouldAssertBadRequest_whenCodeIs400() throws Exception {
        mvc.perform(get("/test/badRequest"))
            .andExpect(status().isOk())
            .andExpect(ApiTestAssertions.assertBadRequest());
    }

    /**
     * 测试 assertForbidden 方法。
     */
    @Test
    void shouldAssertForbidden_whenCodeIs403() throws Exception {
        mvc.perform(get("/test/forbidden"))
            .andExpect(status().isOk())
            .andExpect(ApiTestAssertions.assertForbidden());
    }

    /**
     * 测试 assertData 方法 - 嵌套路径。
     */
    @Test
    void shouldAssertData_whenNestedPath() throws Exception {
        mvc.perform(get("/test/nested"))
            .andExpect(status().isOk())
            .andExpect(ApiTestAssertions.assertData("orderId", "ORD-001"));
    }

    // ========== 请求辅助方法测试 ==========

    /**
     * 测试 toJson 方法。
     */
    @Test
    void shouldSerializeObjectToJson() throws Exception {
        // Given
        TestDto dto = new TestDto("test-name", 123);

        // When
        String json = ApiTestAssertions.toJson(dto);

        // Then
        org.assertj.core.api.Assertions.assertThat(json)
            .isEqualTo("{\"name\":\"test-name\",\"value\":123}");
    }

    /**
     * 测试 withToken 方法。
     */
    @Test
    void shouldAddAuthorizationHeader_whenWithToken() throws Exception {
        // Given
        String token = "test-token-123";

        // When
        var processor = ApiTestAssertions.withToken(token);

        // Then
        org.assertj.core.api.Assertions.assertThat(processor).isNotNull();
    }

    /**
     * 测试 withTenantId 方法。
     */
    @Test
    void shouldAddTenantIdHeader_whenWithTenantId() throws Exception {
        // Given
        long tenantId = 42L;

        // When
        var processor = ApiTestAssertions.withTenantId(tenantId);

        // Then
        org.assertj.core.api.Assertions.assertThat(processor).isNotNull();
    }

    /**
     * 测试 DTO。
     */
    public static class TestDto {
        public String name;
        public int value;

        public TestDto(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}
