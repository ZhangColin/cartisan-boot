package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRequestTest {

    @Test
    void shouldCreateChatRequest() {
        // Given
        List<ChatMessage> messages = List.of(new ChatMessage(Role.USER, "Hello"));

        // When
        ChatRequest request = new ChatRequest("gpt-4", messages, 0.7, 1000, false);

        // Then
        assertThat(request.model()).isEqualTo("gpt-4");
        assertThat(request.messages()).hasSize(1);
        assertThat(request.temperature()).isEqualTo(0.7);
        assertThat(request.maxTokens()).isEqualTo(1000);
        assertThat(request.stream()).isFalse();
    }

    @Test
    void shouldAllowNullOptionalFields() {
        // Given
        List<ChatMessage> messages = List.of(new ChatMessage(Role.USER, "Hello"));

        // When
        ChatRequest request = new ChatRequest("gpt-4", messages, null, null, false);

        // Then
        assertThat(request.temperature()).isNull();
        assertThat(request.maxTokens()).isNull();
    }

    @Test
    void shouldThrowException_whenModelIsNull() {
        List<ChatMessage> messages = List.of(new ChatMessage(Role.USER, "Hello"));

        assertThatThrownBy(() -> new ChatRequest(null, messages, null, null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("model must not be null");
    }

    @Test
    void shouldThrowException_whenMessagesIsNull() {
        assertThatThrownBy(() -> new ChatRequest("gpt-4", null, null, null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("messages must not be null");
    }

    @Test
    void shouldThrowException_whenMessagesIsEmpty() {
        assertThatThrownBy(() -> new ChatRequest("gpt-4", List.of(), null, null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("messages must not be empty");
    }

    @Test
    void shouldDefensivelyCopyMessages() {
        // Given
        ArrayList<ChatMessage> mutableList = new ArrayList<>();
        mutableList.add(new ChatMessage(Role.USER, "Hello"));
        ChatRequest request = new ChatRequest("gpt-4", mutableList, null, null, false);

        // When — 修改原始 List
        mutableList.add(new ChatMessage(Role.ASSISTANT, "Hi"));

        // Then — Record 内部不受影响
        assertThat(request.messages()).hasSize(1);
    }

    @Test
    void shouldReturnUnmodifiableMessages() {
        // Given
        List<ChatMessage> messages = List.of(new ChatMessage(Role.USER, "Hello"));
        ChatRequest request = new ChatRequest("gpt-4", messages, null, null, false);

        // Then
        assertThatThrownBy(() -> request.messages().add(new ChatMessage(Role.ASSISTANT, "Hi")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
