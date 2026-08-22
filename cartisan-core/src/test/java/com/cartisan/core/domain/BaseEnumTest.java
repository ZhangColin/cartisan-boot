package com.cartisan.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 测试用枚举
enum TestStatus implements BaseEnum<TestStatus> {
    ACTIVE(1, "启用"),
    DISABLED(0, "禁用");

    private final Integer code;
    private final String name;

    TestStatus(Integer code, String name) {
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
}

class BaseEnumTest {

    @Test
    void shouldParseByCode_whenCodeValid() {
        assertThat(BaseEnum.parseByCode(TestStatus.class, 1))
            .isEqualTo(TestStatus.ACTIVE);
    }

    @Test
    void shouldReturnNull_whenCodeInvalid() {
        assertThat(BaseEnum.parseByCode(TestStatus.class, 999))
            .isNull();
    }

    @Test
    void shouldReturnNull_whenCodeIsNull() {
        assertThat(BaseEnum.parseByCode(TestStatus.class, null))
            .isNull();
    }

    @Test
    void shouldRequireByCode_whenCodeValid() {
        assertThat(BaseEnum.requireByCode(TestStatus.class, 0))
            .isEqualTo(TestStatus.DISABLED);
    }

    @Test
    void shouldThrowException_whenRequireByCodeInvalid() {
        assertThatThrownBy(() -> BaseEnum.requireByCode(TestStatus.class, 999))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown code: 999");
    }

    @Test
    void shouldReturnSameInstance_whenParsingMultipleTimes() {
        TestStatus first = BaseEnum.parseByCode(TestStatus.class, 1);
        TestStatus second = BaseEnum.parseByCode(TestStatus.class, 1);
        assertThat(first).isSameAs(second);
        assertThat(first).isSameAs(TestStatus.ACTIVE);
    }

    @Test
    void shouldUseCache_whenRequireByCode() {
        TestStatus parsed = BaseEnum.parseByCode(TestStatus.class, 0);
        TestStatus required = BaseEnum.requireByCode(TestStatus.class, 0);
        assertThat(parsed).isSameAs(required);
        assertThat(parsed).isSameAs(TestStatus.DISABLED);
    }

    @Test
    void shouldBuildCodeTable_inDeclarationOrder() {
        assertThat(BaseEnum.codeTableOf(TestStatus.class))
            .isEqualTo("1=启用, 0=禁用");
    }
}