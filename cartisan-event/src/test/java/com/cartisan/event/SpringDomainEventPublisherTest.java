package com.cartisan.event;

import com.cartisan.core.domain.DomainEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link SpringDomainEventPublisher} 的单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SpringDomainEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    // ========================================================================
    // given_publisher_when_publish_then_eventDelegatedToApplicationEventPublisher
    // ========================================================================

    @Test
    void given_publisher_when_publish_then_eventDelegatedToApplicationEventPublisher() {
        // Given: 一个有效的领域事件和发布器
        TestDomainEvent event = new TestDomainEvent("aggregate-123");
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(applicationEventPublisher);

        // When: 发布事件
        publisher.publish(event);

        // Then: 事件应该被委托给 ApplicationEventPublisher
        verify(applicationEventPublisher).publishEvent(event);
    }

    // ========================================================================
    // given_nullEvent_when_publish_then_throwsNullPointerException
    // ========================================================================

    @Test
    void given_nullEvent_when_publish_then_throwsNullPointerException() {
        // Given: 发布器已创建
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(applicationEventPublisher);

        // When & Then: 发布 null 事件应抛出 NPE
        assertThatThrownBy(() -> publisher.publish(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("event cannot be null");

        // Then: ApplicationEventPublisher.publishEvent 不应被调用
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    // ========================================================================
    // Test Event Class
    // ========================================================================

    /**
     * 测试用的领域事件。
     */
    private static class TestDomainEvent extends DomainEvent {
        private final String testData;

        public TestDomainEvent(String aggregateId) {
            super(aggregateId);
            this.testData = aggregateId;
        }

        public String getTestData() {
            return testData;
        }
    }
}
