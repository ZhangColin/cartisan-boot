package com.cartisan.test.fixture;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 全局随机种子管理器。
 *
 * <p>提供线程安全的 Random 实例，支持设置全局种子使测试结果可重复。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 设置固定种子
 * FixtureSeeds.setGlobalSeed(42);
 * String a1 = FixtureStrings.randomString();
 * String a2 = FixtureStrings.randomString();
 *
 * // 重置种子
 * FixtureSeeds.resetSeed();
 * String b1 = FixtureStrings.randomString(); // 不再固定
 *
 * // 再次设置相同种子，序列可重复
 * FixtureSeeds.setGlobalSeed(42);
 * String c1 = FixtureStrings.randomString();
 * assertThat(c1).isEqualTo(a1);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class FixtureSeeds {

    /** 全局种子，null 表示未设置 */
    private static final AtomicReference<Long> GLOBAL_SEED = new AtomicReference<>();

    /** ThreadLocal Random 缓存 */
    private static final ThreadLocal<Random> RANDOM_CACHE = ThreadLocal.withInitial(() -> {
        Long seed = GLOBAL_SEED.get();
        return seed != null ? new Random(seed) : new Random();
    });

    private FixtureSeeds() {
        // 工具类，禁止实例化
    }

    /**
     * 设置全局随机种子。
     *
     * <p>设置后，所有新创建的 Random 实例都会使用该种子。</p>
     *
     * @param seed 随机种子值
     */
    public static void setGlobalSeed(long seed) {
        GLOBAL_SEED.set(seed);
        RANDOM_CACHE.remove(); // 清空缓存，下次获取时使用新种子
    }

    /**
     * 重置为随机种子。
     *
     * <p>清除全局种子，后续 Random 实例将使用随机种子。</p>
     */
    public static void resetSeed() {
        GLOBAL_SEED.set(null);
        RANDOM_CACHE.remove(); // 清空缓存
    }

    /**
     * 获取当前线程的 Random 实例。
     *
     * <p>包可见，仅供同包的 Fixture 类使用。</p>
     *
     * @return Random 实例
     */
    static Random currentRandom() {
        return RANDOM_CACHE.get();
    }
}
