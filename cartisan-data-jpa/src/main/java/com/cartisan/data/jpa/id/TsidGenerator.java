package com.cartisan.data.jpa.id;

import java.time.Instant;
import java.util.Random;

/**
 * TSID（Time-Sorted ID）生成器。
 *
 * <p>基于 TSID 规范生成全局唯一、时间排序的 64 位 ID。</p>
 *
 * <h2>格式</h2>
 * <pre>
 * |------ 42 位时间戳 ------|-- 22 位随机数 --|
 * |    毫秒级（约 69 年）    |    同一毫秒内    |
 * </pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * TsidGenerator generator = TsidGenerator.newInstance();
 * long tsid = generator.generate();
 * Instant createdAt = generator.toInstant(tsid);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class TsidGenerator {

    /** 时间戳位数 */
    public static final int TIMESTAMP_BITS = 42;

    /** 随机数位数 */
    public static final int RANDOM_BITS = 22;

    /** 最大随机数值：(1 &lt;&lt; 22) - 1 */
    public static final int MAX_RANDOM = 4194303;

    /** 自定义 epoch：2024-01-01T00:00:00Z */
    public static final Instant EPOCH = Instant.parse("2024-01-01T00:00:00Z");

    /** epoch 的毫秒表示 */
    private static final long EPOCH_MILLIS = EPOCH.toEpochMilli();

    /** 时间戳掩码：42 位 */
    private static final long TIMESTAMP_MASK = 0x3FFFFFFFFFFL;

    /** 随机数源 */
    private final Random random;

    /** 上一次的时间戳（毫秒，相对于 epoch） */
    private long lastTimestamp = Long.MIN_VALUE;

    /** 同一毫秒内的计数器 */
    private int counter;

    /**
     * 私有构造函数。
     *
     * @param random 随机数生成器
     */
    private TsidGenerator(Random random) {
        this.random = random;
    }

    /**
     * 创建默认的 TsidGenerator 实例。
     *
     * <p>使用 {@link java.util.concurrent.ThreadLocalRandom} 作为随机数源。</p>
     *
     * @return 新的 TsidGenerator 实例
     */
    public static TsidGenerator newInstance() {
        return new TsidGenerator(java.util.concurrent.ThreadLocalRandom.current());
    }

    /**
     * 创建指定随机数源的 TsidGenerator 实例（测试用）。
     *
     * @param random 随机数生成器（用于测试时固定 seed）
     * @return 新的 TsidGenerator 实例
     */
    public static TsidGenerator withRandom(Random random) {
        return new TsidGenerator(random);
    }

    /**
     * 生成一个新的 TSID。
     *
     * <p>TSID 格式：42 位时间戳 + 22 位随机数</p>
     *
     * @return 全局唯一、时间排序的正 long 值
     */
    public synchronized long generate() {
        // 1. 获取当前毫秒时间戳（相对于 epoch）
        long currentMillis = System.currentTimeMillis() - EPOCH_MILLIS;

        // 2. 时间戳部分按 42 位掩码截断（避免溢出）
        long timestampPart = currentMillis & TIMESTAMP_MASK;

        if (timestampPart != lastTimestamp) {
            // 新的毫秒，重置计数器
            lastTimestamp = timestampPart;
            counter = 0;
            // 使用随机数作为第一个值
            int randomValue = this.random.nextInt(MAX_RANDOM + 1);
            return (timestampPart << RANDOM_BITS) | randomValue;
        }

        // 同一毫秒内，使用计数器确保唯一性
        if (counter >= MAX_RANDOM) {
            // 计数器溢出，等待下一毫秒
            while (true) {
                currentMillis = System.currentTimeMillis() - EPOCH_MILLIS;
                timestampPart = currentMillis & TIMESTAMP_MASK;
                if (timestampPart != lastTimestamp) {
                    lastTimestamp = timestampPart;
                    counter = 0;
                    int randomValue = this.random.nextInt(MAX_RANDOM + 1);
                    return (timestampPart << RANDOM_BITS) | randomValue;
                }
                // 短暂休眠避免忙等待
                try {
                    Thread.sleep(0, 100_000); // 0.1ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for next millisecond", e);
                }
            }
        }

        return (timestampPart << RANDOM_BITS) | counter++;
    }

    /**
     * 从 TSID 提取生成时间。
     *
     * @param tsid 由本生成器产生的 ID
     * @return 生成该 ID 的大致时间（误差 &lt; 1ms）
     * @throws IllegalArgumentException 如果 tsid 不是有效的 TSID
     */
    public Instant toInstant(long tsid) {
        if (tsid <= 0) {
            throw new IllegalArgumentException("TSID must be positive: " + tsid);
        }

        // 提取时间戳部分（高 42 位）
        long timestampMillis = (tsid >>> RANDOM_BITS) + EPOCH_MILLIS;
        Instant result = Instant.ofEpochMilli(timestampMillis);

        // 校验：提取的时间不应超过当前时间 + 1 天（避免明显无效的 TSID）
        Instant maxAllowed = Instant.now().plus(java.time.Duration.ofDays(1));
        if (result.isAfter(maxAllowed)) {
            throw new IllegalArgumentException("TSID timestamp is too far in the future: " + tsid);
        }

        return result;
    }
}
