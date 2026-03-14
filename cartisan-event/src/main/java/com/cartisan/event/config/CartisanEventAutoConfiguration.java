package com.cartisan.event.config;

import com.cartisan.event.DomainEventPublisher;
import com.cartisan.event.SpringDomainEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * cartisan-event 模块的 Spring Boot 自动配置。
 *
 * <p>当类路径中存在 cartisan-event 且用户未自定义 {@link DomainEventPublisher} Bean 时，
 * 自动注册 {@link SpringDomainEventPublisher} 作为默认实现。</p>
 *
 * <h2>用户自定义覆盖</h2>
 *
 * <p>用户可通过定义自己的 {@code DomainEventPublisher} Bean 来覆盖默认实现：</p>
 *
 * <pre>{@code
 * @Configuration
 * class CustomEventConfig {
 *     @Bean
 *     DomainEventPublisher customPublisher() {
 *         return new KafkaDomainEventPublisher(); // 例如发到 Kafka
 *     }
 * }
 * }</pre>
 *
 * <p>由于 {@code @ConditionalOnMissingBean} 的存在，用户定义 Bean 后，
 * 默认的 {@code SpringDomainEventPublisher} 不会注册。</p>
 *
 * @since 0.1.0
 */
@Configuration
@ConditionalOnMissingBean(DomainEventPublisher.class)
public class CartisanEventAutoConfiguration {

    /**
     * 注册领域事件发布器 Bean。
     *
     * @param applicationEventPublisher Spring 事件发布器
     * @return 领域事件发布器实例
     */
    @Bean
    public DomainEventPublisher domainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new SpringDomainEventPublisher(applicationEventPublisher);
    }
}
