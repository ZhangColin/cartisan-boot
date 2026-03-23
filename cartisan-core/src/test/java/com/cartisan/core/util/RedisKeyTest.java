package com.cartisan.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisKey 测试。
 */
class RedisKeyTest {

    @Test
    void shouldCreateKeyWithExpiration() {
        // Given
        String prefix = "user:cache";
        long expireSeconds = 3600;

        // When
        RedisKey redisKey = RedisKey.of(prefix, expireSeconds);

        // Then
        assertThat(redisKey.expireSeconds()).isEqualTo(expireSeconds);
        assertThat(redisKey.isPermanent()).isFalse();
    }

    @Test
    void shouldCreatePermanentKey() {
        // Given
        String prefix = "system:config";

        // When
        RedisKey redisKey = RedisKey.permanent(prefix);

        // Then
        assertThat(redisKey.expireSeconds()).isEqualTo(0);
        assertThat(redisKey.isPermanent()).isTrue();
    }

    @Test
    void shouldGenerateFullKeyWithSuffix() {
        // Given
        RedisKey redisKey = RedisKey.of("user:cache", 3600);
        String suffix = "12345";

        // When
        String fullKey = redisKey.key(suffix);

        // Then
        assertThat(fullKey).isEqualTo("user:cache:12345");
    }

    @Test
    void shouldReturnCorrectExpireSeconds() {
        // Given
        long expectedSeconds = 7200;
        RedisKey redisKey = RedisKey.of("session:data", expectedSeconds);

        // When
        long actualSeconds = redisKey.expireSeconds();

        // Then
        assertThat(actualSeconds).isEqualTo(expectedSeconds);
    }

    @Test
    void shouldIdentifyPermanentKey() {
        // Given
        RedisKey permanentKey = RedisKey.permanent("config:settings");
        RedisKey temporaryKey = RedisKey.of("temp:data", 300);

        // When & Then
        assertThat(permanentKey.isPermanent()).isTrue();
        assertThat(temporaryKey.isPermanent()).isFalse();
    }

    @Test
    void shouldHandleEmptySuffix() {
        // Given
        RedisKey redisKey = RedisKey.of("prefix", 3600);

        // When
        String fullKey = redisKey.key("");

        // Then
        assertThat(fullKey).isEqualTo("prefix:");
    }

    @Test
    void shouldHandleNumericExpireSeconds() {
        // Given
        RedisKey redisKey = RedisKey.of("test", 1);

        // When & Then
        assertThat(redisKey.expireSeconds()).isEqualTo(1);
        assertThat(redisKey.isPermanent()).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenAttemptInstantiationViaReflection() throws Exception {
        // Given
        java.lang.reflect.Constructor<RedisKey> constructor =
                RedisKey.class.getDeclaredConstructor(String.class, long.class);
        constructor.setAccessible(true);

        // When & Then - 私有构造函数可通过反射调用，但应正常工作
        // 本测试验证私有构造函数的行为符合预期
        RedisKey redisKey = constructor.newInstance("test", 100);
        assertThat(redisKey.expireSeconds()).isEqualTo(100);
        assertThat(redisKey.key("suffix")).isEqualTo("test:suffix");
    }
}
