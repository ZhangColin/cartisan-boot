package com.cartisan.ai.provider;

import com.cartisan.ai.model.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelProviderContractTest {

    // FakeModelProvider：最小化满足接口契约的测试替身
    static class FakeModelProvider implements ModelProvider {
        @Override
        public String id() {
            return "fake";
        }

        @Override
        public List<String> supportedModels() {
            return List.of("fake-model-v1");
        }

        @Override
        public ChatResponse chat(ChatRequest request) {
            return new ChatResponse("fake response", request.model(), new TokenUsage(10, 20, 30));
        }

        @Override
        public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
            return Flux.just(
                new ChatStreamEvent("hello ", false, null),
                new ChatStreamEvent("world", false, null),
                new ChatStreamEvent("", true, new TokenUsage(10, 20, 30))
            );
        }
    }

    private final ModelProvider provider = new FakeModelProvider();

    private ChatRequest sampleRequest() {
        return new ChatRequest(
            "fake-model-v1",
            List.of(new ChatMessage(Role.USER, "Hi")),
            null, null, false
        );
    }

    private ChatRequest streamingRequest() {
        return new ChatRequest(
            "fake-model-v1",
            List.of(new ChatMessage(Role.USER, "Hi")),
            null, null, true
        );
    }

    @Test
    void shouldReturnNonBlankId() {
        assertThat(provider.id()).isNotBlank();
    }

    @Test
    void shouldReturnNonEmptySupportedModels() {
        assertThat(provider.supportedModels()).isNotEmpty();
    }

    @Test
    void shouldReturnValidChatResponse() {
        ChatResponse response = provider.chat(sampleRequest());

        assertThat(response.content()).isNotBlank();
        assertThat(response.model()).isEqualTo(sampleRequest().model());
        assertThat(response.usage()).isNotNull();
    }

    @Test
    void shouldReturnStreamWithFinishedEvent() {
        List<ChatStreamEvent> events = provider.chatStream(streamingRequest()).collectList().block();

        assertThat(events).isNotEmpty();

        ChatStreamEvent lastEvent = events.getLast();
        assertThat(lastEvent.finished()).isTrue();
        assertThat(lastEvent.usage()).isNotNull();
    }
}
