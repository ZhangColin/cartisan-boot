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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link OpenApiClient#delete(String, TypeReference)} HTTP DELETE 出口测试（#33）。
 *
 * <p>透传型删除端点的回执是 {@code ApiResponse<T>} JSON 信封（data＝删除前终态），
 * 本组测试钉死：DELETE 动词真正发出、信封按 {@code TypeReference} 反序列化、
 * 五头签名 + RequestContext 透传头在场、≥400 复用 {@code OpenApiClientException}
 * （admin AiplatformUpstreamException 翻译路径零改动）。</p>
 */
class OpenApiClientDeleteTest {

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

    /** aiplatform 删除端点回执信封形状（code/message/data，data＝删除前终态 DTO）。 */
    private record ApiResponse<T>(int code, String message, T data) {}

    private record MaterialSummary(long id, String title, long knowledgeCount) {}

    private static final String DELETE_RECEIPT = """
            {"code":0,"message":"ok","data":{"id":42,"title":"素材标题","knowledgeCount":3}}""";

    @Test
    void shouldSendDeleteMethodAndReturnDeserializedEnvelope() {
        AtomicReference<String> requestMethod = new AtomicReference<>();

        server.createContext("/api/backoffice/materials/42", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            byte[] response = DELETE_RECEIPT.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();

        ApiResponse<MaterialSummary> receipt = client.delete(
                "http://localhost:" + port + "/api/backoffice/materials/42",
                new TypeReference<ApiResponse<MaterialSummary>>() {});

        // DELETE 动词真正发出（不是 GET/POST 变通）
        assertThat(requestMethod.get()).isEqualTo("DELETE");
        assertThat(receipt.code()).isZero();
        assertThat(receipt.message()).isEqualTo("ok");
        // 删除前终态 DTO 按 TypeReference 完整反序列化，调用方从信封取 data
        assertThat(receipt.data()).isEqualTo(new MaterialSummary(42L, "素材标题", 3L));
    }

    @Test
    void shouldIncludeSignatureAndContextHeadersInDeleteRequest() {
        AtomicReference<Headers> receivedHeaders = new AtomicReference<>();

        server.createContext("/api/backoffice/materials/42", exchange -> {
            receivedHeaders.set(exchange.getRequestHeaders());
            byte[] response = DELETE_RECEIPT.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();

        RequestContext ctx = new RequestContext(
                "req-1", "10.0.0.1",
                null, null,
                42L, "Alice",
                100L, "TenantX");

        RequestContext.run(ctx, () ->
                client.delete("http://localhost:" + port + "/api/backoffice/materials/42",
                        new TypeReference<ApiResponse<MaterialSummary>>() {}));

        Headers headers = receivedHeaders.get();
        // DELETE 路径必须复用既有五头签名（空 body digest），不得新造签名机制
        assertThat(headers.getFirst("X-Api-Key")).isEqualTo("test-app");
        assertThat(headers.getFirst("X-Timestamp")).isNotNull();
        assertThat(headers.getFirst("X-Nonce")).isNotNull();
        assertThat(headers.getFirst("X-Body-Digest")).isNotNull();
        assertThat(headers.getFirst("X-Sign")).isNotNull();
        // RequestContext 透传头同 get() 语义
        assertThat(headers.getFirst("X-Request-Id")).isEqualTo("req-1");
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("42");
        assertThat(headers.getFirst("X-User-Name")).isEqualTo("Alice");
        assertThat(headers.getFirst("X-Tenant-Id")).isEqualTo("100");
    }

    @Test
    void shouldThrowOpenApiClientException_whenDeleteServerReturnsError() {
        // provider 错误信封是 JSON 文本（如 404 KNW_005），进异常 body 供上游翻译
        server.createContext("/api/backoffice/materials/42", exchange -> {
            byte[] response = "{\"code\":\"KNW_005\",\"message\":\"material not found\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();

        assertThatThrownBy(() -> client.delete(
                "http://localhost:" + port + "/api/backoffice/materials/42",
                new TypeReference<ApiResponse<MaterialSummary>>() {}))
                .isInstanceOf(OpenApiClientException.class)
                .satisfies(ex -> {
                    OpenApiClientException oce = (OpenApiClientException) ex;
                    assertThat(oce.getStatusCode()).isEqualTo(404);
                    assertThat(oce.getBody())
                            .isEqualTo("{\"code\":\"KNW_005\",\"message\":\"material not found\"}");
                });
    }

    @Test
    void shouldReturnNull_whenDeleteReturns204WithEmptyBody() {
        // 204 No Content：删除端点无回执时容忍空 body，跳过反序列化
        server.createContext("/api/backoffice/materials/42", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();

        ApiResponse<MaterialSummary> receipt = client.delete(
                "http://localhost:" + port + "/api/backoffice/materials/42",
                new TypeReference<ApiResponse<MaterialSummary>>() {});

        assertThat(receipt).isNull();
    }

    @Test
    void shouldWrapReadTimeout_asRuntimeExceptionWithUrl() throws Exception {
        // 慢响应服务器：3s 才回——远超配置的 1s 读超时（TimeoutTest 模式）
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(3_000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        });
        server.start();

        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");
        props.getTimeout().setReadSeconds(1L);
        OpenApiClient client = new OpenApiClient(props,
                new HmacSha256SignatureCalculator(), new ObjectMapper());

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> client.delete(
                "http://localhost:" + port + "/api/backoffice/materials/42",
                new TypeReference<ApiResponse<MaterialSummary>>() {}))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(OpenApiClientException.class)
                .hasMessageContaining("OpenApiClient DELETE failed");
        // 1s 读超时生效，远不到默认 30s（留 3s 上限容机器抖动）
        assertThat(System.currentTimeMillis() - start).isLessThan(3_000L);
    }

    @Test
    void shouldWrapConnectionRefused_asRuntimeExceptionWithUrl() throws IOException {
        // 端口确定无监听：起一个 server 拿端口后立即关闭 → ConnectException → RuntimeException 包装
        HttpServer throwaway = HttpServer.create(new InetSocketAddress(0), 0);
        int closedPort = throwaway.getAddress().getPort();
        throwaway.stop(0);

        OpenApiClient client = createClient();

        assertThatThrownBy(() -> client.delete(
                "http://localhost:" + closedPort + "/api/backoffice/materials/42",
                new TypeReference<ApiResponse<MaterialSummary>>() {}))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(OpenApiClientException.class)
                .hasMessageContaining("OpenApiClient DELETE failed");
    }

    private OpenApiClient createClient() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");
        return new OpenApiClient(props, new HmacSha256SignatureCalculator(), new ObjectMapper());
    }
}
