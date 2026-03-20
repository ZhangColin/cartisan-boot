package com.cartisan.ai.provider.anthropic;

import com.cartisan.ai.model.ChatMessage;
import com.cartisan.ai.model.ChatRequest;
import com.cartisan.ai.model.ChatResponse;
import com.cartisan.ai.model.ChatStreamEvent;
import com.cartisan.ai.model.Role;
import com.cartisan.core.exception.DomainException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class AnthropicProviderTest {

    private AnthropicProvider provider;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        AnthropicProperties properties = new AnthropicProperties();
        properties.setBaseUrl("http://localhost:" + wmRuntimeInfo.getHttpPort());
        properties.setApiKey("test-key");
        properties.setModels(List.of("claude-3-5-sonnet-20241022"));
        properties.setDefaultMaxTokens(4096);
        provider = new AnthropicProvider(properties);
    }

    @Test
    void shouldReturnChatResponse_whenChatSucceeds(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/v1/messages"))
                .withHeader("x-api-key", equalTo("test-key"))
                .withHeader("anthropic-version", equalTo("2023-06-01"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "msg_123",
                                    "type": "message",
                                    "role": "assistant",
                                    "content": [{"type": "text", "text": "Hello!"}],
                                    "model": "claude-3-5-sonnet-20241022",
                                    "stop_reason": "end_turn",
                                    "usage": {"input_tokens": 10, "output_tokens": 5}
                                }
                                """)));

        ChatRequest request = new ChatRequest(
                "claude-3-5-sonnet-20241022",
                List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, false);

        ChatResponse response = provider.chat(request);

        assertThat(response.content()).isEqualTo("Hello!");
        assertThat(response.model()).isEqualTo("claude-3-5-sonnet-20241022");
        assertThat(response.usage().promptTokens()).isEqualTo(10);
        assertThat(response.usage().completionTokens()).isEqualTo(5);
        assertThat(response.usage().totalTokens()).isEqualTo(15);
    }

    @Test
    void shouldStreamResponse_whenStreamIsTrue(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/v1/messages"))
                .withHeader("x-api-key", equalTo("test-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("""
                                event: message_start
                                data: {"type":"message_start","message":{"id":"msg_123","role":"assistant","content":[]}}

                                event: content_block_start
                                data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

                                event: content_block_delta
                                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}

                                event: content_block_delta
                                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":" World"}}

                                event: content_block_stop
                                data: {"type":"content_block_stop","index":0}

                                event: message_delta
                                data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":10}}

                                event: message_stop
                                data: {"type":"message_stop"}
                                """)));

        ChatRequest request = new ChatRequest(
                "claude-3-5-sonnet-20241022",
                List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, true);

        StepVerifier.create(provider.chatStream(request))
                .assertNext(event -> {
                    assertThat(event.delta()).isEqualTo("Hello");
                    assertThat(event.finished()).isFalse();
                })
                .assertNext(event -> {
                    assertThat(event.delta()).isEqualTo(" World");
                    assertThat(event.finished()).isFalse();
                })
                .assertNext(event -> {
                    assertThat(event.delta()).isEmpty();
                    assertThat(event.finished()).isTrue();
                    assertThat(event.usage()).isNotNull();
                    assertThat(event.usage().completionTokens()).isEqualTo(10);
                })
                .verifyComplete();
    }

    @Test
    void shouldUseDefaultMaxTokens_whenRequestMaxTokensIsNull(WireMockRuntimeInfo wmRuntimeInfo) {
        AnthropicProperties properties = new AnthropicProperties();
        properties.setBaseUrl("http://localhost:" + wmRuntimeInfo.getHttpPort());
        properties.setApiKey("test-key");
        properties.setModels(List.of("claude-3-5-sonnet-20241022"));
        properties.setDefaultMaxTokens(8192);
        provider = new AnthropicProvider(properties);

        stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "msg_123",
                                    "type": "message",
                                    "role": "assistant",
                                    "content": [{"type": "text", "text": "OK"}],
                                    "model": "claude-3-5-sonnet-20241022",
                                    "stop_reason": "end_turn",
                                    "usage": {"input_tokens": 10, "output_tokens": 5}
                                }
                                """)));

        ChatRequest request = new ChatRequest(
                "claude-3-5-sonnet-20241022",
                List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, false);

        provider.chat(request);

        verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.max_tokens", equalTo("8192"))));
    }

    @Test
    void shouldThrowException_whenStreamReturnsHttpError(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "type": "error",
                                    "error": {"type": "invalid_request_error", "message": "Invalid request"}
                                }
                                """)));

        ChatRequest request = new ChatRequest(
                "claude-3-5-sonnet-20241022",
                List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, true);

        StepVerifier.create(provider.chatStream(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof DomainException &&
                        throwable.getMessage().contains("Invalid request"))
                .verify();
    }

    @Test
    void shouldHandleMalformedSseEvent_gracefully(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("""
                                event: message_start
                                data: {"type":"message_start"}

                                event: content_block_delta
                                data: {invalid json here}

                                event: message_stop
                                data: {"type":"message_stop"}
                                """)));

        ChatRequest request = new ChatRequest(
                "claude-3-5-sonnet-20241022",
                List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, true);

        StepVerifier.create(provider.chatStream(request))
                .expectErrorMatches(throwable -> throwable instanceof DomainException)
                .verify();
    }
}
