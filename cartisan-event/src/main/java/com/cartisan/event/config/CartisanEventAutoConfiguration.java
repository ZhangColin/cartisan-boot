package com.cartisan.event.config;

import com.cartisan.event.ApplicationEventPublisher;
import com.cartisan.event.impl.SpringApplicationEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * cartisan-event 模块的 Spring Boot 自动配置。
 */
@AutoConfiguration
public class CartisanEventAutoConfiguration {

    @Bean
    public ApplicationEventPublisher springEventPublisher(
            org.springframework.context.ApplicationEventPublisher springPublisher) {
        return new SpringApplicationEventPublisher(springPublisher);
    }
}
