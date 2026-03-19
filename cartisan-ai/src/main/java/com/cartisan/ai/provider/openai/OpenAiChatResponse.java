package com.cartisan.ai.provider.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiChatResponse(
        String id,
        String model,
        List<Choice> choices,
        Usage usage
) {
    record Choice(Message message) {
    }

    record Message(String role, String content) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Usage(int promptTokens, int completionTokens, int totalTokens) {
    }
}
