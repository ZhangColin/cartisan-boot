package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatResponseTest {

    @Test
    void shouldCreateChatResponse() {
        // Given
        TokenUsage usage = new TokenUsage(10, 20, 30);

        // When
        ChatResponse response = new ChatResponse("Hello!", "gpt-4", usage);

        // Then
        assertThat(response.content()).isEqualTo("Hello!");
        assertThat(response.model()).isEqualTo("gpt-4");
        assertThat(response.usage()).isEqualTo(usage);
    }

    @Test
    void shouldThrowException_whenContentIsNull() {
        TokenUsage usage = new TokenUsage(10, 20, 30);

        assertThatThrownBy(() -> new ChatResponse(null, "gpt-4", usage))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("content must not be null");
    }

    @Test
    void shouldThrowException_whenModelIsNull() {
        TokenUsage usage = new TokenUsage(10, 20, 30);

        assertThatThrownBy(() -> new ChatResponse("Hello!", null, usage))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("model must not be null");
    }

    @Test
    void shouldThrowException_whenUsageIsNull() {
        assertThatThrownBy(() -> new ChatResponse("Hello!", "gpt-4", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("usage must not be null");
    }

    @Test
    void shouldBeEqual_whenSameValues() {
        // Given
        TokenUsage usage = new TokenUsage(10, 20, 30);
        ChatResponse resp1 = new ChatResponse("Hello!", "gpt-4", usage);
        ChatResponse resp2 = new ChatResponse("Hello!", "gpt-4", usage);

        // Then
        assertThat(resp1).isEqualTo(resp2);
        assertThat(resp1.hashCode()).isEqualTo(resp2.hashCode());
    }
}
