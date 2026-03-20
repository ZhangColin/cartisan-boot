package com.cartisan.ai.provider.anthropic.dto;

import com.cartisan.ai.model.ChatMessage;
import com.cartisan.ai.model.Role;

import java.util.List;

/**
 * Anthropic Messages API 请求体。
 *
 * @see <a href="https://docs.anthropic.com/en/api/messages">Anthropic Messages API</a>
 */
public record AnthropicChatRequest(
        String model,
        List<AnthropicMessage> messages,
        int maxTokens,
        Double temperature,
        boolean stream
) {
    public AnthropicChatRequest {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
    }

    /**
     * 从统一 ChatRequest 转换。
     */
    public static AnthropicChatRequest from(com.cartisan.ai.model.ChatRequest request, int defaultMaxTokens) {
        List<AnthropicMessage> messages = request.messages().stream()
                .map(m -> new AnthropicMessage(
                        m.role() == Role.USER ? "user" : "assistant",
                        m.content()))
                .toList();
        int maxTokens = request.maxTokens() != null ? request.maxTokens() : defaultMaxTokens;
        return new AnthropicChatRequest(request.model(), messages, maxTokens, request.temperature(), request.stream());
    }

    /**
     * Anthropic 消息格式（role + content 字符串）。
     */
    public record AnthropicMessage(String role, String content) {
        public AnthropicMessage {
            if (!role.equals("user") && !role.equals("assistant")) {
                throw new IllegalArgumentException("role must be 'user' or 'assistant'");
            }
        }
    }
}
