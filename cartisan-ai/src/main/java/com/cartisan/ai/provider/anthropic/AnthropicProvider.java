package com.cartisan.ai.provider.anthropic;

import com.cartisan.ai.model.ChatRequest;
import com.cartisan.ai.model.ChatResponse;
import com.cartisan.ai.model.ChatStreamEvent;
import com.cartisan.ai.model.TokenUsage;
import com.cartisan.ai.provider.ModelProvider;
import com.cartisan.ai.provider.anthropic.dto.AnthropicChatRequest;
import com.cartisan.ai.provider.anthropic.dto.AnthropicChatResponse;
import com.cartisan.ai.provider.anthropic.dto.AnthropicStreamChunk;
import com.cartisan.core.exception.DomainException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Anthropic Claude Provider 实现。
 */
public class AnthropicProvider implements ModelProvider {

    private final AnthropicClient client;
    private final List<String> supportedModels;
    private final int defaultMaxTokens;

    public AnthropicProvider(AnthropicProperties properties) {
        this.client = new AnthropicClient(properties.getBaseUrl(), properties.getApiKey());
        this.supportedModels = List.copyOf(properties.getModels());
        this.defaultMaxTokens = properties.getDefaultMaxTokens();
    }

    /**
     * 测试用构造函数（支持注入 Mock client）。
     */
    AnthropicProvider(AnthropicClient client, List<String> models, int defaultMaxTokens) {
        this.client = client;
        this.supportedModels = List.copyOf(models);
        this.defaultMaxTokens = defaultMaxTokens;
    }

    @Override
    public String id() {
        return "anthropic";
    }

    @Override
    public List<String> supportedModels() {
        return supportedModels;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        AnthropicChatRequest anthropicRequest = AnthropicChatRequest.from(request, defaultMaxTokens);
        AnthropicChatResponse response = client.chat(anthropicRequest);

        // Anthropic content 是数组，取第一个 text 块
        String content = response.content().stream()
                .filter(b -> "text".equals(b.type()))
                .findFirst()
                .map(AnthropicChatResponse.ContentBlock::text)
                .orElse("");

        return new ChatResponse(
                content,
                response.model(),
                new TokenUsage(response.usage().inputTokens(), response.usage().outputTokens(),
                        response.usage().inputTokens() + response.usage().outputTokens())
        );
    }

    @Override
    public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
        AnthropicChatRequest anthropicRequest = AnthropicChatRequest.from(request.withStream(true), defaultMaxTokens);

        return Flux.defer(() -> {
            AtomicReference<TokenUsage> pendingUsage = new AtomicReference<>();

            return client.chatStream(anthropicRequest)
                    .filter(chunk -> !chunk.isPing()) // 忽略 ping 事件
                    .flatMap(chunk -> {
                        // 错误事件
                        if (chunk.isError()) {
                            return Flux.error(new DomainException(
                                    com.cartisan.core.exception.BaseCodeMessage.THIRD_PARTY_ERROR,
                                    chunk.getErrorMessage()));
                        }

                        // text delta
                        String delta = chunk.getTextDelta();
                        if (!delta.isEmpty()) {
                            return Flux.just(new ChatStreamEvent(delta, false, null));
                        }

                        // message_delta 包含 usage
                        if (chunk.isMessageDelta()) {
                            AnthropicStreamChunk.Usage usage = chunk.getUsage();
                            if (usage != null) {
                                TokenUsage tokenUsage = new TokenUsage(
                                        usage.inputTokens(),
                                        usage.outputTokens(),
                                        usage.inputTokens() + usage.outputTokens());
                                pendingUsage.set(tokenUsage);
                            }
                            return Flux.empty();
                        }

                        // message_stop: 流结束，发出最终的 finished event
                        if (chunk.isMessageStop()) {
                            TokenUsage usage = pendingUsage.getAndSet(null);
                            return Flux.just(new ChatStreamEvent("", true, usage));
                        }

                        return Flux.empty();
                    });
        });
    }
}
