package com.cartisan.data.jpa.repository.impl;

import com.cartisan.core.domain.DomainEvent;
import com.cartisan.event.DomainEventPublisher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository 测试配置。
 *
 * <p>配置测试用的 Repository 和 DomainEventPublisher。</p>
 */
@Configuration
@EntityScan("com.cartisan.data.jpa.repository.impl")
@EnableJpaRepositories(
    basePackages = "com.cartisan.data.jpa.repository.impl",
    repositoryBaseClass = BaseRepositoryImpl.class
)
public class RepositoryTestConfig {

    @Bean
    public TestEventCollector testEventCollector() {
        return new TestEventCollector();
    }

    @Bean
    public DomainEventPublisher domainEventPublisher(TestEventCollector collector) {
        return new DomainEventPublisher() {
            @Override
            public void publish(DomainEvent event) {
                collector.collect(event);
            }
        };
    }

    @Bean
    public TestEventListener testEventListener(TestEventCollector collector) {
        return new TestEventListener(collector);
    }

    @Bean
    public DomainEventPublisherHolderConfigurer domainEventPublisherHolderConfigurer(DomainEventPublisher publisher) {
        return new DomainEventPublisherHolderConfigurer(publisher);
    }

    /**
     * 配置器，在容器初始化时设置 DomainEventPublisherHolder。
     */
    static class DomainEventPublisherHolderConfigurer implements InitializingBean {
        private final DomainEventPublisher publisher;

        DomainEventPublisherHolderConfigurer(DomainEventPublisher publisher) {
            this.publisher = publisher;
        }

        @Override
        public void afterPropertiesSet() {
            DomainEventPublisherHolder.setPublisher(publisher);
        }
    }

    /**
     * 领域事件监听器，用于测试。
     */
    static class TestEventListener {
        private final TestEventCollector collector;

        TestEventListener(TestEventCollector collector) {
            this.collector = collector;
        }

        @EventListener
        public void handle(TestDomainEvent event) {
            if (collector.shouldThrow()) {
                throw new RuntimeException("Listener exception");
            }
        }
    }

    /**
     * 测试事件收集器。
     */
    static class TestEventCollector {
        private final List<DomainEvent> events = new ArrayList<>();
        private boolean throwOnNextEvent = false;

        void collect(DomainEvent event) {
            events.add(event);
            if (throwOnNextEvent) {
                throw new RuntimeException("Listener exception");
            }
        }

        List<DomainEvent> getEvents() {
            return events;
        }

        void clear() {
            events.clear();
            throwOnNextEvent = false;
        }

        void setThrowOnNextEvent(boolean throwOnNextEvent) {
            this.throwOnNextEvent = throwOnNextEvent;
        }

        boolean shouldThrow() {
            return throwOnNextEvent;
        }
    }
}
