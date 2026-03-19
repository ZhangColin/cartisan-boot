package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageTest {

    @Test
    void shouldCreateChatMessage() {
        // Given & When
        ChatMessage message = new ChatMessage(Role.USER, "Hello");

        // Then
        assertThat(message.role()).isEqualTo(Role.USER);
        assertThat(message.content()).isEqualTo("Hello");
    }

    @Test
    void shouldThrowException_whenRoleIsNull() {
        assertThatThrownBy(() -> new ChatMessage(null, "Hello"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("role must not be null");
    }

    @Test
    void shouldThrowException_whenContentIsNull() {
        assertThatThrownBy(() -> new ChatMessage(Role.USER, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("content must not be null");
    }

    @Test
    void shouldBeEqual_whenSameRoleAndContent() {
        // Given
        ChatMessage msg1 = new ChatMessage(Role.USER, "Hello");
        ChatMessage msg2 = new ChatMessage(Role.USER, "Hello");

        // Then
        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
    }
}
