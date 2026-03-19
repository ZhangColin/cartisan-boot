package com.cartisan.ai.provider.openaicompat;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.DomainException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.HttpProtocol;

import java.io.IOException;
import java.net.http.HttpClient;

public class OpenAiCompatibleClient {

    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final RestClient restClient;
    private final WebClient webClient;

    public OpenAiCompatibleClient(String baseUrl, String apiKey) {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        reactor.netty.http.client.HttpClient.create().protocol(HttpProtocol.HTTP11)))
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public Flux<OpenAiStreamChunk> chatStream(OpenAiChatRequest request) {
        return webClient.post()
                .uri("/chat/completions")
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), res ->
                        res.bodyToMono(OpenAiErrorResponse.class)
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
                .flatMap(chunk -> Flux.fromArray(chunk.split("\n")))
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring(5).trim())
                .filter(data -> !"[DONE]".equals(data))
                .map(data -> {
                    try {
                        return objectMapper.readValue(data, OpenAiStreamChunk.class);
                    } catch (JsonProcessingException e) {
                        throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, "Failed to parse stream chunk: " + e.getMessage());
                    }
                });
    }

    public OpenAiChatResponse chat(OpenAiChatRequest request) {
        return restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    byte[] body = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                    if (body.length == 0) {
                        throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, "Unknown error (no response body)");
                    }
                    try {
                        OpenAiErrorResponse err = objectMapper.readValue(body, OpenAiErrorResponse.class);
                        throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, err.error().message());
                    } catch (IOException e) {
                        throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, e, "Failed to parse error response");
                    }
                })
                .body(OpenAiChatResponse.class);
    }
}
