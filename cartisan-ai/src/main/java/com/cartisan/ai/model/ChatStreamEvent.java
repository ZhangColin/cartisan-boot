package com.cartisan.ai.model;

import java.util.Objects;

public record ChatStreamEvent(String delta, boolean finished, TokenUsage usage) {
    public ChatStreamEvent {
        Objects.requireNonNull(delta, "delta must not be null");
    }
}
