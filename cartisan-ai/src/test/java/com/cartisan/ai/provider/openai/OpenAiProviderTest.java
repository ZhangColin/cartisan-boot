package com.cartisan.ai.provider.openai;

import com.cartisan.ai.model.*;
import com.cartisan.ai.provider.openaicompat.OpenAiChatRequest;
import com.cartisan.ai.provider.openaicompat.OpenAiChatResponse;
import com.cartisan.ai.provider.openaicompat.OpenAiCompatibleClient;
import com.cartisan.ai.provider.openaicompat.OpenAiStreamChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiProviderTest {

    private OpenAiCompatibleClient mockClient;
    private OpenAiProvider provider;

    @BeforeEach
    void setUp() {
        mockClient = mock(OpenAiCompatibleClient.class);
        provider = new OpenAiProvider(mockClient, List.of("gpt-4o", "gpt-4o-mini"));
    }

    // ─── Metadata ────────────────────────────────────────────────────────────

    @Test
    void shouldReturnOpenAi_whenIdCalled() {
        assertThat(provider.id()).isEqualTo("openai");
    }

    @Test
    void shouldReturnConfiguredModels_whenSupportedModelsCalled() {
        assertThat(provider.supportedModels()).containsExactly("gpt-4o", "gpt-4o-mini");
    }

    // ─── Sync chat() ─────────────────────────────────────────────────────────

    @Test
    void shouldMapChatRequestToOpenAiFormat() {
        ArgumentCaptor<OpenAiChatRequest> captor = ArgumentCaptor.forClass(OpenAiChatRequest.class);
        when(mockClient.chat(captor.capture())).thenReturn(fakeOpenAiResponse());

        ChatRequest request = new ChatRequest("gpt-4o",
                List.of(new ChatMessage(Role.USER, "Hello")), null, null, false);
        provider.chat(request);

        OpenAiChatRequest captured = captor.getValue();
        assertThat(captured.model()).isEqualTo("gpt-4o");
        assertThat(captured.stream()).isFalse();
        assertThat(captured.streamOptions()).isNull();
        assertThat(captured.messages()).hasSize(1);
        assertThat(captured.messages().get(0).get("role")).isEqualTo("user");
        assertThat(captured.messages().get(0).get("content")).isEqualTo("Hello");
    }

    @Test
    void shouldMapRolesToLowercase_whenBuildingMessages() {
        ArgumentCaptor<OpenAiChatRequest> captor = ArgumentCaptor.forClass(OpenAiChatRequest.class);
        when(mockClient.chat(captor.capture())).thenReturn(fakeOpenAiResponse());

        ChatRequest request = new ChatRequest("gpt-4o",
                List.of(new ChatMessage(Role.USER, "Hello")), null, null, false);
        provider.chat(request);

        Map<String, String> msg = captor.getValue().messages().get(0);
        assertThat(msg.get("role")).isEqualTo("user");
        assertThat(msg.get("content")).isEqualTo("Hello");
    }

    @Test
    void shouldMapOpenAiResponseToChatResponse() {
        when(mockClient.chat(any())).thenReturn(fakeOpenAiResponse());

        ChatRequest request = new ChatRequest("gpt-4o",
                List.of(new ChatMessage(Role.USER, "Hello")), null, null, false);
        ChatResponse response = provider.chat(request);

        assertThat(response.content()).isEqualTo("Hi there!");
        assertThat(response.model()).isEqualTo("gpt-4o");
        assertThat(response.usage().promptTokens()).isEqualTo(10);
        assertThat(response.usage().completionTokens()).isEqualTo(5);
        assertThat(response.usage().totalTokens()).isEqualTo(15);
    }

    // ─── Streaming chatStream() ───────────────────────────────────────────────

    @Test
    void shouldMapStreamChunksToChatStreamEvents() {
        OpenAiStreamChunk chunk1 = new OpenAiStreamChunk("id1",
                List.of(new OpenAiStreamChunk.StreamChoice(new OpenAiStreamChunk.Delta("Hello"), null)),
                null);
        OpenAiStreamChunk chunk2 = new OpenAiStreamChunk("id2",
                List.of(new OpenAiStreamChunk.StreamChoice(new OpenAiStreamChunk.Delta(" world"), null)),
                null);
        when(mockClient.chatStream(any())).thenReturn(Flux.just(chunk1, chunk2));

        ChatRequest request = new ChatRequest("gpt-4o",
                List.of(new ChatMessage(Role.USER, "Hello")), null, null, true);
        List<ChatStreamEvent> events = provider.chatStream(request).collectList().block();

        assertThat(events).hasSize(2);
        assertThat(events.get(0).delta()).isEqualTo("Hello");
        assertThat(events.get(0).finished()).isFalse();
        assertThat(events.get(1).delta()).isEqualTo(" world");
        assertThat(events.get(1).finished()).isFalse();
    }

    @Test
    void shouldMergeUsageIntoFinishedEvent_whenUsageChunkArrivesAfterStop() {
        OpenAiStreamChunk contentChunk = new OpenAiStreamChunk("id1",
                List.of(new OpenAiStreamChunk.StreamChoice(new OpenAiStreamChunk.Delta("Hello"), null)),
                null);
        OpenAiStreamChunk stopChunk = new OpenAiStreamChunk("id2",
                List.of(new OpenAiStreamChunk.StreamChoice(new OpenAiStreamChunk.Delta(null), "stop")),
                null);
        OpenAiStreamChunk usageChunk = new OpenAiStreamChunk("id3",
                List.of(),
                new OpenAiStreamChunk.Usage(10, 5, 15));
        when(mockClient.chatStream(any())).thenReturn(Flux.just(contentChunk, stopChunk, usageChunk));

        ChatRequest request = new ChatRequest("gpt-4o",
                List.of(new ChatMessage(Role.USER, "Hello")), null, null, true);
        List<ChatStreamEvent> events = provider.chatStream(request).collectList().block();

        assertThat(events).hasSize(2);
        ChatStreamEvent finishedEvent = events.stream().filter(ChatStreamEvent::finished).findFirst().orElseThrow();
        assertThat(finishedEvent.finished()).isTrue();
        assertThat(finishedEvent.usage()).isNotNull();
        assertThat(finishedEvent.usage().promptTokens()).isEqualTo(10);
        assertThat(finishedEvent.usage().completionTokens()).isEqualTo(5);
        assertThat(finishedEvent.usage().totalTokens()).isEqualTo(15);
    }

    @Test
    void shouldEmitFinishedEventWithNullUsage_whenUsageChunkNeverArrives() {
        OpenAiStreamChunk contentChunk = new OpenAiStreamChunk("id1",
                List.of(new OpenAiStreamChunk.StreamChoice(new OpenAiStreamChunk.Delta("Hello"), null)),
                null);
        OpenAiStreamChunk stopChunk = new OpenAiStreamChunk("id2",
                List.of(new OpenAiStreamChunk.StreamChoice(new OpenAiStreamChunk.Delta(null), "stop")),
                null);
        when(mockClient.chatStream(any())).thenReturn(Flux.just(contentChunk, stopChunk));

        ChatRequest request = new ChatRequest("gpt-4o",
                List.of(new ChatMessage(Role.USER, "Hello")), null, null, true);
        List<ChatStreamEvent> events = provider.chatStream(request).collectList().block();

        assertThat(events).hasSize(2);
        ChatStreamEvent finishedEvent = events.stream().filter(ChatStreamEvent::finished).findFirst().orElseThrow();
        assertThat(finishedEvent.finished()).isTrue();
        assertThat(finishedEvent.usage()).isNull();
    }

    @Test
    void shouldSetStreamOptionsIncludeUsage_whenStreamIsTrue() {
        OpenAiStreamChunk contentChunk = new OpenAiStreamChunk("id1",
                List.of(new OpenAiStreamChunk.StreamChoice(new OpenAiStreamChunk.Delta("Hi"), null)),
                null);
        ArgumentCaptor<OpenAiChatRequest> captor = ArgumentCaptor.forClass(OpenAiChatRequest.class);
        when(mockClient.chatStream(captor.capture())).thenReturn(Flux.just(contentChunk));

        ChatRequest request = new ChatRequest("gpt-4o",
                List.of(new ChatMessage(Role.USER, "Hello")), null, null, true);
        provider.chatStream(request).collectList().block();

        OpenAiChatRequest captured = captor.getValue();
        assertThat(captured.stream()).isTrue();
        assertThat(captured.streamOptions()).isEqualTo(Map.of("include_usage", true));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private OpenAiChatResponse fakeOpenAiResponse() {
        return new OpenAiChatResponse(
                "chatcmpl-123",
                "gpt-4o",
                List.of(new OpenAiChatResponse.Choice(new OpenAiChatResponse.Message("assistant", "Hi there!"))),
                new OpenAiChatResponse.Usage(10, 5, 15)
        );
    }
}
