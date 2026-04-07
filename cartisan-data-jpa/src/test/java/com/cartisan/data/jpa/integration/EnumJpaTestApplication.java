package com.cartisan.data.jpa.integration;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 测试应用配置，用于 BaseEnum JPA 集成测试。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.cartisan.data.jpa.integration")
@EnableJpaRepositories(basePackages = "com.cartisan.data.jpa.integration")
public class EnumJpaTestApplication {
}
