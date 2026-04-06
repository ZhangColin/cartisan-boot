package com.cartisan.data.jpa.repository.impl;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Repository 测试配置。
 */
@Configuration
@EntityScan("com.cartisan.data.jpa.repository.impl")
@EnableJpaRepositories(
    basePackages = "com.cartisan.data.jpa.repository.impl",
    repositoryBaseClass = BaseRepositoryImpl.class
)
public class RepositoryTestConfig {
}
