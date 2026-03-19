package com.cartisan.ai.provider;

import com.cartisan.ai.model.*;
import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.DomainException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModelProviderRegistryTest {

    // ── Test doubles ─────────────────────────────────────────────────────────

    static ChatRequest sampleRequest(String model) {
        return new ChatRequest(model, List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, false);
    }

    static class FakeProvider implements ModelProvider {
        private final String id;
        private final List<String> models;

        FakeProvider(String id, String... models) {
            this.id = id;
            this.models = List.of(models);
        }

        @Override public String id() { return id; }
        @Override public List<String> supportedModels() { return models; }

        @Override
        public ChatResponse chat(ChatRequest request) {
            return new ChatResponse("response from " + id, request.model(),
                    new TokenUsage(10, 20, 30));
        }

        @Override
        public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
            return Flux.just(
                new ChatStreamEvent("hello", false, null),
                new ChatStreamEvent("", true, new TokenUsage(10, 20, 30))
            );
        }
    }

    private final FakeProvider openai    = new FakeProvider("openai", "gpt-4o", "gpt-4o-mini");
    private final FakeProvider anthropic = new FakeProvider("anthropic", "claude-3-5-sonnet");

    // ── Lookup tests ─────────────────────────────────────────────────────────

    @Test
    void shouldReturnAllProviders() {
        var registry = new ModelProviderRegistry(List.of(openai, anthropic), List.of());

        assertThat(registry.listProviders()).containsExactlyInAnyOrder(openai, anthropic);
        // list must be unmodifiable
        assertThatThrownBy(() -> registry.listProviders().add(openai))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldReturnEmptyList_whenNoProviders() {
        var registry = new ModelProviderRegistry(List.of(), List.of());

        assertThat(registry.listProviders()).isEmpty();
    }

    @Test
    void shouldFindProviderById() {
        var registry = new ModelProviderRegistry(List.of(openai, anthropic), List.of());

        assertThat(registry.getProvider("openai")).isSameAs(openai);
    }

    @Test
    void shouldFindProviderByModel() {
        var registry = new ModelProviderRegistry(List.of(openai, anthropic), List.of());

        assertThat(registry.getProviderByModel("gpt-4o")).isSameAs(openai);
        assertThat(registry.getProviderByModel("claude-3-5-sonnet")).isSameAs(anthropic);
    }

    @Test
    void shouldThrowWhenProviderNotFound() {
        var registry = new ModelProviderRegistry(List.of(openai), List.of());

        assertThatThrownBy(() -> registry.getProvider("unknown"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCodeMessage())
                        .isEqualTo(BaseCodeMessage.RESOURCE_NOT_FOUND));
    }

    @Test
    void shouldThrowWhenModelNotFound() {
        var registry = new ModelProviderRegistry(List.of(openai), List.of());

        assertThatThrownBy(() -> registry.getProviderByModel("unknown-model"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCodeMessage())
                        .isEqualTo(BaseCodeMessage.RESOURCE_NOT_FOUND));
    }

    @Test
    void shouldThrowOnDuplicateModelName() {
        var conflict = new FakeProvider("deepseek", "gpt-4o"); // same model as openai

        assertThatThrownBy(() -> new ModelProviderRegistry(List.of(openai, conflict), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gpt-4o");
    }
}
