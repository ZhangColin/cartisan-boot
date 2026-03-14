package com.cartisan.test.base;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 测试用的 Spring Boot 配置类。
 *
 * <p>提供最小化的自动配置，用于集成测试。</p>
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.cartisan.test.base")
public class TestConfiguration {

    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    }

    @Bean
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis() {
        return new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    }
}
