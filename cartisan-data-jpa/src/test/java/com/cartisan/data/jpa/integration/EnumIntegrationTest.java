package com.cartisan.data.jpa.integration;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.data.jpa.annotation.EnumConvert;
import com.cartisan.data.jpa.converter.UniversalEnumConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 枚举增强集成测试。
 * <p>
 * 验证 BaseEnum ↔ UniversalEnumConverter 的转换流程。
 */
class EnumIntegrationTest {

    @Test
    void shouldConvertEnumToInteger() {
        UniversalEnumConverter<TestUserStatus> converter = new UniversalEnumConverter<>(TestUserStatus.class);

        assertThat(converter.convertToDatabaseColumn(TestUserStatus.ACTIVE))
            .isEqualTo(1);
        assertThat(converter.convertToDatabaseColumn(TestUserStatus.INACTIVE))
            .isEqualTo(0);
    }

    @Test
    void shouldConvertIntegerToEnum() {
        UniversalEnumConverter<TestUserStatus> converter = new UniversalEnumConverter<>(TestUserStatus.class);

        assertThat(converter.convertToEntityAttribute(1))
            .isEqualTo(TestUserStatus.ACTIVE);
        assertThat(converter.convertToEntityAttribute(0))
            .isEqualTo(TestUserStatus.INACTIVE);
    }

    @Test
    void shouldHandleNullValues() {
        UniversalEnumConverter<TestUserStatus> converter = new UniversalEnumConverter<>(TestUserStatus.class);

        assertThat(converter.convertToDatabaseColumn(null))
            .isNull();
        assertThat(converter.convertToEntityAttribute(null))
            .isNull();
    }

    @Test
    void shouldThrowException_forInvalidCode() {
        UniversalEnumConverter<TestUserStatus> converter = new UniversalEnumConverter<>(TestUserStatus.class);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(999))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown code: 999");
    }
}

// 测试实体 - 展示 @EnumConvert 用法
@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "test_users")
class TestUser {
    @jakarta.persistence.Id
    Long id;

    @EnumConvert(TestUserStatus.class)
    @jakarta.persistence.Column(name = "status")
    TestUserStatus status;
}

// 测试枚举
enum TestUserStatus implements BaseEnum<TestUserStatus> {
    ACTIVE(1, "激活"),
    INACTIVE(0, "未激活");

    private final Integer code;
    private final String name;

    TestUserStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}
