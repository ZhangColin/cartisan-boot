package com.cartisan.test.fixture;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 数字随机生成器。
 *
 * <p>提供各种数字随机生成方法，用于测试数据构建。</p>
 *
 * @since 0.1.0
 */
public final class FixtureNumbers {

    /** 默认金额小数位数 */
    private static final int DEFAULT_AMOUNT_SCALE = 2;

    /** 默认金额上限 */
    private static final BigDecimal DEFAULT_AMOUNT_MAX = new BigDecimal("1000000");

    private FixtureNumbers() {
    }

    /**
     * 生成随机正整数。
     *
     * @return 正整数
     */
    public static int randomInt() {
        return FixtureSeeds.currentRandom().nextInt(Integer.MAX_VALUE);
    }

    /**
     * 生成指定范围内的随机整数。
     *
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return [min, max] 范围内的整数
     */
    public static int randomInt(int min, int max) {
        if (min < 0) {
            throw new IllegalArgumentException("min must be >= 0");
        }
        if (max <= min) {
            throw new IllegalArgumentException("max must be > min");
        }
        return min + FixtureSeeds.currentRandom().nextInt(max - min + 1);
    }

    /**
     * 生成随机正长整数。
     *
     * @return [0, Long.MAX_VALUE] 范围内的长整数
     */
    public static long randomLong() {
        // 使用 nextLong(bound) 避免 Long.MIN_VALUE 溢出问题
        return FixtureSeeds.currentRandom().nextLong(Long.MAX_VALUE);
    }

    /**
     * 生成指定范围内的随机长整数。
     *
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return [min, max] 范围内的长整数
     */
    public static long randomLong(long min, long max) {
        if (min < 0) {
            throw new IllegalArgumentException("min must be >= 0");
        }
        if (max <= min) {
            throw new IllegalArgumentException("max must be > min");
        }
        long range = max - min + 1;
        long value = FixtureSeeds.currentRandom().nextLong() % range;
        if (value < 0) {
            value += range;
        }
        return min + value;
    }

    /**
     * 生成随机正数 ID。
     *
     * @return [1, Long.MAX_VALUE] 范围内的长整数
     */
    public static long randomId() {
        return randomLong(1, Long.MAX_VALUE);
    }

    /**
     * 生成随机金额（2 位小数）。
     *
     * @return 2 位小数的 BigDecimal，范围 (0, 1000000]
     */
    public static BigDecimal randomAmount() {
        // 确保 > 0：使用 0.01 作为最小值
        double max = DEFAULT_AMOUNT_MAX.doubleValue();
        double value = 0.01 + FixtureSeeds.currentRandom().nextDouble() * (max - 0.01);
        return BigDecimal.valueOf(value).setScale(DEFAULT_AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 生成指定范围和精度的随机 BigDecimal。
     *
     * @param min 最小值
     * @param max 最大值
     * @param scale 小数位数
     * @return [min, max] 范围内，精度为 scale 的 BigDecimal
     */
    public static BigDecimal randomDecimal(BigDecimal min, BigDecimal max, int scale) {
        if (min.compareTo(max) >= 0) {
            throw new IllegalArgumentException("max must be > min");
        }
        double range = max.subtract(min).doubleValue();
        double value = min.doubleValue() + (FixtureSeeds.currentRandom().nextDouble() * range);
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }
}
