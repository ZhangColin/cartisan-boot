package com.cartisan.test.container;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * PostgreSQL 16 测试容器预配置。
 *
 * <p>通过 {@code @ServiceConnection} 自动注入 DataSource 属性，
 * 业务项目无需手动配置连接信息。</p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 方式 1：通过 IntegrationTestBase 自动获得（推荐）
 * class MyTest extends IntegrationTestBase { ... }
 *
 * // 方式 2：只需要 PostgreSQL，不需要 Redis
 * @SpringBootTest
 * @Import(PostgresTestContainer.class)
 * class MyTest { ... }
 * }</pre>
 *
 * @since 0.1.0
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainer {

    /**
     * 创建 PostgreSQL 容器。
     *
     * <p>容器在 Spring 上下文启动时自动启动，
     * Spring Boot 通过 {@code @ServiceConnection} 自动注入 DataSource。</p>
     *
     * @return 预配置的 PostgreSQL 容器
     */
    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    }
}
