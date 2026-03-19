package com.cartisan.ai.model;

import java.util.Objects;

public record ChatMessage(Role role, String content) {
    public ChatMessage {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}
