package com.cartisan.ai.provider.anthropic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * Anthropic Messages API 响应体。
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnthropicChatResponse(
        String id,
        String type,
        String role,
        List<ContentBlock> content,
        String model,
        String stopReason,
        Usage usage
) {
    /**
     * 内容块，当前仅支持 text 类型。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text) {
        public ContentBlock {
            if (!"text".equals(type)) {
                throw new IllegalArgumentException("Only text type is supported");
            }
        }
    }

    /**
     * Token 使用量。
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(int inputTokens, int outputTokens) {
    }
}
