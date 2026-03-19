package com.cartisan.ai.model;

import java.util.List;
import java.util.Objects;

public record ChatRequest(
        String model,
        List<ChatMessage> messages,
        Double temperature,
        Integer maxTokens,
        boolean stream
) {
    public ChatRequest {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(messages, "messages must not be null");
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        messages = List.copyOf(messages);
    }
}
