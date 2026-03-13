package com.cartisan.test.fixture;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日期随机生成器。
 *
 * <p>提供当前时间、过去时间、未来时间等生成方法，用于测试数据构建。</p>
 *
 * @since 0.1.0
 */
public final class FixtureDates {

    private FixtureDates() {
    }

    /**
     * 获取当前时间。
     *
     * @return 当前 LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前日期。
     *
     * @return 当前 LocalDate
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * 获取过去 N 天的时间。
     *
     * @param days 天数（非负）
     * @return 当前时间减去 N 天
     */
    public static LocalDateTime pastDays(int days) {
        return now().minusDays(days);
    }

    /**
     * 获取未来 N 天的时间。
     *
     * @param days 天数（非负）
     * @return 当前时间加上 N 天
     */
    public static LocalDateTime futureDays(int days) {
        return now().plusDays(days);
    }

    /**
     * 获取基准时间的过去 N 天。
     *
     * @param base 基准时间
     * @param days 天数（非负）
     * @return 基准时间减去 N 天
     */
    public static LocalDateTime pastDays(LocalDateTime base, int days) {
        return base.minusDays(days);
    }

    /**
     * 获取基准时间的未来 N 天。
     *
     * @param base 基准时间
     * @param days 天数（非负）
     * @return 基准时间加上 N 天
     */
    public static LocalDateTime futureDays(LocalDateTime base, int days) {
        return base.plusDays(days);
    }
}
