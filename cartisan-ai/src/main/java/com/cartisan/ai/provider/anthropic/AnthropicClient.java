package com.cartisan.ai.provider.anthropic;

import com.cartisan.ai.provider.anthropic.dto.AnthropicChatRequest;
import com.cartisan.ai.provider.anthropic.dto.AnthropicChatResponse;
import com.cartisan.ai.provider.anthropic.dto.AnthropicErrorResponse;
import com.cartisan.ai.provider.anthropic.dto.AnthropicStreamChunk;
import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.DomainException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.HttpProtocol;

/**
 * Anthropic API HTTP 客户端。
 * <p>封装同步调用（RestClient）和流式调用（WebClient）。
 */
public class AnthropicClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final RestClient restClient;
    private final WebClient webClient;

    public AnthropicClient(String baseUrl, String apiKey) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        reactor.netty.http.client.HttpClient.create().protocol(HttpProtocol.HTTP11)))
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 同步调用 /v1/messages。
     */
    public AnthropicChatResponse chat(AnthropicChatRequest request) {
        return restClient.post()
                .uri("/v1/messages")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    byte[] body = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                    if (body.length == 0) {
                        throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, "Unknown error (no response body)");
                    }
                    try {
                        AnthropicErrorResponse err = objectMapper.readValue(body, AnthropicErrorResponse.class);
                        throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, err.error().message());
                    } catch (IOException e) {
                        throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, e, "Failed to parse error response");
                    }
                })
                .body(AnthropicChatResponse.class);
    }

    /**
     * 流式调用 /v1/messages（SSE）。
     * <p>解析 Anthropic 特有的事件格式：event: xxx\ndata: {...}
     */
    public Flux<AnthropicStreamChunk> chatStream(AnthropicChatRequest request) {
        return webClient.post()
                .uri("/v1/messages")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), res ->
                        res.bodyToMono(AnthropicErrorResponse.class)
                                .map(err -> new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, err.error().message()))
                                .switchIfEmpty(Mono.just(new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, "Unknown error (no response body)"))))
                // Use DataBuffer instead of String to bypass Spring's ServerSentEventHttpMessageReader,
                // which activates on text/event-stream responses and returns empty stream.
                // Each DataBuffer is processed individually to preserve true streaming (no join/buffering).
                .bodyToFlux(DataBuffer.class)
                .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
                .map(buf -> {
                    byte[] bytes = new byte[buf.readableByteCount()];
                    buf.read(bytes);
                    DataBufferUtils.release(buf);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .flatMap(chunk -> Flux.fromArray(chunk.split("\n\n")))
                .filter(event -> !event.isBlank())
                .map(event -> parseSseEvent(event));
    }

    /**
     * 解析单个 SSE 事件。
     * <p>格式：event: message_start\ndata: {...}
     */
    private AnthropicStreamChunk parseSseEvent(String event) {
        String[] lines = event.split("\n", 2);
        String eventType = lines.length > 0 && lines[0].startsWith("event: ")
                ? lines[0].substring(7)
                : "";
        String dataLine = lines.length > 1 && lines[1].startsWith("data: ")
                ? lines[1].substring(6)
                : "{}";

        try {
            JsonNode dataNode = objectMapper.readValue(dataLine, JsonNode.class);
            // 如果事件中没有 type 字段，从 event 行提取
            if (dataNode.has("type")) {
                eventType = dataNode.get("type").asText();
            }
            return new AnthropicStreamChunk(eventType, dataNode);
        } catch (JsonProcessingException e) {
            // 解析失败时返回错误事件
            return new AnthropicStreamChunk("error",
                    objectMapper.createObjectNode().put("error", "Failed to parse event: " + e.getMessage()));
        }
    }
}
