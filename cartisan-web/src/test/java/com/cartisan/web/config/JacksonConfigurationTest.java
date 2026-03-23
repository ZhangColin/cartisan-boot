package com.cartisan.web.config;

import com.cartisan.web.TestApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jackson 全局配置测试。
 *
 * <p>验证 Jackson 序列化/反序列化行为符合预期。</p>
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class
)
class JacksonConfigurationTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 验证 Long 类型序列化为字符串。
     * <p>解决 JavaScript Long 精度问题（JS Number 最大安全整数是 2^53 - 1）。</p>
     */
    @Test
    void shouldSerializeLongAsString() throws Exception {
        // Given
        Long value = 1234567890123456789L;

        // When
        String json = objectMapper.writeValueAsString(value);

        // Then
        assertThat(json).isEqualTo("\"1234567890123456789\"");
    }

    /**
     * 验证 LocalDateTime 序列化为 ISO 8601 格式。
     */
    @Test
    void shouldSerializeLocalDateTimeAsIso8601() throws Exception {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2026, Month.MARCH, 24, 12, 30, 45);

        // When
        String json = objectMapper.writeValueAsString(dateTime);

        // Then
        assertThat(json).isEqualTo("\"2026-03-24T12:30:45\"");
    }

    /**
     * 验证 BigDecimal 序列化时不使用科学计数法。
     */
    @Test
    void shouldSerializeBigDecimalWithoutScientificNotation() throws Exception {
        // Given
        BigDecimal value = new BigDecimal("12345678901234567890.123456789");

        // When
        String json = objectMapper.writeValueAsString(value);

        // Then
        assertThat(json).isEqualTo("12345678901234567890.123456789");
    }

    /**
     * 验证 Enum 序列化为字符串（而非索引）。
     */
    @Test
    void shouldSerializeEnumAsString() throws Exception {
        // Given
        TestEnum value = TestEnum.VALUE_ONE;

        // When
        String json = objectMapper.writeValueAsString(value);

        // Then
        assertThat(json).isEqualTo("\"VALUE_ONE\"");
    }

    /**
     * 验证反序列化时忽略未知属性。
     */
    @Test
    void shouldIgnoreUnknownProperties() throws Exception {
        // Given
        String json = "{\"name\":\"test\",\"unknownField\":\"value\"}";

        // When
        TestDto result = objectMapper.readValue(json, TestDto.class);

        // Then
        assertThat(result.name()).isEqualTo("test");
    }

    /**
     * 测试用枚举。
     */
    enum TestEnum {
        VALUE_ONE,
        VALUE_TWO
    }

    /**
     * 测试用 DTO。
     */
    record TestDto(String name) {
    }
}
