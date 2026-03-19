package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatStreamEventTest {

    @Test
    void shouldCreateStreamingEvent() {
        // Given & When
        ChatStreamEvent event = new ChatStreamEvent("Hello", false, null);

        // Then
        assertThat(event.delta()).isEqualTo("Hello");
        assertThat(event.finished()).isFalse();
        assertThat(event.usage()).isNull();
    }

    @Test
    void shouldCreateFinishedEventWithUsage() {
        // Given
        TokenUsage usage = new TokenUsage(10, 20, 30);

        // When
        ChatStreamEvent event = new ChatStreamEvent("", true, usage);

        // Then
        assertThat(event.delta()).isEmpty();
        assertThat(event.finished()).isTrue();
        assertThat(event.usage()).isEqualTo(usage);
    }

    @Test
    void shouldThrowException_whenDeltaIsNull() {
        assertThatThrownBy(() -> new ChatStreamEvent(null, false, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("delta must not be null");
    }

    @Test
    void shouldBeEqual_whenSameValues() {
        // Given
        ChatStreamEvent event1 = new ChatStreamEvent("Hi", false, null);
        ChatStreamEvent event2 = new ChatStreamEvent("Hi", false, null);

        // Then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }
}
