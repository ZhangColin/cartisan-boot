package com.cartisan.ai.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SseProperties 单元测试")
class SsePropertiesTest {

    @Test
    @DisplayName("给定默认配置 - 获取 timeout - 返回 5 分钟")
    void shouldReturnDefaultTimeout() {
        // Given: 默认构造的 SseProperties
        SseProperties properties = new SseProperties();

        // When
        Duration timeout = properties.getTimeout();

        // Then
        assertThat(timeout).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("给定自定义 timeout - 设置后获取 - 返回自定义值")
    void shouldReturnCustomTimeout() {
        // Given
        SseProperties properties = new SseProperties();
        Duration customTimeout = Duration.ofMinutes(10);

        // When
        properties.setTimeout(customTimeout);

        // Then
        assertThat(properties.getTimeout()).isEqualTo(customTimeout);
    }

    @Test
    @DisplayName("给定默认配置 - 获取 heartbeat - 返回 30 秒")
    void shouldReturnDefaultHeartbeat() {
        // Given
        SseProperties properties = new SseProperties();

        // When
        Duration heartbeat = properties.getHeartbeat();

        // Then
        assertThat(heartbeat).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("给定自定义 heartbeat - 设置后获取 - 返回自定义值")
    void shouldReturnCustomHeartbeat() {
        // Given
        SseProperties properties = new SseProperties();
        Duration customHeartbeat = Duration.ofSeconds(60);

        // When
        properties.setHeartbeat(customHeartbeat);

        // Then
        assertThat(properties.getHeartbeat()).isEqualTo(customHeartbeat);
    }

    @Test
    @DisplayName("给定默认配置 - 获取 heartbeatEnabled - 返回 true")
    void shouldReturnDefaultHeartbeatEnabled() {
        // Given
        SseProperties properties = new SseProperties();

        // When & Then
        assertThat(properties.isHeartbeatEnabled()).isTrue();
    }

    @Test
    @DisplayName("给定自定义 heartbeatEnabled - 设置后获取 - 返回自定义值")
    void shouldReturnCustomHeartbeatEnabled() {
        // Given
        SseProperties properties = new SseProperties();

        // When
        properties.setHeartbeatEnabled(false);

        // Then
        assertThat(properties.isHeartbeatEnabled()).isFalse();
    }
}
