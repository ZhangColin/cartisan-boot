package com.cartisan.web.config;

import com.cartisan.web.exception.InvalidEnumValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BaseEnumConverter 单元测试")
class BaseEnumConverterTest {

    private final BaseEnumConverter converter = new BaseEnumConverter();

    @Test
    @DisplayName("应该将有效的 Integer code 转换为对应枚举")
    void shouldConvertValidCodeToEnum() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        TestUserStatus result = enumConverter.convert("1");

        assertThat(result).isEqualTo(TestUserStatus.ACTIVE);
    }

    @Test
    @DisplayName("应该将 code 0 转换为对应枚举")
    void shouldConvertZeroCodeToEnum() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        TestUserStatus result = enumConverter.convert("0");

        assertThat(result).isEqualTo(TestUserStatus.DISABLED);
    }

    @Test
    @DisplayName("当传入 null 时应该返回 null")
    void shouldReturnNull_whenInputIsNull() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        TestUserStatus result = enumConverter.convert(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("当传入空字符串时应该返回 null")
    void shouldReturnNull_whenInputIsEmpty() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        TestUserStatus result = enumConverter.convert("");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("当传入无效 code 时应该抛出携带枚举类与取值表的专用异常")
    void shouldThrowInvalidEnumValueException_whenCodeIsInvalid() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        assertThatThrownBy(() -> enumConverter.convert("999"))
            .isInstanceOf(InvalidEnumValueException.class)
            .hasMessage("TestUserStatus 取值 999 非法，合法取值：1=启用, 0=禁用, 2=待审核");
    }

    @Test
    @DisplayName("当传入非数字字符串时应该抛出携带枚举类与取值表的专用异常")
    void shouldThrowInvalidEnumValueException_whenInputIsNotNumber() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        assertThatThrownBy(() -> enumConverter.convert("ACTIVE"))
            .isInstanceOf(InvalidEnumValueException.class)
            .hasMessage("TestUserStatus 取值 ACTIVE 非法，合法取值：1=启用, 0=禁用, 2=待审核");
    }
}