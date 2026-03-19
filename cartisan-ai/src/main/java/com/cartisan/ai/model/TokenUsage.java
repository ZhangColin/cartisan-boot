package com.cartisan.ai.model;

public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
    public TokenUsage {
        if (promptTokens < 0 || completionTokens < 0 || totalTokens < 0) {
            throw new IllegalArgumentException("Token counts must not be negative");
        }
    }
}
