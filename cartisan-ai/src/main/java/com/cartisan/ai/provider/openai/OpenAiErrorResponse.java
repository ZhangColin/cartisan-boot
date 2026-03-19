package com.cartisan.ai.provider.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiErrorResponse(Error error) {
    record Error(String message, String type, String code) {
    }
}
