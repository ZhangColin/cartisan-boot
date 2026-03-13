package com.cartisan.test.fixture;

/**
 * 字符串随机生成器。
 *
 * <p>提供各种随机字符串生成方法，用于测试数据构建。</p>
 *
 * @since 0.1.0
 */
public final class FixtureStrings {

    /** 默认随机字符串长度 */
    private static final int DEFAULT_LENGTH = 12;

    /** 字母数字字符集 */
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private FixtureStrings() {
    }

    /**
     * 生成 12 位随机字母数字字符串。
     *
     * @return 随机字符串
     */
    public static String randomString() {
        return randomString(DEFAULT_LENGTH);
    }

    /**
     * 生成指定位数的随机字母数字字符串。
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        var random = FixtureSeeds.currentRandom();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 生成带前缀的随机字符串（随机部分 12 位）。
     *
     * @param prefix 前缀
     * @return prefix + 12 位随机字符
     */
    public static String randomString(String prefix) {
        return randomString(prefix, DEFAULT_LENGTH);
    }

    /**
     * 生成带前缀的随机字符串。
     *
     * @param prefix 前缀
     * @param length 随机部分的长度（不含前缀）
     * @return prefix + length 位随机字符
     */
    public static String randomString(String prefix, int length) {
        return prefix + randomString(length);
    }

    /**
     * 生成随机 Email 地址。
     *
     * @return 格式为 {randomString()}@test.local 的 Email
     */
    public static String randomEmail() {
        return randomString() + "@test.local";
    }

    /**
     * 生成随机 UUID 字符串（无连字符）。
     *
     * @return 32 位十六进制字符串
     */
    public static String randomUuid() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成随机手机号（1 开头 12 位）。
     *
     * @return 1 开头的 12 位数字字符串
     */
    public static String randomPhoneNumber() {
        var random = FixtureSeeds.currentRandom();
        StringBuilder sb = new StringBuilder(12);
        sb.append("1");
        for (int i = 0; i < 11; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
