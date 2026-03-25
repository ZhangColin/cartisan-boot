package com.cartisan.data.jpa.integration;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.data.jpa.annotation.EnumConvert;
import com.cartisan.data.jpa.converter.UniversalEnumConverter;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * 枚举增强集成测试。
 * <p>
 * 验证 BaseEnum ↔ JPA ↔ Database 的完整流程。
 */
class EnumIntegrationTest {

    @Test
    void shouldConvertEnumToInteger() {
        @SuppressWarnings("unchecked")
        UniversalEnumConverter<TestUserStatus> converter = new UniversalEnumConverter<>(TestUserStatus.class);

        assertThat(converter.convertToDatabaseColumn(TestUserStatus.ACTIVE))
            .isEqualTo(1);
    }

    @Test
    void shouldConvertIntegerToEnum() {
        @SuppressWarnings("unchecked")
        UniversalEnumConverter<TestUserStatus> converter = new UniversalEnumConverter<>(TestUserStatus.class);

        assertThat(converter.convertToEntityAttribute(0))
            .isEqualTo(TestUserStatus.INACTIVE);
    }

    @Test
    void shouldHandleNullValues() {
        @SuppressWarnings("unchecked")
        UniversalEnumConverter<TestUserStatus> converter = new UniversalEnumConverter<>(TestUserStatus.class);

        assertThat(converter.convertToDatabaseColumn(null))
            .isNull();
        assertThat(converter.convertToEntityAttribute(null))
            .isNull();
    }
}

// 测试实体
@Entity
@Table(name = "test_users")
class TestUser {
    @Id
    Long id;

    @EnumConvert(TestUserStatus.class)
    @Column(name = "status")
    TestUserStatus status;
}