package com.cartisan.openapi.client;

import com.cartisan.core.context.RequestContext;
import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.cartisan.openapi.signature.HmacSha256SignatureCalculator;
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
 * {@link OpenApiClient#download(String)} 二进制下载测试（#30）。
 *
 * <p>回归锚点：JSON 路径的 {@code BodyHandlers.ofString()} 对非 UTF-8 字节做 REPLACE
 * （→ U+FFFD，不可逆损坏），本组测试钉死 binary 路径字节逐位原样到达调用方。</p>
 */
class OpenApiClientDownloadTest {

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

    /**
     * 载荷刻意含多个非法 UTF-8 序列（gzip magic、invalid/overlong/surrogate 编码）——
     * ofString 路径会把这些字节替换为 U+FFFD，任何基于字符串的断言都无法逐位还原。
     */
    private static final byte[] GZIP_LIKE_PAYLOAD = {
            (byte) 0x1F, (byte) 0x8B, (byte) 0x08, 0x00,          // gzip magic + deflate
            (byte) 0xC3, 0x28,                                    // invalid UTF-8
            (byte) 0xE0, (byte) 0x80, (byte) 0x80,                // overlong encoding
            (byte) 0xFF, (byte) 0xFE, (byte) 0xFD,                // never valid in UTF-8
            (byte) 0xED, (byte) 0xA0, (byte) 0x80                 // encoded surrogate
    };

    private static final String CONTENT_DISPOSITION =
            "attachment; filename=\"order-1-source.tar.gz\"";

    @Test
    void shouldDownloadRawBytesWithResponseHeaders() {
        server.createContext("/api/orders/1/source-package", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/gzip");
            exchange.getResponseHeaders().set("Content-Disposition", CONTENT_DISPOSITION);
            exchange.sendResponseHeaders(200, GZIP_LIKE_PAYLOAD.length);
            exchange.getResponseBody().write(GZIP_LIKE_PAYLOAD);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();

        BinaryResponse response = client.download("http://localhost:" + port + "/api/orders/1/source-package");

        assertThat(response.statusCode()).isEqualTo(200);
        // 字节完整性：逐位相等（ofString 损坏的回归锚点）
        assertThat(response.body()).isEqualTo(GZIP_LIKE_PAYLOAD);
        // BFF 透传依赖的两个响应头可取
        assertThat(response.headers().firstValue("Content-Type")).contains("application/gzip");
        assertThat(response.headers().firstValue("Content-Disposition")).contains(CONTENT_DISPOSITION);
        // 大小写不敏感是透传场景刚需（header 名大小写由 provider 决定）
        assertThat(response.headers().firstValue("content-disposition")).contains(CONTENT_DISPOSITION);
    }

    @Test
    void shouldIncludeSignatureAndContextHeadersInDownloadRequest() {
        AtomicReference<Headers> receivedHeaders = new AtomicReference<>();

        server.createContext("/api/orders/1/source-package", exchange -> {
            receivedHeaders.set(exchange.getRequestHeaders());
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        });
        server.start();

        OpenApiClient client = createClient();

        RequestContext ctx = new RequestContext(
                "req-1", "10.0.0.1",
                null, null,
                42L, "Alice",
                100L, "TenantX");

        RequestContext.run(ctx, () ->
                client.download("http://localhost:" + port + "/api/orders/1/source-package"));

        Headers headers = receivedHeaders.get();
        // binary 路径必须复用既有五头签名（GET 空 body digest），不得新造签名机制
        assertThat(headers.getFirst("X-Api-Key")).isEqualTo("test-app");
        assertThat(headers.getFirst("X-Timestamp")).isNotNull();
        assertThat(headers.getFirst("X-Nonce")).isNotNull();
        assertThat(headers.getFirst("X-Body-Digest")).isNotNull();
        assertThat(headers.getFirst("X-Sign")).isNotNull();
        // RequestContext 透传头同 get() 语义
        assertThat(headers.getFirst("X-Request-Id")).isEqualTo("req-1");
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("42");
        assertThat(headers.getFirst("X-Tenant-Id")).isEqualTo("100");
    }

    @Test
    void shouldThrowOpenApiClientException_whenDownloadServerReturnsError() {
        // provider 错误信封是 JSON 文本——即使下载端点，错误体仍按文本进异常
        server.createContext("/api/orders/1/source-package", exchange -> {
            byte[] response = "{\"code\":500,\"message\":\"package not found\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        OpenApiClient client = createClient();

        assertThatThrownBy(() -> client.download("http://localhost:" + port + "/api/orders/1/source-package"))
                .isInstanceOf(OpenApiClientException.class)
                .satisfies(ex -> {
                    OpenApiClientException oce = (OpenApiClientException) ex;
                    assertThat(oce.getStatusCode()).isEqualTo(500);
                    assertThat(oce.getBody()).isEqualTo("{\"code\":500,\"message\":\"package not found\"}");
                });
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
        assertThatThrownBy(() -> client.download("http://localhost:" + port + "/api/orders/1/source-package"))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(OpenApiClientException.class)
                .hasMessageContaining("OpenApiClient download failed");
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

        assertThatThrownBy(() -> client.download("http://localhost:" + closedPort + "/api/orders/1/source-package"))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(OpenApiClientException.class)
                .hasMessageContaining("OpenApiClient download failed");
    }

    private OpenApiClient createClient() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");
        return new OpenApiClient(props, new HmacSha256SignatureCalculator(), new ObjectMapper());
    }
}
