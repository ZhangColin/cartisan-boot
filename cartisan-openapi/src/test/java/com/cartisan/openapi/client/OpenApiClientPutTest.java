package com.cartisan.openapi.client;

import com.cartisan.core.context.RequestContext;
import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.cartisan.openapi.signature.HmacSha256SignatureCalculator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenApiClientPutTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldSendPutRequest() throws IOException {
        AtomicReference<String> receivedMethod = new AtomicReference<>();
        AtomicReference<String> receivedBody = new AtomicReference<>();

        server.createContext("/api/resource", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "\"ok\"".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");

        OpenApiClient client = new OpenApiClient(props,
                new HmacSha256SignatureCalculator(), new ObjectMapper());

        String url = "http://localhost:" + port + "/api/resource";
        Map<String, String> body = Map.of("name", "test");

        String result = client.put(url, body, new TypeReference<String>() {});

        assertThat(receivedMethod.get()).isEqualTo("PUT");
        assertThat(receivedBody.get()).isEqualTo("{\"name\":\"test\"}");
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void shouldThrowException_whenServerReturns4xx() throws IOException {
        server.createContext("/api/resource", exchange -> {
            byte[] response = "{\"error\":\"bad request\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");

        OpenApiClient client = new OpenApiClient(props,
                new HmacSha256SignatureCalculator(), new ObjectMapper());

        String url = "http://localhost:" + port + "/api/resource";

        assertThatThrownBy(() -> client.put(url, Map.of(), new TypeReference<String>() {}))
                .isInstanceOf(OpenApiClientException.class)
                .satisfies(ex -> {
                    OpenApiClientException oce = (OpenApiClientException) ex;
                    assertThat(oce.getStatusCode()).isEqualTo(400);
                    assertThat(oce.getBody()).isEqualTo("{\"error\":\"bad request\"}");
                });
    }

    @Test
    void shouldHandleNullBody() throws IOException {
        AtomicReference<String> receivedMethod = new AtomicReference<>();
        AtomicReference<String> receivedBody = new AtomicReference<>();

        server.createContext("/api/resource", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "\"ok\"".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");

        OpenApiClient client = new OpenApiClient(props,
                new HmacSha256SignatureCalculator(), new ObjectMapper());

        String url = "http://localhost:" + port + "/api/resource";

        String result = client.put(url, null, new TypeReference<String>() {});

        assertThat(receivedMethod.get()).isEqualTo("PUT");
        assertThat(receivedBody.get()).isEmpty();
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void shouldIncludeSignatureAndContextHeadersInPutRequest() throws IOException {
        AtomicReference<Headers> receivedHeaders = new AtomicReference<>();

        server.createContext("/api/resource", exchange -> {
            receivedHeaders.set(exchange.getRequestHeaders());
            byte[] response = "\"ok\"".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");

        OpenApiClient client = new OpenApiClient(props,
                new HmacSha256SignatureCalculator(), new ObjectMapper());

        RequestContext ctx = new RequestContext(
                "req-1", "10.0.0.1",
                null, null,
                42L, "Alice",
                100L, "TenantX");

        String url = "http://localhost:" + port + "/api/resource";

        RequestContext.run(ctx, () -> {
            client.put(url, Map.of("key", "value"), new TypeReference<String>() {});
        });

        Headers headers = receivedHeaders.get();
        assertThat(headers.getFirst("X-Api-key")).isEqualTo("test-app");
        assertThat(headers.getFirst("X-Timestamp")).isNotNull();
        assertThat(headers.getFirst("X-Nonce")).isNotNull();
        assertThat(headers.getFirst("X-Body-digest")).isNotNull();
        assertThat(headers.getFirst("X-Sign")).isNotNull();
        assertThat(headers.getFirst("X-Request-id")).isEqualTo("req-1");
        assertThat(headers.getFirst("X-User-id")).isEqualTo("42");
        assertThat(headers.getFirst("X-Tenant-id")).isEqualTo("100");
    }
}
