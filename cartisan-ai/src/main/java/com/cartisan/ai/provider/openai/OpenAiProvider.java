package com.cartisan.ai.provider.openai;

import com.cartisan.ai.provider.openaicompat.OpenAiChatRequest;
import com.cartisan.ai.provider.openaicompat.OpenAiChatResponse;
import com.cartisan.ai.provider.openaicompat.OpenAiStreamChunk;
import com.cartisan.ai.model.ChatRequest;
import com.cartisan.ai.model.ChatResponse;
import com.cartisan.ai.model.ChatStreamEvent;
import com.cartisan.ai.model.TokenUsage;
import com.cartisan.ai.provider.ModelProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class OpenAiProvider implements ModelProvider {

    private final OpenAiClient client;
    private final List<String> supportedModels;

    public OpenAiProvider(OpenAiProperties properties) {
        this.client = new OpenAiClient(properties.getBaseUrl(), properties.getApiKey());
        this.supportedModels = List.copyOf(properties.getModels());
    }

    OpenAiProvider(OpenAiClient client, List<String> models) {
        this.client = client;
        this.supportedModels = List.copyOf(models);
    }

    @Override
    public String id() {
        return "openai";
    }

    @Override
    public List<String> supportedModels() {
        return supportedModels;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        OpenAiChatRequest req = toOpenAiRequest(request, false);
        OpenAiChatResponse res = client.chat(req);
        return new ChatResponse(
                res.choices().get(0).message().content(),
                res.model(),
                new TokenUsage(res.usage().promptTokens(), res.usage().completionTokens(), res.usage().totalTokens())
        );
    }

    @Override
    public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
        OpenAiChatRequest req = toOpenAiRequest(request, true);
        return Flux.defer(() -> {
            AtomicReference<ChatStreamEvent> pendingFinished = new AtomicReference<>();
            return client.chatStream(req)
                    .concatMap(chunk -> {
                        // usage-only chunk: choices is empty, usage is non-null
                        if (chunk.choices().isEmpty() && chunk.usage() != null) {
                            ChatStreamEvent pending = pendingFinished.getAndSet(null);
                            if (pending != null) {
                                return Flux.just(new ChatStreamEvent(
                                        pending.delta(), true, toTokenUsage(chunk.usage())));
                            }
                            // usage chunk arrived without a preceding stop chunk — discard (orphaned, no matching finished event)
                            return Flux.empty();
                        }

                        boolean finished = !chunk.choices().isEmpty()
                                && "stop".equals(chunk.choices().get(0).finishReason());
                        String delta = chunk.choices().isEmpty() ? ""
                                : Optional.ofNullable(chunk.choices().get(0).delta().content()).orElse("");

                        if (finished) {
                            pendingFinished.set(new ChatStreamEvent(delta, true, null));
                            return Flux.empty(); // hold until usage arrives
                        }
                        return Flux.just(new ChatStreamEvent(delta, false, null));
                    })
                    .concatWith(Mono.defer(() -> {
                        // Fallback: stream ended but usage chunk never arrived (proxy compatibility)
                        ChatStreamEvent pending = pendingFinished.getAndSet(null);
                        return pending != null ? Mono.just(pending) : Mono.empty();
                    }));
        });
    }

    private OpenAiChatRequest toOpenAiRequest(ChatRequest request, boolean stream) {
        List<Map<String, String>> messages = request.messages().stream()
                .map(m -> Map.of("role", m.role().name().toLowerCase(), "content", m.content()))
                .toList();
        Map<String, Object> streamOptions = stream ? Map.of("include_usage", true) : null;
        return new OpenAiChatRequest(request.model(), messages, request.temperature(),
                request.maxTokens(), stream, streamOptions);
    }

    private TokenUsage toTokenUsage(OpenAiStreamChunk.Usage u) {
        return new TokenUsage(u.promptTokens(), u.completionTokens(), u.totalTokens());
    }
}
