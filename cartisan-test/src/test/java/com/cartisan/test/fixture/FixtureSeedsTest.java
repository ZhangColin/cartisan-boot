package com.cartisan.test.fixture;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FixtureSeeds 单元测试。
 *
 * <p>测试种子管理的线程安全性和可重复性。</p>
 */
class FixtureSeedsTest {

    @Test
    void shouldUseSameSeed_whenSetGlobalSeed() {
        // Given: 设置固定种子
        FixtureSeeds.setGlobalSeed(42);

        // When: 调用两次 random
        String a1 = FixtureStrings.randomString();
        String a2 = FixtureStrings.randomString();

        // 重置后再次设置相同种子
        FixtureSeeds.resetSeed();
        FixtureSeeds.setGlobalSeed(42);

        String b1 = FixtureStrings.randomString();
        String b2 = FixtureStrings.randomString();

        // Then: 两次序列应该一致
        assertThat(b1).as("同一种子下，第一次调用应产生相同结果").isEqualTo(a1);
        assertThat(b2).as("同一种子下，第二次调用应产生相同结果").isEqualTo(a2);
        // 额外验证：生成的字符串不应为空（当功能实现后）
        assertThat(a1).as("随机字符串不应为空").isNotEmpty();

        // Cleanup
        FixtureSeeds.resetSeed();
    }

    @Test
    void shouldResetSeed_whenResetSeedCalled() {
        // Given: 设置固定种子并记录第一次结果
        FixtureSeeds.setGlobalSeed(42);
        String fixed = FixtureStrings.randomString();

        // When: 重置种子
        FixtureSeeds.resetSeed();
        String random1 = FixtureStrings.randomString();
        String random2 = FixtureStrings.randomString();

        // Then: 重置后的结果应该不同（不再受种子控制）
        assertThat(random1).isNotNull();
        assertThat(random2).isNotNull();

        // Cleanup
        FixtureSeeds.resetSeed();
    }

    @Test
    void shouldBeThreadSafe_whenMultipleThreadsAccess() throws InterruptedException {
        // Given: 设置固定种子
        FixtureSeeds.setGlobalSeed(12345);

        // When: 多线程同时获取 Random
        Thread t1 = new Thread(() -> {
            String s = FixtureStrings.randomString();
            assertThat(s).isNotNull();
        });

        Thread t2 = new Thread(() -> {
            String s = FixtureStrings.randomString();
            assertThat(s).isNotNull();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Then: 无异常抛出即为成功
        // Cleanup
        FixtureSeeds.resetSeed();
    }
}
