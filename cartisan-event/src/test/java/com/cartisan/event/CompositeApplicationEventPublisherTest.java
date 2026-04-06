package com.cartisan.event;

import com.cartisan.event.example.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositeApplicationEventPublisherTest {

    @Mock
    private ApplicationEventPublisher springPublisher;

    @Mock
    private ApplicationEventPublisher rabbitmqPublisher;

    private CompositeApplicationEventPublisher compositePublisher;

    @BeforeEach
    void setUp() {
        when(springPublisher.getType()).thenReturn("spring");
        when(rabbitmqPublisher.getType()).thenReturn("rabbitmq");

        compositePublisher = new CompositeApplicationEventPublisher(
            List.of(springPublisher, rabbitmqPublisher)
        );
    }

    @Test
    void should_publish_to_multiple_publishers_when_annotation_has_multiple_types() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(1L, "customer", BigDecimal.valueOf(100));

        // When
        compositePublisher.publishApplicationEvent(event);

        // Then
        verify(springPublisher).publishApplicationEvent(event);
        verify(rabbitmqPublisher).publishApplicationEvent(event);
    }

    @Test
    void should_publish_to_spring_by_default_when_no_annotation() {
        // Given
        ApplicationEvent event = new ApplicationEvent() {
            @Override
            public String eventId() { return "test-id"; }
            @Override
            public Instant occurredAt() { return Instant.now(); }
            @Override
            public String eventType() { return "test.event"; }
        };

        // When
        compositePublisher.publishApplicationEvent(event);

        // Then
        verify(springPublisher).publishApplicationEvent(event);
        verify(rabbitmqPublisher, never()).publishApplicationEvent(any());
    }

    @Test
    void should_throw_exception_when_publisher_type_not_found() {
        // Given
        TestEvent event = new TestEvent(UUID.randomUUID().toString(), Instant.now());

        // When & Then
        assertThatThrownBy(() -> compositePublisher.publishApplicationEvent(event))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No publisher found for type: nonexistent");
    }
}
