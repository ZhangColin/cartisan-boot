package com.cartisan.ai.provider.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
record OpenAiChatRequest(
        String model,
        List<Map<String, String>> messages,
        Double temperature,
        Integer maxTokens,
        // Primitive boolean: NON_NULL does not affect primitives, so `stream` is ALWAYS serialized (intentional).
        boolean stream,
        Map<String, Object> streamOptions
) {
}
