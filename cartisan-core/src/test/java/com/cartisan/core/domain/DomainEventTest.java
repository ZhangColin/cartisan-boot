package com.cartisan.core.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DomainEvent 基类测试。
 *
 * <p>验证领域事件的元数据自动生成和基本行为。</p>
 */
class DomainEventTest {

    /**
     * 测试用领域事件。
     */
    private static class TestDomainEvent extends DomainEvent {
        TestDomainEvent(String aggregateId) {
            super(aggregateId);
        }
    }

    @Test
    void shouldGenerateEventId_whenCreatingEventWithValidAggregateId() {
        // Given
        String aggregateId = "order-123";

        // When
        TestDomainEvent event = new TestDomainEvent(aggregateId);

        // Then
        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventId()).isInstanceOf(String.class);
        assertThat(event.eventId()).hasSize(36); // UUID 格式: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
    }

    @Test
    void shouldSetOccurredAtToNow_whenCreatingEventWithValidAggregateId() {
        // Given
        String aggregateId = "order-123";
        Instant before = Instant.now();

        // When
        TestDomainEvent event = new TestDomainEvent(aggregateId);
        Instant after = Instant.now();

        // Then
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.occurredAt()).isBetween(before, after);
    }

    @Test
    void shouldThrowNullPointerException_whenAggregateIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> new TestDomainEvent(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("aggregateId");
    }

    @Test
    void shouldStoreAggregateId_whenCreatingEventWithValidAggregateId() {
        // Given
        String aggregateId = "order-456";

        // When
        TestDomainEvent event = new TestDomainEvent(aggregateId);

        // Then
        assertThat(event.aggregateId()).isEqualTo(aggregateId);
    }

    @Test
    void shouldReturnClassName_whenGettingEventType() {
        // Given
        TestDomainEvent event = new TestDomainEvent("order-123");

        // When
        String eventType = event.eventType();

        // Then
        assertThat(eventType).isEqualTo("TestDomainEvent");
    }

    @Test
    void shouldGenerateUniqueEventId_whenCreatingMultipleEvents() {
        // Given
        String aggregateId = "order-123";

        // When
        TestDomainEvent event1 = new TestDomainEvent(aggregateId);
        TestDomainEvent event2 = new TestDomainEvent(aggregateId);

        // Then
        assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
    }

    @Test
    void shouldHaveDifferentOccurredAt_whenCreatingMultipleEvents() {
        // Given
        String aggregateId = "order-123";

        // When - 添加微小的延迟确保时间戳不同
        TestDomainEvent event1 = new TestDomainEvent(aggregateId);
        try {
            Thread.sleep(1); // 1ms 延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        TestDomainEvent event2 = new TestDomainEvent(aggregateId);

        // Then
        assertThat(event1.occurredAt()).isBefore(event2.occurredAt());
    }
}
