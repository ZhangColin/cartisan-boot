package com.cartisan.ai.model;

import java.util.Objects;

public record ChatResponse(String content, String model, TokenUsage usage) {
    public ChatResponse {
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(usage, "usage must not be null");
    }
}
