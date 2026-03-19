package com.cartisan.ai.provider;

import com.cartisan.ai.model.*;
import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.DomainException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

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

    // ── Sync delegation tests ─────────────────────────────────────────────────

    @Test
    void shouldTriggerListenerOnChat() {
        var listener = mock(ModelUsageListener.class);
        var registry = new ModelProviderRegistry(List.of(openai), List.of(listener));

        registry.chat("openai", sampleRequest("gpt-4o"));

        verify(listener).onUsage("openai", "gpt-4o", new TokenUsage(10, 20, 30));
    }

    @Test
    void shouldNotTriggerListener_whenProviderNotFound() {
        var listener = mock(ModelUsageListener.class);
        var registry = new ModelProviderRegistry(List.of(openai), List.of(listener));

        assertThatThrownBy(() -> registry.chat("unknown", sampleRequest("gpt-4o")))
                .isInstanceOf(DomainException.class);
        verifyNoInteractions(listener);
    }

    @Test
    void shouldNotPropagateListenerException() {
        var badListener = mock(ModelUsageListener.class);
        doThrow(new RuntimeException("billing down")).when(badListener)
                .onUsage(any(), any(), any());
        var registry = new ModelProviderRegistry(List.of(openai), List.of(badListener));

        // chat() must succeed despite listener failure
        ChatResponse response = registry.chat("openai", sampleRequest("gpt-4o"));
        assertThat(response.content()).isEqualTo("response from openai");
    }

    // ── Streaming delegation tests ────────────────────────────────────────────

    @Test
    void shouldTriggerListenerOnStream_exactlyOnce() {
        var listener = mock(ModelUsageListener.class);
        // Provider that emits two finished=true events (protocol violation)
        var badProtocolProvider = new FakeProvider("openai", "gpt-4o") {
            @Override
            public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
                return Flux.just(
                    new ChatStreamEvent("a", false, null),
                    new ChatStreamEvent("", true, new TokenUsage(10, 20, 30)),
                    new ChatStreamEvent("", true, new TokenUsage(99, 99, 99)) // duplicate finish
                );
            }
        };
        var registry = new ModelProviderRegistry(List.of(badProtocolProvider), List.of(listener));

        registry.chatStream("openai", sampleRequest("gpt-4o")).collectList().block();

        // Listener must fire exactly once with the FIRST finished event's usage
        verify(listener, times(1)).onUsage("openai", "gpt-4o", new TokenUsage(10, 20, 30));
    }

    @Test
    void shouldNotPropagateListenerException_onStream() {
        var badListener = mock(ModelUsageListener.class);
        doThrow(new RuntimeException("billing down")).when(badListener)
                .onUsage(any(), any(), any());
        var registry = new ModelProviderRegistry(List.of(openai), List.of(badListener));

        // Flux must complete normally despite listener failure
        List<ChatStreamEvent> events = registry.chatStream("openai", sampleRequest("gpt-4o"))
                .collectList().block();
        assertThat(events).hasSize(2);
        assertThat(events.getLast().finished()).isTrue();
    }

    @Test
    void shouldSkipListener_whenStreamFinishedEventHasNullUsage() {
        var listener = mock(ModelUsageListener.class);
        var nullUsageProvider = new FakeProvider("openai", "gpt-4o") {
            @Override
            public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
                return Flux.just(new ChatStreamEvent("", true, null)); // finished but no usage
            }
        };
        var registry = new ModelProviderRegistry(List.of(nullUsageProvider), List.of(listener));

        registry.chatStream("openai", sampleRequest("gpt-4o")).collectList().block();

        verifyNoInteractions(listener);
    }

    @Test
    void shouldNotTriggerListenerOnStream_whenProviderNotFound() {
        var listener = mock(ModelUsageListener.class);
        var registry = new ModelProviderRegistry(List.of(openai), List.of(listener));

        assertThatThrownBy(() -> registry.chatStream("unknown", sampleRequest("gpt-4o")))
                .isInstanceOf(DomainException.class);
        verifyNoInteractions(listener);
    }
}
