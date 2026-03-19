package com.cartisan.ai.provider.openai;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.DomainException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.HttpProtocol;

import java.io.IOException;
import java.net.http.HttpClient;

public class OpenAiClient {

    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final RestClient restClient;
    private final WebClient webClient;

    public OpenAiClient(String baseUrl, String apiKey) {

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
                        throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, "Failed to parse error response");
                    }
                })
                .body(OpenAiChatResponse.class);
    }
}
