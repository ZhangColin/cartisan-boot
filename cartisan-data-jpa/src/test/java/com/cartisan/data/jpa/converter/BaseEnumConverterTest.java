package com.cartisan.data.jpa.converter;

import com.cartisan.core.domain.BaseEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

enum TestOrderStatus implements BaseEnum<TestOrderStatus> {
    PENDING(1, "待处理"),
    COMPLETED(2, "已完成");

    private final Integer code;
    private final String name;

    TestOrderStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * JPA Converter for TestOrderStatus.
     * <p>
     * This demonstrates the recommended pattern: enum with internal Converter class.
     */
    @jakarta.persistence.Converter(autoApply = true)
    public static class Converter extends BaseEnumConverter<TestOrderStatus> {
        public Converter() {
            super(TestOrderStatus.class);
        }
    }
}

@DisplayName("BaseEnumConverter 单元测试")
class BaseEnumConverterTest {

    @Test
    @DisplayName("应该将枚举转换为数据库整数值")
    void shouldConvertEnumToInteger() {
        TestOrderStatus.Converter converter = new TestOrderStatus.Converter();

        assertThat(converter.convertToDatabaseColumn(TestOrderStatus.PENDING))
            .isEqualTo(1);
    }

    @Test
    @DisplayName("应该将 null 枚举转换为 null 数据库值")
    void shouldReturnNull_whenConvertNullEnumToDatabase() {
        TestOrderStatus.Converter converter = new TestOrderStatus.Converter();

        assertThat(converter.convertToDatabaseColumn(null))
            .isNull();
    }

    @Test
    @DisplayName("应该将数据库整数值转换为枚举")
    void shouldConvertIntegerToEnum() {
        TestOrderStatus.Converter converter = new TestOrderStatus.Converter();

        assertThat(converter.convertToEntityAttribute(2))
            .isEqualTo(TestOrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("应该将 null 数据库值转换为 null 枚举")
    void shouldReturnNull_whenConvertNullIntegerToEntity() {
        TestOrderStatus.Converter converter = new TestOrderStatus.Converter();

        assertThat(converter.convertToEntityAttribute(null))
            .isNull();
    }

    @Test
    @DisplayName("应该抛出异常当转换无效整数值")
    void shouldThrowException_whenConvertInvalidInteger() {
        TestOrderStatus.Converter converter = new TestOrderStatus.Converter();

        assertThatThrownBy(() -> converter.convertToEntityAttribute(999))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown code: 999");
    }
}