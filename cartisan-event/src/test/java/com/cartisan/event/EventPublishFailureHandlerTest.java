package com.cartisan.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EventPublishFailureHandlerTest {

    @Test
    void should_not_throw_when_logging_handler_handles_failure() {
        // Given
        LoggingEventPublishFailureHandler handler = new LoggingEventPublishFailureHandler();
        ApplicationEvent event = new ApplicationEvent() {
            @Override
            public String eventId() { return "test-id"; }
            @Override
            public Instant occurredAt() { return Instant.now(); }
            @Override
            public String eventType() { return "test.event"; }
        };
        Exception exception = new RuntimeException("Connection refused");

        // When & Then
        assertThatCode(() -> handler.onFailure("spring", event, exception))
            .doesNotThrowAnyException();
    }

    @Test
    void should_invoke_custom_handler_on_failure() {
        // Given
        StringBuilder captured = new StringBuilder();
        EventPublishFailureHandler customHandler = (publisherType, event, exception) ->
            captured.append(publisherType).append(":").append(event.eventId());

        ApplicationEvent event = new ApplicationEvent() {
            @Override
            public String eventId() { return "evt-123"; }
            @Override
            public Instant occurredAt() { return Instant.now(); }
            @Override
            public String eventType() { return "test.event"; }
        };

        // When
        customHandler.onFailure("rabbitmq", event, new RuntimeException("error"));

        // Then
        assertThat(captured.toString()).isEqualTo("rabbitmq:evt-123");
    }
}
