package com.cartisan.test.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

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
    @DisplayName("构造函数应抛出 UnsupportedOperationException")
    void given_whenInstantiate_thenThrowsException() throws Exception {
        // Given
        Constructor<RedisTestContainer> constructor =
            RedisTestContainer.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When & Then
        Throwable exception = assertThrows(
            Exception.class,
            constructor::newInstance
        );
        assertThat(exception)
            .hasCauseExactlyInstanceOf(UnsupportedOperationException.class);
        assertThat(exception.getCause())
            .hasMessageContaining("Utility class");
    }

    @Test
    @DisplayName("类应为 final")
    void given_whenCheckClass_thenIsFinal() {
        // Then
        assertThat(Modifier.isFinal(RedisTestContainer.class.getModifiers())).isTrue();
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
