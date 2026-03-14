package com.cartisan.test.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

@DisplayName("RedisTestContainer 单元测试")
class RedisTestContainerTest {

    @Test
    @DisplayName("redis() 方法应返回配置好的 GenericContainer")
    void given_whenRedis_thenReturnsConfiguredContainer() {
        // When
        GenericContainer<?> container = RedisTestContainer.redis();

        // Then
        assertThat(container).isNotNull();
        assertThat(container.getExposedPorts()).contains(6379);
    }

    @Test
    @DisplayName("应有 @TestConfiguration 注解")
    void given_whenCheckAnnotation_thenHasTestConfiguration() {
        // Then
        assertThat(RedisTestContainer.class.isAnnotationPresent(
            org.springframework.boot.test.context.TestConfiguration.class
        )).isTrue();
    }
}
