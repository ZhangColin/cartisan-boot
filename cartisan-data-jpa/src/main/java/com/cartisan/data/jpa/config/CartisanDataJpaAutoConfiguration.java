package com.cartisan.data.jpa.config;

import com.cartisan.data.jpa.repository.impl.DomainEventPublisherHolder;
import com.cartisan.event.DomainEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * cartisan-data-jpa 模块自动配置。
 *
 * <p>配置领域事件发布器持有者，使 Repository 实例能够发布领域事件。</p>
 */
@AutoConfiguration
public class CartisanDataJpaAutoConfiguration {

    /**
     * 配置领域事件发布器持有者。
     *
     * <p>将 {@link DomainEventPublisher} Bean 注入到
     * {@link DomainEventPublisherHolder} 中，供 Repository 实例使用。</p>
     *
     * @param publisher 领域事件发布器，由 Spring 提供
     * @return 配置器 Runnable，在容器启动时执行
     */
    @Bean
    public Runnable configureDomainEventPublisherHolder(DomainEventPublisher publisher) {
        return () -> DomainEventPublisherHolder.setPublisher(publisher);
    }
}
