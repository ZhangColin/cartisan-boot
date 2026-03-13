package com.cartisan.test.fixture;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FixtureDates 单元测试。
 */
class FixtureDatesTest {

    @Test
    void shouldReturnCurrentTime_whenNow() {
        LocalDateTime result = FixtureDates.now();
        assertThat(result).isNotNull();
        assertThat(result).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    void shouldReturnPastTime_whenPastDays() {
        LocalDateTime now = FixtureDates.now();
        LocalDateTime result = FixtureDates.pastDays(7);
        assertThat(result).isBefore(now);
        // 允许 1 秒误差
        assertThat(result).isAfter(now.minusDays(7).minusSeconds(1));
    }

    @Test
    void shouldReturnFutureTime_whenFutureDays() {
        LocalDateTime now = FixtureDates.now();
        LocalDateTime result = FixtureDates.futureDays(3);
        assertThat(result).isAfter(now);
        // 允许 1 秒误差
        assertThat(result).isBefore(now.plusDays(3).plusSeconds(1));
    }

    @Test
    void shouldRelativeTime_whenPastDaysWithBase() {
        LocalDateTime base = LocalDateTime.of(2026, 3, 14, 12, 0);
        LocalDateTime result = FixtureDates.pastDays(base, 7);
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 3, 7, 12, 0));
    }

    @Test
    void shouldRelativeTime_whenFutureDaysWithBase() {
        LocalDateTime base = LocalDateTime.of(2026, 3, 14, 12, 0);
        LocalDateTime result = FixtureDates.futureDays(base, 7);
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 3, 21, 12, 0));
    }
}
