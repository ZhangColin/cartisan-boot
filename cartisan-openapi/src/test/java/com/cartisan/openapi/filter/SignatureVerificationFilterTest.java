package com.cartisan.openapi.filter;

import com.cartisan.core.context.RequestContext;
import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.cartisan.openapi.nonce.NonceRepository;
import com.cartisan.openapi.provider.ApiKeyInfo;
import com.cartisan.openapi.provider.ApiKeyProvider;
import com.cartisan.openapi.signature.SignatureCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignatureVerificationFilterTest {

    @Mock
    private SignatureCalculator signatureCalculator;
    @Mock
    private ApiKeyProvider apiKeyProvider;
    @Mock
    private NonceRepository nonceRepository;
    @Mock
    private CartisanOpenapiProperties properties;
    @Mock
    private ObjectMapper objectMapper;

    private SignatureVerificationFilter filter;

    private static final String APP_ID = "test-app";
    private static final String APP_SECRET = "test-secret";
    private static final String APP_NAME = "Test App";
    private static final ApiKeyInfo API_KEY_INFO = new ApiKeyInfo(
            APP_ID, APP_NAME, APP_SECRET, Set.of("read", "write"), "ACTIVE");

    @BeforeEach
    void setUp() {
        filter = new SignatureVerificationFilter(
                signatureCalculator, apiKeyProvider, nonceRepository, properties, objectMapper);
    }

    @Test
    void shouldPassThrough_whenNoAppIdHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        RequestContext baseCtx = new RequestContext("req-1", "127.0.0.1", null, null, null, null, null, null);
        runInContext(baseCtx, () -> filter.doFilterInternal(request, response, filterChain));

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldReject_whenTimestampExpired() throws Exception {
        // Use a timestamp far in the past (1 day ago = 86400 seconds)
        long expiredTimestamp = (System.currentTimeMillis() / 1000) - 86400;
        MockHttpServletRequest request = createSignedRequest(
                String.valueOf(expiredTimestamp), "nonce-1", "digest", "sign");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(properties.getTimestampTolerance()).thenReturn(300L);

        RequestContext baseCtx = new RequestContext("req-1", "127.0.0.1", null, null, null, null, null, null);
        runInContext(baseCtx, () -> filter.doFilterInternal(request, response, filterChain));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void shouldReject_whenNonceDuplicated() throws Exception {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        MockHttpServletRequest request = createSignedRequest(
                String.valueOf(currentTimestamp), "dup-nonce", "digest", "sign");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(properties.getTimestampTolerance()).thenReturn(300L);
        when(properties.getNonceTtl()).thenReturn(300L);
        when(nonceRepository.tryAcquire(eq("dup-nonce"), any(Duration.class))).thenReturn(false);

        RequestContext baseCtx = new RequestContext("req-1", "127.0.0.1", null, null, null, null, null, null);
        runInContext(baseCtx, () -> filter.doFilterInternal(request, response, filterChain));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void shouldReject_whenInvalidAppId() throws Exception {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        MockHttpServletRequest request = createSignedRequest(
                String.valueOf(currentTimestamp), "nonce-ok", "digest", "sign");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(properties.getTimestampTolerance()).thenReturn(300L);
        when(properties.getNonceTtl()).thenReturn(300L);
        when(nonceRepository.tryAcquire(eq("nonce-ok"), any(Duration.class))).thenReturn(true);
        when(apiKeyProvider.getByAppId(APP_ID)).thenReturn(null);

        RequestContext baseCtx = new RequestContext("req-1", "127.0.0.1", null, null, null, null, null, null);
        runInContext(baseCtx, () -> filter.doFilterInternal(request, response, filterChain));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void shouldReject_whenBodyDigestMismatch() throws Exception {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        byte[] body = "{\"data\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        String wrongDigest = "0000000000000000";

        MockHttpServletRequest request = createSignedRequestWithBody(
                String.valueOf(currentTimestamp), "nonce-ok", wrongDigest, "sign", body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(properties.getTimestampTolerance()).thenReturn(300L);
        when(properties.getNonceTtl()).thenReturn(300L);
        when(nonceRepository.tryAcquire(eq("nonce-ok"), any(Duration.class))).thenReturn(true);
        when(apiKeyProvider.getByAppId(APP_ID)).thenReturn(API_KEY_INFO);

        RequestContext baseCtx = new RequestContext("req-1", "127.0.0.1", null, null, null, null, null, null);
        runInContext(baseCtx, () -> filter.doFilterInternal(request, response, filterChain));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void shouldReject_whenSignatureMismatch() throws Exception {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        byte[] body = "{\"data\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        String bodyDigest = sha256Hex(body);

        MockHttpServletRequest request = createSignedRequestWithBody(
                String.valueOf(currentTimestamp), "nonce-ok", bodyDigest, "wrong-sign", body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(properties.getTimestampTolerance()).thenReturn(300L);
        when(properties.getNonceTtl()).thenReturn(300L);
        when(nonceRepository.tryAcquire(eq("nonce-ok"), any(Duration.class))).thenReturn(true);
        when(apiKeyProvider.getByAppId(APP_ID)).thenReturn(API_KEY_INFO);
        when(signatureCalculator.calculate(any(String.class), eq(APP_SECRET))).thenReturn("correct-sign");

        RequestContext baseCtx = new RequestContext("req-1", "127.0.0.1", null, null, null, null, null, null);
        runInContext(baseCtx, () -> filter.doFilterInternal(request, response, filterChain));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void shouldEnrichRequestContext_whenSignatureValid() throws Exception {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        byte[] body = "{\"data\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        String bodyDigest = sha256Hex(body);
        String correctSign = "expected-sign";

        MockHttpServletRequest request = createSignedRequestWithBody(
                String.valueOf(currentTimestamp), "nonce-ok", bodyDigest, correctSign, body);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Capture RequestContext inside filter chain
        RequestContext[] capturedCtx = new RequestContext[1];
        FilterChain filterChain = capturingFilterChain((servletRequest, servletResponse) -> {
            capturedCtx[0] = RequestContext.CONTEXT.orElse(null);
        });

        when(properties.getTimestampTolerance()).thenReturn(300L);
        when(properties.getNonceTtl()).thenReturn(300L);
        when(nonceRepository.tryAcquire(eq("nonce-ok"), any(Duration.class))).thenReturn(true);
        when(apiKeyProvider.getByAppId(APP_ID)).thenReturn(API_KEY_INFO);
        when(signatureCalculator.calculate(any(String.class), eq(APP_SECRET))).thenReturn(correctSign);

        RequestContext baseCtx = new RequestContext("req-1", "127.0.0.1", null, null, null, null, null, null);
        runInContext(baseCtx, () -> filter.doFilterInternal(request, response, filterChain));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(capturedCtx[0]).isNotNull();
        assertThat(capturedCtx[0].callerAppId()).isEqualTo(APP_ID);
        assertThat(capturedCtx[0].callerAppName()).isEqualTo(APP_NAME);
    }

    @Test
    void shouldStoreApiKeyInfoInRequestAttribute_whenSignatureValid() throws Exception {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        byte[] body = "{\"data\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        String bodyDigest = sha256Hex(body);
        String correctSign = "expected-sign";

        MockHttpServletRequest request = createSignedRequestWithBody(
                String.valueOf(currentTimestamp), "nonce-ok", bodyDigest, correctSign, body);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object[] capturedAttribute = new Object[1];
        FilterChain filterChain = capturingFilterChain((servletRequest, servletResponse) -> {
            capturedAttribute[0] = servletRequest.getAttribute(SignatureVerificationFilter.API_KEY_INFO_ATTR);
        });

        when(properties.getTimestampTolerance()).thenReturn(300L);
        when(properties.getNonceTtl()).thenReturn(300L);
        when(nonceRepository.tryAcquire(eq("nonce-ok"), any(Duration.class))).thenReturn(true);
        when(apiKeyProvider.getByAppId(APP_ID)).thenReturn(API_KEY_INFO);
        when(signatureCalculator.calculate(any(String.class), eq(APP_SECRET))).thenReturn(correctSign);

        RequestContext baseCtx = new RequestContext("req-1", "127.0.0.1", null, null, null, null, null, null);
        runInContext(baseCtx, () -> filter.doFilterInternal(request, response, filterChain));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(capturedAttribute[0]).isSameAs(API_KEY_INFO);
    }

    @Test
    void shouldNotEnrichContext_whenSignatureInvalid() throws Exception {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        byte[] body = "{\"data\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        String bodyDigest = sha256Hex(body);

        MockHttpServletRequest request = createSignedRequestWithBody(
                String.valueOf(currentTimestamp), "nonce-ok", bodyDigest, "wrong-sign", body);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Verify that RequestContext is NOT enriched during the filter chain (which should not be called)
        RequestContext[] ctxDuringChain = new RequestContext[1];
        FilterChain filterChain = capturingFilterChain((servletRequest, servletResponse) -> {
            ctxDuringChain[0] = RequestContext.CONTEXT.orElse(null);
        });

        when(properties.getTimestampTolerance()).thenReturn(300L);
        when(properties.getNonceTtl()).thenReturn(300L);
        when(nonceRepository.tryAcquire(eq("nonce-ok"), any(Duration.class))).thenReturn(true);
        when(apiKeyProvider.getByAppId(APP_ID)).thenReturn(API_KEY_INFO);
        when(signatureCalculator.calculate(any(String.class), eq(APP_SECRET))).thenReturn("different-sign");

        RequestContext baseCtx = new RequestContext("req-1", "127.0.0.1", null, null, null, null, null, null);
        runInContext(baseCtx, () -> filter.doFilterInternal(request, response, filterChain));

        assertThat(response.getStatus()).isEqualTo(401);
        // The filter chain should NOT have been called (verification failed), so ctxDuringChain stays null
        assertThat(ctxDuringChain[0]).isNull();
    }

    // ---- Helper methods ----

    private static void runInContext(RequestContext ctx, ThrowingRunnable action) throws Exception {
        RequestContext.runFor(ctx, () -> {
            action.run();
            return null;
        });
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static FilterChain capturingFilterChain(BiConsumer<ServletRequest, ServletResponse> action) {
        return new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                action.accept(request, response);
            }
        };
    }

    private MockHttpServletRequest createSignedRequest(String timestamp, String nonce,
                                                       String bodyDigest, String sign) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-App-Id", APP_ID);
        request.addHeader("X-Timestamp", timestamp);
        request.addHeader("X-Nonce", nonce);
        request.addHeader("X-Body-Digest", bodyDigest);
        request.addHeader("X-Sign", sign);
        return request;
    }

    private MockHttpServletRequest createSignedRequestWithBody(String timestamp, String nonce,
                                                               String bodyDigest, String sign,
                                                               byte[] body) {
        MockHttpServletRequest request = createSignedRequest(timestamp, nonce, bodyDigest, sign);
        request.setContent(body);
        return request;
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
