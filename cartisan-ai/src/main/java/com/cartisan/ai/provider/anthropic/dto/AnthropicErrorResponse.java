package com.cartisan.ai.provider.anthropic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Anthropic HTTP 错误响应。
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnthropicErrorResponse(Error error) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(String type, String message) {
    }
}
