package com.cartisan.web.exception;

import com.cartisan.web.config.TestUserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvalidEnumValueException 单元测试")
class InvalidEnumValueExceptionTest {

    @Test
    @DisplayName("异常应为 IllegalArgumentException 子类，兼容现有 cause 链匹配")
    void shouldBeIllegalArgumentException() {
        InvalidEnumValueException ex = new InvalidEnumValueException(TestUserStatus.class, "99");

        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("异常应携带枚举类型与非法值")
    void shouldCarryEnumTypeAndInvalidValue() {
        InvalidEnumValueException ex = new InvalidEnumValueException(TestUserStatus.class, "99");

        assertThat(ex.getEnumType()).isEqualTo(TestUserStatus.class);
        assertThat(ex.getInvalidValue()).isEqualTo("99");
    }

    @Test
    @DisplayName("无名字时 message 应用枚举简单名 + 非法值 + 完整取值表")
    void shouldBuildMessageWithEnumName_whenNameAbsent() {
        InvalidEnumValueException ex = new InvalidEnumValueException(TestUserStatus.class, "99");

        assertThat(ex.getMessage())
                .isEqualTo("TestUserStatus 取值 99 非法，合法取值：1=启用, 0=禁用, 2=待审核");
    }

    @Test
    @DisplayName("有名字时 message 应用参数名/字段名替代枚举简单名")
    void shouldBuildMessageWithGivenName() {
        String message = InvalidEnumValueException.message(
                "status", TestUserStatus.class, "99");

        assertThat(message)
                .isEqualTo("status 取值 99 非法，合法取值：1=启用, 0=禁用, 2=待审核");
    }

    @Test
    @DisplayName("名字为 null 或空时 message 应回退到枚举简单名")
    void shouldFallbackToEnumName_whenNameIsBlank() {
        assertThat(InvalidEnumValueException.message(null, TestUserStatus.class, "99"))
                .contains("TestUserStatus 取值 99 非法");
        assertThat(InvalidEnumValueException.message("", TestUserStatus.class, "99"))
                .contains("TestUserStatus 取值 99 非法");
    }

    @Test
    @DisplayName("非法值支持非字符串类型（如 Integer code）")
    void shouldSupportNonStringInvalidValue() {
        InvalidEnumValueException ex = new InvalidEnumValueException(TestUserStatus.class, 99);

        assertThat(ex.getMessage()).contains("取值 99 非法");
        assertThat(ex.getInvalidValue()).isEqualTo(99);
    }

    @Test
    @DisplayName("enumType 为 null 时应抛出带清晰消息的 NPE（PIT-004）")
    void shouldThrowNpeWithClearMessage_whenEnumTypeIsNull() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new InvalidEnumValueException(null, "99"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("enumType cannot be null");
    }
}
