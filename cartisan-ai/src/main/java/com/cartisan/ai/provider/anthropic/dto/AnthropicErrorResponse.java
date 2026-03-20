package com.cartisan.ai.provider.anthropic.dto;

/**
 * Anthropic HTTP 错误响应。
 */
public record AnthropicErrorResponse(Error error) {
    public record Error(String type, String message) {
    }
}
