package com.cartisan.test.base;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 测试用的 Spring Boot 配置类。
 *
 * <p>提供最小化的自动配置，用于集成测试。</p>
 */
@SpringBootConfiguration
@ImportAutoConfiguration({
    JacksonAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    RedisAutoConfiguration.class
})
@ComponentScan(basePackages = "com.cartisan.test.base")
public class TestConfiguration {
}
