package com.cartisan.ai.provider.openaicompat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiChatResponse(
        String id,
        String model,
        List<Choice> choices,
        Usage usage
) {
    public record Choice(Message message) {
    }

    public record Message(String role, String content) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Usage(int promptTokens, int completionTokens, int totalTokens) {
    }
}
