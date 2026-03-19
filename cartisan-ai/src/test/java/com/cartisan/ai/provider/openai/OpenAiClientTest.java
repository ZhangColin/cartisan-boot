package com.cartisan.ai.provider.openai;

import com.cartisan.core.exception.DomainException;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class OpenAiClientTest {

    private static final String ERROR_BODY_401 = """
            {"error":{"message":"Unauthorized","type":"auth_error","code":"invalid_api_key"}}
            """;
    private static final String ERROR_BODY_429 = """
            {"error":{"message":"Rate limit exceeded","type":"rate_limit_error","code":"rate_limit_exceeded"}}
            """;
    private static final String ERROR_BODY_500 = """
            {"error":{"message":"Internal server error","type":"server_error","code":"server_error"}}
            """;

    private OpenAiChatRequest buildRequest() {
        return new OpenAiChatRequest(
                "gpt-4o",
                List.of(Map.of("role", "user", "content", "Hello")),
                null,
                null,
                false,
                null
        );
    }

    @Test
    void shouldReturnChatResponse_whenOpenAiReturns200(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"chatcmpl-1","model":"gpt-4o","choices":[{"message":{"role":"assistant","content":"Hello!"}}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}
                                """)));

        OpenAiClient client = new OpenAiClient("http://localhost:" + wmRuntimeInfo.getHttpPort(), "test-key");
        OpenAiChatResponse response = client.chat(buildRequest());

        assertThat(response.id()).isEqualTo("chatcmpl-1");
        assertThat(response.model()).isEqualTo("gpt-4o");
        assertThat(response.choices()).hasSize(1);
        assertThat(response.choices().get(0).message().content()).isEqualTo("Hello!");
        assertThat(response.usage().promptTokens()).isEqualTo(10);
        assertThat(response.usage().completionTokens()).isEqualTo(5);
        assertThat(response.usage().totalTokens()).isEqualTo(15);
    }

    @Test
    void shouldThrowDomainException_whenOpenAiReturns401(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ERROR_BODY_401)));

        OpenAiClient client = new OpenAiClient("http://localhost:" + wmRuntimeInfo.getHttpPort(), "test-key");

        assertThatThrownBy(() -> client.chat(buildRequest()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void shouldThrowDomainException_whenOpenAiReturns429(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ERROR_BODY_429)));

        OpenAiClient client = new OpenAiClient("http://localhost:" + wmRuntimeInfo.getHttpPort(), "test-key");

        assertThatThrownBy(() -> client.chat(buildRequest()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Rate limit exceeded");
    }

    @Test
    void shouldThrowDomainException_whenOpenAiReturns500(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ERROR_BODY_500)));

        OpenAiClient client = new OpenAiClient("http://localhost:" + wmRuntimeInfo.getHttpPort(), "test-key");

        assertThatThrownBy(() -> client.chat(buildRequest()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Internal server error");
    }

    @Test
    void shouldStreamChatEvents_whenOpenAiReturnsSSE(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(
                                "data: {\"id\":\"x\",\"choices\":[{\"delta\":{\"content\":\"Hello\"},\"finish_reason\":null}],\"usage\":null}\n" +
                                "data: {\"id\":\"x\",\"choices\":[{\"delta\":{\"content\":\"!\"},\"finish_reason\":\"stop\"}],\"usage\":null}\n" +
                                "data: {\"id\":\"x\",\"choices\":[],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}\n" +
                                "data: [DONE]\n"
                        )));

        OpenAiClient client = new OpenAiClient("http://localhost:" + wmRuntimeInfo.getHttpPort(), "test-key");
        List<OpenAiStreamChunk> chunks = client.chatStream(buildRequest()).collectList().block();

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(2).usage()).isNotNull();
        assertThat(chunks.get(2).usage().promptTokens()).isEqualTo(10);
        assertThat(chunks.get(2).usage().completionTokens()).isEqualTo(5);
        assertThat(chunks.get(2).usage().totalTokens()).isEqualTo(15);
    }

    @Test
    void shouldCompleteFlux_whenDoneSignalReceived(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: [DONE]\n")));

        OpenAiClient client = new OpenAiClient("http://localhost:" + wmRuntimeInfo.getHttpPort(), "test-key");
        List<OpenAiStreamChunk> chunks = client.chatStream(buildRequest()).collectList().block();

        assertThat(chunks).isEmpty();
    }

    @Test
    void shouldThrowDomainException_whenStreamErrorOccurs(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ERROR_BODY_401)));

        OpenAiClient client = new OpenAiClient("http://localhost:" + wmRuntimeInfo.getHttpPort(), "test-key");

        assertThatThrownBy(() -> client.chatStream(buildRequest()).collectList().block())
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Unauthorized");
    }
}
