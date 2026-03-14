package com.cartisan.event.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * cartisan-event 集成测试配置。
 */
@Configuration
@ImportAutoConfiguration(classes = com.cartisan.event.config.CartisanEventAutoConfiguration.class)
public class TestConfiguration {
}
