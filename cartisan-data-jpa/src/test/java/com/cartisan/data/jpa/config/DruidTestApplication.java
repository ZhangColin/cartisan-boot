package com.cartisan.data.jpa.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

/**
 * Druid 自动配置测试应用。
 *
 * <p>使用 @SpringBootConfiguration 而非 @SpringBootApplication，
 * 避免与其他测试应用类冲突。</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = DruidTestApplication.class)
public class DruidTestApplication {
}
