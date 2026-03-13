package com.cartisan.core.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AbstractAggregateRoot 抽象类测试。
 *
 * <p>验证聚合根的事件管理功能。</p>
 */
class AbstractAggregateRootTest {

    /**
     * 测试用聚合根。
     */
    private static class TestAggregateRoot extends AbstractAggregateRoot<TestAggregateRoot> {
        private final String id;

        TestAggregateRoot(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        void doSomething() {
            registerEvent(new TestDomainEvent(id));
        }
    }

    /**
     * 测试用领域事件。
     */
    private static class TestDomainEvent extends DomainEvent {
        TestDomainEvent(String aggregateId) {
            super(aggregateId);
        }
    }

    @Test
    void shouldAddEvent_whenRegisteringEvent() {
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot("agg-1");
        TestDomainEvent event = new TestDomainEvent("agg-1");

        // When
        aggregate.registerEvent(event);

        // Then
        assertThat(aggregate.getDomainEvents()).hasSize(1);
        assertThat(aggregate.getDomainEvents().getFirst()).isEqualTo(event);
    }

    @Test
    void shouldAddAllEvents_whenRegisteringMultipleEvents() {
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot("agg-1");
        TestDomainEvent event1 = new TestDomainEvent("agg-1");
        TestDomainEvent event2 = new TestDomainEvent("agg-1");

        // When
        aggregate.registerEvent(event1);
        aggregate.registerEvent(event2);

        // Then
        assertThat(aggregate.getDomainEvents()).hasSize(2);
        assertThat(aggregate.getDomainEvents()).containsExactly(event1, event2);
    }

    @Test
    void shouldThrowNullPointerException_whenRegisteringNullEvent() {
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot("agg-1");

        // When & Then
        assertThatThrownBy(() -> aggregate.registerEvent(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("event");
    }

    @Test
    void shouldReturnUnmodifiableList_whenGettingDomainEvents() {
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot("agg-1");
        aggregate.registerEvent(new TestDomainEvent("agg-1"));
        List<DomainEvent> events = aggregate.getDomainEvents();

        // When & Then - 尝试修改应抛出异常
        assertThatThrownBy(() -> events.add(new TestDomainEvent("agg-1")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldClearList_whenClearingDomainEvents() {
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot("agg-1");
        aggregate.registerEvent(new TestDomainEvent("agg-1"));
        assertThat(aggregate.getDomainEvents()).hasSize(1);

        // When
        aggregate.clearDomainEvents();

        // Then
        assertThat(aggregate.getDomainEvents()).isEmpty();
    }

    @Test
    void shouldNotThrowException_whenClearingDomainEventsMultipleTimes() {
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot("agg-1");
        aggregate.registerEvent(new TestDomainEvent("agg-1"));

        // When - 多次清空不应抛出异常
        aggregate.clearDomainEvents();
        aggregate.clearDomainEvents();
        aggregate.clearDomainEvents();

        // Then
        assertThat(aggregate.getDomainEvents()).isEmpty();
    }

    @Test
    void shouldAddEvent_whenRegisteringEventAfterClear() {
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot("agg-1");
        aggregate.registerEvent(new TestDomainEvent("agg-1"));
        aggregate.clearDomainEvents();

        // When
        aggregate.registerEvent(new TestDomainEvent("agg-1"));

        // Then
        assertThat(aggregate.getDomainEvents()).hasSize(1);
    }

    @Test
    void shouldRegisterEvent_whenDoingSomething() {
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot("agg-1");

        // When
        aggregate.doSomething();

        // Then
        assertThat(aggregate.getDomainEvents()).hasSize(1);
        assertThat(aggregate.getDomainEvents().getFirst().aggregateId()).isEqualTo("agg-1");
    }
}
