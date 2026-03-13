package com.cartisan.test.container;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

/**
 * Redis 7 测试容器预配置。
 *
 * <p>通过 {@code @ServiceConnection} 自动注入 Redis 连接属性。</p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 方式 1：通过 IntegrationTestBase 自动获得（推荐）
 * class MyTest extends IntegrationTestBase { ... }
 *
 * // 方式 2：只需要 Redis，不需要 PostgreSQL
 * @SpringBootTest
 * @Import(RedisTestContainer.class)
 * class MyTest { ... }
 * }</pre>
 *
 * @since 0.1.0
 */
@TestConfiguration(proxyBeanMethods = false)
public final class RedisTestContainer {

    private RedisTestContainer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 创建 Redis 容器。
     *
     * @return 预配置的 Redis 容器
     */
    @Bean
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis() {
        return new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    }
}
