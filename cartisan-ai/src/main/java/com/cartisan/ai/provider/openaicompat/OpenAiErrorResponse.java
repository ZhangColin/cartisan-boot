package com.cartisan.ai.provider.openaicompat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiErrorResponse(Error error) {
    public record Error(String message, String type, String code) {
    }
}
