package com.cartisan.ai.provider.anthropic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Anthropic Messages API 响应体。
 */
public record AnthropicChatResponse(
        String id,
        String type,
        String role,
        List<ContentBlock> content,
        String model,
        @JsonProperty("stop_reason") String stopReason,
        Usage usage
) {
    /**
     * 内容块，当前仅支持 text 类型。
     */
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
    public record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens) {
    }
}
