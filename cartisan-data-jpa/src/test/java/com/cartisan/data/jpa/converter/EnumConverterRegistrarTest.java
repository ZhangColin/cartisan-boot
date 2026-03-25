package com.cartisan.data.jpa.converter;

import com.cartisan.core.domain.BaseEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

enum TestPaymentStatus implements BaseEnum<TestPaymentStatus> {
    PAID(1, "已支付"),
    UNPAID(0, "未支付");

    private final Integer code;
    private final String name;

    TestPaymentStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}

class EnumConverterRegistrarTest {

    @Test
    void shouldCreateConverter_forEnumType() {
        var registrar = new EnumConverterRegistrar();

        UniversalEnumConverter<?> converter = registrar.createConverter(TestPaymentStatus.class);

        assertThat(converter.getEnumType()).isEqualTo(TestPaymentStatus.class);
    }

    @Test
    void shouldConvertEnumToDatabaseColumn() {
        var converter = new UniversalEnumConverter<TestPaymentStatus>(TestPaymentStatus.class);

        assertThat(converter.convertToDatabaseColumn(TestPaymentStatus.PAID)).isEqualTo(1);
    }

    @Test
    void shouldConvertDatabaseColumnToEnum() {
        var converter = new UniversalEnumConverter<TestPaymentStatus>(TestPaymentStatus.class);

        assertThat(converter.convertToEntityAttribute(1)).isEqualTo(TestPaymentStatus.PAID);
    }
}
