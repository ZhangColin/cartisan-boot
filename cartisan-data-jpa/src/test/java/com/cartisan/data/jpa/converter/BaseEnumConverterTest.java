package com.cartisan.data.jpa.converter;

import com.cartisan.core.domain.BaseEnum;
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
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}

class BaseEnumConverterTest {

    @Test
    void shouldConvertEnumToInteger() {
        BaseEnumConverter<TestOrderStatus> converter = new TestConverter();

        assertThat(converter.convertToDatabaseColumn(TestOrderStatus.PENDING))
            .isEqualTo(1);
    }

    @Test
    void shouldReturnNull_whenConvertNullEnumToDatabase() {
        BaseEnumConverter<TestOrderStatus> converter = new TestConverter();

        assertThat(converter.convertToDatabaseColumn(null))
            .isNull();
    }

    @Test
    void shouldConvertIntegerToEnum() {
        BaseEnumConverter<TestOrderStatus> converter = new TestConverter();

        assertThat(converter.convertToEntityAttribute(2))
            .isEqualTo(TestOrderStatus.COMPLETED);
    }

    @Test
    void shouldReturnNull_whenConvertNullIntegerToEntity() {
        BaseEnumConverter<TestOrderStatus> converter = new TestConverter();

        assertThat(converter.convertToEntityAttribute(null))
            .isNull();
    }

    @Test
    void shouldThrowException_whenConvertInvalidInteger() {
        BaseEnumConverter<TestOrderStatus> converter = new TestConverter();

        assertThatThrownBy(() -> converter.convertToEntityAttribute(999))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown code: 999");
    }

    // Test helper class to instantiate the abstract base class
    private static class TestConverter extends BaseEnumConverter<TestOrderStatus> {
        public TestConverter() {
            super(TestOrderStatus.class);
        }
    }
}