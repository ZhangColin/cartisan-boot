package com.cartisan.data.jpa.repository.impl;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 测试应用配置。
 *
 * <p>作为 @DataJpaTest 的 @SpringBootConfiguration。</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("com.cartisan.data.jpa.repository.impl")
@EnableJpaRepositories(
    basePackages = "com.cartisan.data.jpa.repository.impl",
    repositoryBaseClass = BaseRepositoryImpl.class
)
public class TestApplication {
}
