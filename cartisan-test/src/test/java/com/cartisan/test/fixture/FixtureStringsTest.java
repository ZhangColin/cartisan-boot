package com.cartisan.test.fixture;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FixtureStrings 单元测试。
 */
class FixtureStringsTest {

    @Test
    void shouldReturnDefaultLength_whenRandomString() {
        String result = FixtureStrings.randomString();
        assertThat(result).hasSize(12);
    }

    @Test
    void shouldReturnSpecifiedLength_whenRandomStringWithLength() {
        String result = FixtureStrings.randomString(20);
        assertThat(result).hasSize(20);
    }

    @Test
    void shouldStartWithPrefix_whenRandomStringWithPrefix() {
        String result = FixtureStrings.randomString("USER_");
        assertThat(result).startsWith("USER_");
        assertThat(result).hasSize(5 + 12); // prefix + 12
    }

    @Test
    void shouldIncludePrefixAndSpecifiedLength_whenRandomStringWithPrefixAndLength() {
        String result = FixtureStrings.randomString("ORDER_", 8);
        assertThat(result).startsWith("ORDER_");
        assertThat(result).hasSize(6 + 8); // prefix + length
    }

    @Test
    void shouldHaveValidFormat_whenRandomEmail() {
        String result = FixtureStrings.randomEmail();
        assertThat(result).endsWith("@test.local");
        assertThat(result).matches("^.+@test\\.local$");
    }

    @Test
    void shouldHave32Chars_whenRandomUuid() {
        String result = FixtureStrings.randomUuid();
        assertThat(result).hasSize(32);
        assertThat(result).matches("^[a-f0-9]{32}$");
    }

    @Test
    void shouldStartWith1_whenRandomPhoneNumber() {
        String result = FixtureStrings.randomPhoneNumber();
        assertThat(result).hasSize(12);
        assertThat(result).startsWith("1");
        assertThat(result).matches("^1\\d{11}$");
    }
}
