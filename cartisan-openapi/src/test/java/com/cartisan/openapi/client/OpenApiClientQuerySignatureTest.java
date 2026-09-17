package com.cartisan.openapi.client;

import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.cartisan.openapi.filter.SignatureVerificationFilter;
import com.cartisan.openapi.nonce.InMemoryNonceRepository;
import com.cartisan.openapi.provider.ApiKeyInfo;
import com.cartisan.openapi.signature.HmacSha256SignatureCalculator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OpenApiClient} 签名 query 口径与服务端验签的一致性测试（#35）。
 *
 * <p>钉死「客户端入签串 = 服务端验签串」这一 wire 契约：服务端
 * {@link SignatureVerificationFilter} 以 {@code request.getQueryString()} 的
 * <strong>raw 形态</strong>入签，客户端必须以 {@link java.net.URI#getRawQuery()}
 * 同形入签——query 值含需 percent-encode 的字符（{@code :}、{@code /}、中文、空格）
 * 时两形态分叉，decoded 形态（{@code getQuery()}）入签必 401 Signature mismatch。</p>
 *
 * <p>Seam = wire：client 发真实 HTTP 到本地 {@code HttpServer}，捕获 raw query +
 * 五签名头 + body，回放进真实 {@link SignatureVerificationFilter}（真实 HMAC 计算器、
 * {@link InMemoryNonceRepository}、真实 {@link ApiKeyInfo}）——验签通过的判据是
 * FilterChain 被调用（响应非 401）。</p>
 */
class OpenApiClientQuerySignatureTest {

    private static final String APP_KEY = "test-app";
    private static final String APP_SECRET = "test-secret";

    private HttpServer server;
    private int port;

    private final AtomicReference<String> capturedRawQuery = new AtomicReference<>();
    private final AtomicReference<Headers> capturedHeaders = new AtomicReference<>();
    private final AtomicReference<byte[]> capturedBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        // 绑定与 URL 双端钉死 IPv4 loopback——localhost 会同时解析 ::1/127.0.0.1，
        // 与 wildcard bind 组合在本机出现间歇 RST/卡顿（#35 评审实测）
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldPassServerSignatureVerification_whenGetQueryValueContainsColon() throws Exception {
        startCapturingServer("/api/backoffice/costs/overview");
        OpenApiClient client = createClient();

        // issue #35 实案：T5 成本中心 ISO 时间窗含 ":"，BFF 按 URLEncoder 编码拼 URL
        client.get("http://127.0.0.1:" + port + "/api/backoffice/costs/overview"
                        + "?from=" + URLEncoder.encode("2026-09-01T00:00:00Z", StandardCharsets.UTF_8)
                        + "&to=" + URLEncoder.encode("2026-09-17T00:00:00Z", StandardCharsets.UTF_8),
                new TypeReference<Map<String, Object>>() {});

        assertServerSignatureVerificationPasses();
    }

    @Test
    void shouldPassServerSignatureVerification_whenGetQueryValueContainsChinese() throws Exception {
        startCapturingServer("/api/backoffice/materials/search");
        OpenApiClient client = createClient();

        // 素材搜索 keyword 中文（issue #35 影响面"潜在"案）
        client.get("http://127.0.0.1:" + port + "/api/backoffice/materials/search"
                        + "?keyword=" + URLEncoder.encode("中文", StandardCharsets.UTF_8),
                new TypeReference<Map<String, Object>>() {});

        assertServerSignatureVerificationPasses();
    }

    @Test
    void shouldPassServerSignatureVerification_whenGetQueryValueContainsPercent20Space() throws Exception {
        startCapturingServer("/api/backoffice/materials/search");
        OpenApiClient client = createClient();

        // 空格的 %20 形态（部分编码器直出 %20 而非 URLEncoder 的 +）
        client.get("http://127.0.0.1:" + port + "/api/backoffice/materials/search"
                        + "?keyword=a%20b",
                new TypeReference<Map<String, Object>>() {});

        assertServerSignatureVerificationPasses();
    }

    /**
     * 空格的 {@code +} 形态（URLEncoder form 编码实出）。{@code URI.getQuery()} 不把
     * {@code +} 解码为空格，此案在修复前后均一致——作为口径锚点保留，防未来"顺手解码 +"。
     */
    @Test
    void shouldPassServerSignatureVerification_whenGetQueryValueContainsPlusSpace() throws Exception {
        startCapturingServer("/api/backoffice/materials/search");
        OpenApiClient client = createClient();

        client.get("http://127.0.0.1:" + port + "/api/backoffice/materials/search"
                        + "?keyword=" + URLEncoder.encode("a b", StandardCharsets.UTF_8),
                new TypeReference<Map<String, Object>>() {});

        assertServerSignatureVerificationPasses();
    }

    @Test
    void shouldPassServerSignatureVerification_whenDownloadQueryValueContainsSlash() throws Exception {
        startCapturingServer("/api/backoffice/projects/1/files/content");
        OpenApiClient client = createClient();

        // issue #35 实案：T3 交付文件树嵌套路径含 "/"
        client.download("http://127.0.0.1:" + port + "/api/backoffice/projects/1/files/content"
                + "?path=" + URLEncoder.encode("src/main.ts", StandardCharsets.UTF_8));

        assertServerSignatureVerificationPasses();
    }

    @Test
    void shouldPassServerSignatureVerification_whenDeleteQueryValueContainsSlash() throws Exception {
        startCapturingServer("/api/backoffice/projects/1/files");
        OpenApiClient client = createClient();

        client.delete("http://127.0.0.1:" + port + "/api/backoffice/projects/1/files"
                        + "?path=" + URLEncoder.encode("src/main.ts", StandardCharsets.UTF_8),
                new TypeReference<Map<String, Object>>() {});

        assertServerSignatureVerificationPasses();
    }

    /** 捕获 server：记录 wire 上的 raw query、请求头、body，回 200 JSON 信封。 */
    private void startCapturingServer(String path) {
        server.createContext(path, exchange -> {
            capturedRawQuery.set(exchange.getRequestURI().getRawQuery());
            capturedHeaders.set(exchange.getRequestHeaders());
            capturedBody.set(exchange.getRequestBody().readAllBytes());
            byte[] response = "{\"code\":0}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    /**
     * 把捕获的 wire 请求回放进真实 {@link SignatureVerificationFilter}，
     * 断言验签通过（FilterChain 被调用、响应非 401）。
     */
    private void assertServerSignatureVerificationPasses() throws Exception {
        Headers headers = capturedHeaders.get();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString(capturedRawQuery.get());
        request.addHeader("X-Api-Key", headers.getFirst("X-Api-Key"));
        request.addHeader("X-Timestamp", headers.getFirst("X-Timestamp"));
        request.addHeader("X-Nonce", headers.getFirst("X-Nonce"));
        request.addHeader("X-Body-Digest", headers.getFirst("X-Body-Digest"));
        request.addHeader("X-Sign", headers.getFirst("X-Sign"));
        byte[] body = capturedBody.get();
        if (body != null) {
            request.setContent(body);
        }

        SignatureVerificationFilter filter = new SignatureVerificationFilter(
                new HmacSha256SignatureCalculator(),
                apiKey -> new ApiKeyInfo(APP_KEY, "Test App", APP_SECRET),
                new InMemoryNonceRepository(),
                new CartisanOpenapiProperties(),   // 默认 timestampTolerance/nonceTtl=300s
                new ObjectMapper());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        // 验签通过才会进入 FilterChain（401 时 filter 直接写错误响应、短路 chain）
        assertThat(chain.getRequest()).isNotNull();
    }

    private OpenApiClient createClient() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey(APP_KEY);
        props.getSelf().setApiSecret(APP_SECRET);
        return new OpenApiClient(props, new HmacSha256SignatureCalculator(), new ObjectMapper());
    }
}
