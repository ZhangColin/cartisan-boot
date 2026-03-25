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

class UniversalEnumConverterTest {

    @Test
    void shouldConvertEnumToInteger() {
        @SuppressWarnings("unchecked")
        UniversalEnumConverter<TestOrderStatus> converter = new UniversalEnumConverter<>(TestOrderStatus.class);

        assertThat(converter.convertToDatabaseColumn(TestOrderStatus.PENDING))
            .isEqualTo(1);
    }

    @Test
    void shouldReturnNull_whenConvertNullEnumToDatabase() {
        @SuppressWarnings("unchecked")
        UniversalEnumConverter<TestOrderStatus> converter = new UniversalEnumConverter<>(TestOrderStatus.class);

        assertThat(converter.convertToDatabaseColumn(null))
            .isNull();
    }

    @Test
    void shouldConvertIntegerToEnum() {
        @SuppressWarnings("unchecked")
        UniversalEnumConverter<TestOrderStatus> converter = new UniversalEnumConverter<>(TestOrderStatus.class);

        assertThat(converter.convertToEntityAttribute(2))
            .isEqualTo(TestOrderStatus.COMPLETED);
    }

    @Test
    void shouldReturnNull_whenConvertNullIntegerToEntity() {
        @SuppressWarnings("unchecked")
        UniversalEnumConverter<TestOrderStatus> converter = new UniversalEnumConverter<>(TestOrderStatus.class);

        assertThat(converter.convertToEntityAttribute(null))
            .isNull();
    }

    @Test
    void shouldThrowException_whenConvertInvalidInteger() {
        @SuppressWarnings("unchecked")
        UniversalEnumConverter<TestOrderStatus> converter = new UniversalEnumConverter<>(TestOrderStatus.class);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(999))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown code: 999");
    }
}