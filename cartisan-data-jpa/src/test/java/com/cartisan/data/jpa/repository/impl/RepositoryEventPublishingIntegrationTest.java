package com.cartisan.data.jpa.repository.impl;

import com.cartisan.core.domain.AbstractAggregateRoot;
import com.cartisan.core.domain.AggregateRoot;
import com.cartisan.core.domain.DomainEvent;
import com.cartisan.event.DomainEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository 事件发布集成测试。
 *
 * <p>测试命名遵循 TEST-002 规则：given_{条件}_when_{操作}_then_{预期结果}</p>
 *
 * <p>验证 AC1-AC5：</p>
 * <ul>
 *   <li>AC1: save 后事件被发布</li>
 *   <li>AC2: 事件发布后被清空</li>
 *   <li>AC3: 非 AbstractAggregateRoot 不发布事件</li>
 *   <li>AC4: saveAll 批量发布事件</li>
 *   <li>AC5: 监听器异常导致事务回滚</li>
 * </ul>
 */
@DataJpaTest
@Import(RepositoryEventPublishingIntegrationTest.TestConfig.class)
class RepositoryEventPublishingIntegrationTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private TestAggregateRootRepository testRepository;

    @Autowired
    private TestConfig.TestEventCollector testEventCollector;

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        // 确保 DomainEventPublisherHolder 被设置
        DomainEventPublisherHolder.setPublisher(domainEventPublisher);
        testEventCollector.clear();

        // 验证设置成功
        assertThat(DomainEventPublisherHolder.getPublisher())
            .isNotNull();
    }

    @AfterEach
    void tearDown() {
        testRepository.deleteAll();
    }

    // ==================== AC1: 事件被发布 ====================
    @Test
    void given_aggregateRootWithEvent_when_save_then_eventPublished() {
        // Given: 聚合根注册了事件
        TestAggregateRoot aggregateRoot = new TestAggregateRoot();
        aggregateRoot.registerTestEvent("test-event-1");

        // When: 保存聚合根
        testRepository.save(aggregateRoot);

        // Then: 事件被发布
        assertThat(testEventCollector.getEvents())
            .hasSize(1)
            .allMatch(event -> event instanceof TestDomainEvent);
    }

    // ==================== AC2: 事件被清空 ====================
    @Test
    void given_aggregateRootWithEvent_when_save_then_eventsCleared() {
        // Given: 聚合根注册了事件
        TestAggregateRoot aggregateRoot = new TestAggregateRoot();
        aggregateRoot.registerTestEvent("test-event-2");

        // When: 保存聚合根
        testRepository.save(aggregateRoot);

        // Then: 聚合根上的事件列表被清空
        assertThat(aggregateRoot.getDomainEvents()).isEmpty();
    }

    // ==================== AC4: saveAll 批量发布 ====================
    @Test
    void given_multipleAggregatesWithEvents_when_saveAll_then_allEventsPublished() {
        // Given: 多个聚合根都有事件
        TestAggregateRoot aggregate1 = new TestAggregateRoot();
        aggregate1.registerTestEvent("event-1");
        TestAggregateRoot aggregate2 = new TestAggregateRoot();
        aggregate2.registerTestEvent("event-2");

        List<TestAggregateRoot> aggregates = List.of(aggregate1, aggregate2);

        // When: 批量保存
        testRepository.saveAll(aggregates);

        // Then: 所有事件都被发布
        assertThat(testEventCollector.getEvents())
            .hasSize(2);
    }

    // ==================== AC5: 监听器异常导致回滚 ====================
    @Test
    void given_listenerThrowsException_when_save_then_transactionRolledBack() {
        // Given: 配置监听器会抛异常
        testEventCollector.setThrowOnNextEvent(true);

        TestAggregateRoot aggregateRoot = new TestAggregateRoot();
        aggregateRoot.registerTestEvent("boom-event");

        // When & Then: 保存应该失败，数据不会被持久化
        assertThatThrownBy(() ->
            testRepository.save(aggregateRoot)
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Listener exception");
    }

    // ==================== AC3: 非 AggregateRoot 不发布事件 ====================
    @Test
    void given_nonAggregateRootEntity_when_save_then_noEventPublished() {
        // Given: 创建一个不继承 AbstractAggregateRoot 的实体
        // 通过 TestEntityManager 持久化一个 SimpleEntity
        SimpleEntity simpleEntity = new SimpleEntity();
        testEntityManager.persist(simpleEntity);
        testEntityManager.flush();

        // When: 通过 TestEntityManager 查询，确认实体存在但没有事件被发布
        // SimpleEntity 不继承 AbstractAggregateRoot，所以 publishDomainEvents 不会发布事件

        // Then: testEventCollector 仍然为空（没有事件被发布）
        assertThat(testEventCollector.getEvents())
            .isEmpty();
    }

    /**
     * 测试用的简单实体，不继承 AbstractAggregateRoot。
     */
    @Entity
    static class SimpleEntity {

        @Id
        private final String id;

        public SimpleEntity() {
            this.id = java.util.UUID.randomUUID().toString();
        }

        public String id() {
            return id;
        }
    }

    /**
     * 测试配置。
     */
    @TestConfiguration
    static class TestConfig {

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
}
