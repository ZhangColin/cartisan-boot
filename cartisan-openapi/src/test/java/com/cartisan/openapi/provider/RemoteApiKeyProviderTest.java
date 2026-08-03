package com.cartisan.openapi.provider;

import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteApiKeyProviderTest {

    private HttpServer server;
    private int port;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> requestedPath = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = startServer(200, """
                {
                    "data": {
                        "apiKey": "myKey",
                        "appName": "TestApp",
                        "apiSecret": "test-secret"
                    }
                }""");
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldReplaceApiKeyPlaceholderInUrl() {
        RemoteApiKeyProvider provider = createProvider(port);

        ApiKeyInfo info = provider.getByApiKey("myKey");

        assertThat(info).isNotNull();
        assertThat(info.apiKey()).isEqualTo("myKey");
        assertThat(info.appName()).isEqualTo("TestApp");
        assertThat(info.apiSecret()).isEqualTo("test-secret");
        assertThat(requestedPath.get()).isEqualTo("/api-keys/myKey");
    }

    @Test
    void shouldIgnoreStatusField_whenRemoteReturnsStatus() throws Exception {
        HttpServer statusServer = startServer(200, """
                {
                    "data": {
                        "apiKey": "myKey",
                        "appName": "TestApp",
                        "apiSecret": "test-secret",
                        "status": "DISABLED"
                    }
                }""");
        try {
            int statusPort = statusServer.getAddress().getPort();
            RemoteApiKeyProvider provider = createProvider(statusPort);

            ApiKeyInfo info = provider.getByApiKey("myKey");

            assertThat(info).isNotNull();
            assertThat(info.apiKey()).isEqualTo("myKey");
            assertThat(info.appName()).isEqualTo("TestApp");
            assertThat(info.apiSecret()).isEqualTo("test-secret");
        } finally {
            statusServer.stop(0);
        }
    }

    @Test
    void shouldReturnNull_whenRemoteReturns404() throws Exception {
        HttpServer errorServer = startServer(404, "{\"error\":\"not found\"}");
        try {
            int errorPort = errorServer.getAddress().getPort();
            RemoteApiKeyProvider provider = createProvider(errorPort);

            ApiKeyInfo info = provider.getByApiKey("nonexistent");

            assertThat(info).isNull();
        } finally {
            errorServer.stop(0);
        }
    }

    @Test
    void shouldReturnNull_whenUrlNotConfigured() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        RemoteApiKeyProvider provider = new RemoteApiKeyProvider(props, objectMapper);

        ApiKeyInfo info = provider.getByApiKey("anyKey");

        assertThat(info).isNull();
    }

    @Test
    void shouldReturnNull_whenUrlIsBlank() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.setApikeyServiceUrl("   ");
        RemoteApiKeyProvider provider = new RemoteApiKeyProvider(props, objectMapper);

        ApiKeyInfo info = provider.getByApiKey("anyKey");

        assertThat(info).isNull();
    }

    // ---- helpers ----

    private RemoteApiKeyProvider createProvider(int port) {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.setApikeyServiceUrl("http://localhost:" + port + "/api-keys/{apiKey}");
        props.getCache().setExpireAfterAccess(Duration.ofSeconds(1));
        props.getCache().setMaximumSize(10);
        return new RemoteApiKeyProvider(props, objectMapper);
    }

    private HttpServer startServer(int statusCode, String body) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress(0), 0);
        s.createContext("/", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        s.start();
        return s;
    }
}
