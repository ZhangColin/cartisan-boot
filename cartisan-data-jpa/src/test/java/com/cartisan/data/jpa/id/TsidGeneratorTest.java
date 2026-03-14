package com.cartisan.data.jpa.id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TsidGenerator 单元测试。
 *
 * <p>遵循 TEST-002 命名规范：given_{条件}_when_{操作}_then_{预期结果}</p>
 */
@DisplayName("TsidGenerator 测试")
class TsidGeneratorTest {

    @Test
    @DisplayName("given_generator_when_generate_then_returnsNonNullPositiveLong")
    void given_generator_when_generate_then_returnsNonNullPositiveLong() {
        // Given
        TsidGenerator generator = TsidGenerator.newInstance();

        // When
        long tsid = generator.generate();

        // Then
        assertThat(tsid).isPositive();
    }

    @Test
    @DisplayName("given_generator_when_generate10000_then_allUnique")
    void given_generator_when_generate10000_then_allUnique() {
        // Given
        TsidGenerator generator = TsidGenerator.newInstance();
        Set<Long> generatedIds = new HashSet<>();

        // When
        for (int i = 0; i < 10000; i++) {
            generatedIds.add(generator.generate());
        }

        // Then
        assertThat(generatedIds).hasSize(10000);
    }

    @Test
    @DisplayName("given_generator_when_generateMultiple_then_timestampPartNonDecreasing")
    void given_generator_when_generateMultiple_then_timestampPartNonDecreasing() {
        // Given
        TsidGenerator generator = TsidGenerator.newInstance();
        int count = 1000;

        // When
        long[] tsids = new long[count];
        for (int i = 0; i < count; i++) {
            tsids[i] = generator.generate();
        }

        // Then: 验证时间戳部分不递减
        long previousTimestamp = extractTimestampPart(tsids[0]);
        for (int i = 1; i < count; i++) {
            long currentTimestamp = extractTimestampPart(tsids[i]);
            assertThat(currentTimestamp).isGreaterThanOrEqualTo(previousTimestamp);
            previousTimestamp = currentTimestamp;
        }
    }

    @Test
    @DisplayName("given_tsid_when_toInstant_then_withinOneMillisecond")
    void given_tsid_when_toInstant_then_withinOneMillisecond() {
        // Given
        TsidGenerator generator = TsidGenerator.newInstance();
        Instant before = Instant.now();

        // When
        long tsid = generator.generate();
        Instant extracted = generator.toInstant(tsid);
        Instant after = Instant.now();

        // Then
        assertThat(extracted).isAfterOrEqualTo(toInstantBeforeEpoch(before));
        assertThat(extracted).isBeforeOrEqualTo(after.plusMillis(1));
    }

    @Test
    @DisplayName("given_zeroTsid_when_toInstant_then_throwsIllegalArgumentException")
    void given_zeroTsid_when_toInstant_then_throwsIllegalArgumentException() {
        // Given
        TsidGenerator generator = TsidGenerator.newInstance();

        // When & Then
        assertThatThrownBy(() -> generator.toInstant(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("given_negativeTsid_when_toInstant_then_throwsIllegalArgumentException")
    void given_negativeTsid_when_toInstant_then_throwsIllegalArgumentException() {
        // Given
        TsidGenerator generator = TsidGenerator.newInstance();

        // When & Then
        assertThatThrownBy(() -> generator.toInstant(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("given_futureTsid_when_toInstant_then_throwsIllegalArgumentException")
    void given_futureTsid_when_toInstant_then_throwsIllegalArgumentException() {
        // Given
        TsidGenerator generator = TsidGenerator.newInstance();
        // 构造一个时间戳为"当前时间 + 2 天"的 TSID
        long futureTimestamp = System.currentTimeMillis() + Duration.ofDays(2).toMillis();
        long epochMillis = TsidGenerator.EPOCH.toEpochMilli();
        long timestampPart = (futureTimestamp - epochMillis) & 0x3FFFFFFFFFFL;
        long futureTsid = timestampPart << TsidGenerator.RANDOM_BITS;

        // When & Then
        assertThatThrownBy(() -> generator.toInstant(futureTsid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    @DisplayName("given_concurrentGeneration_when_10Threads1000Each_then_allUnique")
    void given_concurrentGeneration_when_10Threads1000Each_then_allUnique() throws InterruptedException {
        // Given
        TsidGenerator generator = TsidGenerator.newInstance();
        int threads = 10;
        int idsPerThread = 1000;
        Set<Long> generatedIds = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        // When
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < idsPerThread; j++) {
                    generatedIds.add(generator.generate());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Then
        assertThat(generatedIds).hasSize(threads * idsPerThread);
    }

    // ========== 辅助方法 ==========

    /**
     * 提取 TSID 的时间戳部分。
     */
    private long extractTimestampPart(long tsid) {
        return tsid >>> TsidGenerator.RANDOM_BITS;
    }

    /**
     * 将 Instant 转换为相对于 epoch 的时间，用于比较。
     */
    private Instant toInstantBeforeEpoch(Instant instant) {
        long epochMillis = TsidGenerator.EPOCH.toEpochMilli();
        return Instant.ofEpochMilli(Math.max(epochMillis, instant.toEpochMilli()));
    }
}
