package com.cartisan.ai.provider.openaicompat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiStreamChunk(
        String id,
        List<StreamChoice> choices,
        Usage usage
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StreamChoice(Delta delta, String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Delta(String content) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Usage(int promptTokens, int completionTokens, int totalTokens) {
    }
}
