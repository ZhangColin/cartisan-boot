package com.cartisan.openapi.client;

import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.cartisan.openapi.signature.HmacSha256SignatureCalculator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OpenApiClient 对空 body 响应（如 204 No Content）的容忍行为：
 * body 为 null/blank 时跳过反序列化、返回 null，由 body 内容驱动而非状态码驱动。
 */
class OpenApiClientEmptyBodyTest {

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
    void shouldReturnNull_whenPostReturns204WithEmptyBody() {
        // 204 No Content：responseLength = -1 表示无 body
        server.createContext("/api/resource", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();
        String url = "http://localhost:" + port + "/api/resource";

        String result = client.post(url, Map.of("id", 1), new TypeReference<String>() {});

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNull_whenPutReturns204WithEmptyBody() {
        server.createContext("/api/resource", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();
        String url = "http://localhost:" + port + "/api/resource";

        String result = client.put(url, Map.of("id", 1), new TypeReference<String>() {});

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNull_whenGetReturns204WithEmptyBody() {
        server.createContext("/api/resource", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();
        String url = "http://localhost:" + port + "/api/resource";

        String result = client.get(url, new TypeReference<String>() {});

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNull_when200ResponseHasEmptyBody() {
        // 200 + 空 body：证明容忍由 body 内容驱动，而非特判状态码
        server.createContext("/api/resource", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();
        String url = "http://localhost:" + port + "/api/resource";

        String result = client.post(url, Map.of(), new TypeReference<String>() {});

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNull_whenResponseBodyIsWhitespaceOnly() {
        // 仅空白字符的 body 同样视为无 body（用 isBlank 而非 isEmpty 判断）
        server.createContext("/api/resource", exchange -> {
            byte[] response = "   \n\t  ".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();
        String url = "http://localhost:" + port + "/api/resource";

        String result = client.put(url, Map.of(), new TypeReference<String>() {});

        assertThat(result).isNull();
    }

    @Test
    void shouldPreserveWrapping_whenResponseBodyIsMalformed() {
        // 非空但非法的 body 仍走反序列化，Jackson 异常被原样包装为 RuntimeException
        server.createContext("/api/resource", exchange -> {
            byte[] response = "{bad json".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();
        String url = "http://localhost:" + port + "/api/resource";

        assertThatThrownBy(() -> client.post(url, Map.of(), new TypeReference<String>() {}))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OpenApiClient POST failed")
                .hasCauseInstanceOf(JsonProcessingException.class);
    }

    private OpenApiClient createClient() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");
        return new OpenApiClient(props, new HmacSha256SignatureCalculator(), new ObjectMapper());
    }
}
