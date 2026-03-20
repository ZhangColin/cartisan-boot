package com.cartisan.ai.provider.anthropic.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Anthropic 流式事件（SSE）。
 * <p>支持的事件类型：message_start, content_block_start, content_block_delta,
 * content_block_stop, message_delta, message_stop, ping, error
 */
public record AnthropicStreamChunk(
        String type,
        JsonNode data
) {
    /**
     * 获取 content_block_delta 的文本增量。
     */
    public String getTextDelta() {
        if (!"content_block_delta".equals(type)) {
            return "";
        }
        JsonNode delta = data.get("delta");
        if (delta != null && delta.has("text")) {
            return delta.get("text").asText();
        }
        return "";
    }

    /**
     * 检查是否是最终 message_delta 事件（含 usage）。
     */
    public boolean isMessageDelta() {
        return "message_delta".equals(type);
    }

    /**
     * 获取 message_delta 中的 usage。
     */
    public Usage getUsage() {
        if (!isMessageDelta()) {
            return null;
        }
        JsonNode usageNode = data.get("usage");
        if (usageNode == null) {
            return null;
        }
        // message_delta only contains output_tokens, input_tokens is in message_start
        JsonNode outputTokensNode = usageNode.get("output_tokens");
        if (outputTokensNode == null || !outputTokensNode.isInt()) {
            return null;
        }
        return new Usage(
                0, // input_tokens not available in message_delta
                outputTokensNode.asInt()
        );
    }

    /**
     * 检查是否是 message_stop（流结束）。
     */
    public boolean isMessageStop() {
        return "message_stop".equals(type);
    }

    /**
     * 检查是否是 ping 事件（应忽略）。
     */
    public boolean isPing() {
        return "ping".equals(type);
    }

    /**
     * 检查是否是错误事件。
     */
    public boolean isError() {
        return "error".equals(type);
    }

    /**
     * 获取错误信息。
     */
    public String getErrorMessage() {
        if (!isError()) {
            return null;
        }
        JsonNode errorNode = data.get("error");
        if (errorNode != null && errorNode.has("message")) {
            return errorNode.get("message").asText();
        }
        return "Unknown error";
    }

    /**
     * Token 使用量（复用 AnthropicChatResponse.Usage）。
     */
    public record Usage(int inputTokens, int outputTokens) {
    }
}
