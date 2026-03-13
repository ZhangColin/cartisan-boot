package com.cartisan.test.fixture;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FixtureNumbers 单元测试。
 */
class FixtureNumbersTest {

    @Test
    void shouldReturnPositive_whenRandomInt() {
        int result = FixtureNumbers.randomInt();
        assertThat(result).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldReturnInRange_whenRandomIntWithRange() {
        int result = FixtureNumbers.randomInt(10, 20);
        assertThat(result).isBetween(10, 20);
    }

    @Test
    void shouldReturnPositiveId_whenRandomId() {
        long id = FixtureNumbers.randomId();
        assertThat(id).isGreaterThan(0);
    }

    @Test
    void shouldHaveScale2_whenRandomAmount() {
        BigDecimal amount = FixtureNumbers.randomAmount();
        assertThat(amount).isPositive();
        assertThat(amount.scale()).isEqualTo(2);
    }

    @Test
    void shouldReturnInDecimalRange_whenRandomDecimal() {
        BigDecimal min = new BigDecimal("10.5");
        BigDecimal max = new BigDecimal("20.5");
        BigDecimal result = FixtureNumbers.randomDecimal(min, max, 1);
        assertThat(result).isBetween(min, max);
        assertThat(result.scale()).isEqualTo(1);
    }
}
