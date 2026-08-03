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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * 验签 Filter，在 Servlet Filter 层完成签名验证。
 *
 * <p>从 Interceptor 拆分而来，目的是在 Filter 层完成验签后，
 * 通过 {@link RequestContext#run} 写入 callerAppId/callerAppName，
 * 解决 ScopedValue 在 Interceptor 层无法重绑定的问题。</p>
 *
 * <p>验签成功后将 {@link ApiKeyInfo} 存入 request attribute，
 * 供下游 {@link com.cartisan.openapi.interceptor.SignatureVerificationInterceptor} 做注解权限检查。</p>
 */
public class SignatureVerificationFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(SignatureVerificationFilter.class);

    /**
     * Request attribute key，存储验签成功后的 ApiKeyInfo。
     */
    public static final String API_KEY_INFO_ATTR = "openapi.apiKeyInfo";

    private final SignatureCalculator signatureCalculator;
    private final ApiKeyProvider apiKeyProvider;
    private final NonceRepository nonceRepository;
    private final CartisanOpenapiProperties properties;
    private final ObjectMapper objectMapper;

    public SignatureVerificationFilter(SignatureCalculator signatureCalculator,
                                        ApiKeyProvider apiKeyProvider,
                                        NonceRepository nonceRepository,
                                        CartisanOpenapiProperties properties,
                                        ObjectMapper objectMapper) {
        this.signatureCalculator = signatureCalculator;
        this.apiKeyProvider = apiKeyProvider;
        this.nonceRepository = nonceRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 15;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        // 1. Check X-App-Key header — if absent, skip signature verification
        String appKey = request.getHeader("X-App-Key");
        if (appKey == null || appKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. Extract other signature headers
            String timestamp = getRequiredHeader(request, "X-Timestamp");
            String nonce = getRequiredHeader(request, "X-Nonce");
            String clientBodyDigest = getRequiredHeader(request, "X-Body-Digest");
            String sign = getRequiredHeader(request, "X-Sign");

            // 3. Validate timestamp
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            if (Math.abs(currentTime - requestTime) > properties.getTimestampTolerance()) {
                writeError(response, 401, "Timestamp expired");
                return;
            }

            // 4. Validate nonce
            if (!nonceRepository.tryAcquire(nonce, Duration.ofSeconds(properties.getNonceTtl()))) {
                writeError(response, 401, "Nonce duplicated");
                return;
            }

            // 5. Get API Key
            ApiKeyInfo apiKeyInfo = apiKeyProvider.getByAppKey(appKey.trim());
            if (apiKeyInfo == null || !apiKeyInfo.isActive()) {
                writeError(response, 401, "Invalid app id");
                return;
            }

            // 6. Calculate server body digest
            byte[] body = getCachedBody(request);
            String serverBodyDigest = calculateBodyDigest(body);

            // 7. Verify body digest
            if (!serverBodyDigest.equals(clientBodyDigest)) {
                writeError(response, 401, "Body digest mismatch");
                return;
            }

            // 8. Build string to sign and verify HMAC-SHA256
            Map<String, String> queryParams = extractQueryParams(request);
            String stringToSign = buildStringToSign(appKey.trim(), clientBodyDigest, nonce, timestamp, queryParams);
            String expectedSign = signatureCalculator.calculate(stringToSign, apiKeyInfo.apiSecret());

            if (!expectedSign.equals(sign)) {
                writeError(response, 401, "Signature mismatch");
                return;
            }

            // 9. Verification succeeded — store ApiKeyInfo in request attribute
            request.setAttribute(API_KEY_INFO_ATTR, apiKeyInfo);

            // 10. Enrich RequestContext with caller info and continue filter chain
            RequestContext current = RequestContext.CONTEXT.orElse(null);
            if (current != null) {
                RequestContext enriched = current.withCaller(appKey.trim(), apiKeyInfo.appName());
                RequestContext.run(enriched, () -> {
                    try {
                        filterChain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                filterChain.doFilter(request, response);
            }

        } catch (SignatureException e) {
            writeError(response, 401, e.getMessage());
        } catch (NumberFormatException e) {
            writeError(response, 401, "Invalid timestamp");
        }
    }

    // ---- Private helper methods ----

    private String getRequiredHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            throw new SignatureException("Missing header: " + name);
        }
        return value.trim();
    }

    private byte[] getCachedBody(HttpServletRequest request) {
        if (request instanceof CachingRequestBodyFilter.CachedBodyHttpServletRequest cached) {
            return cached.getCachedBody();
        }
        try {
            return request.getInputStream().readAllBytes();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private String calculateBodyDigest(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body != null ? body : new byte[0]);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private Map<String, String> extractQueryParams(HttpServletRequest request) {
        Map<String, String> params = new TreeMap<>();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            for (String pair : queryString.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    params.put(kv[0], kv[1]);
                }
            }
        }
        return params;
    }

    private String buildStringToSign(String appKey, String bodyDigest, String nonce,
                                      String timestamp, Map<String, String> queryParams) {
        TreeMap<String, String> sortedParams = new TreeMap<>();
        sortedParams.put("appKey", appKey);
        sortedParams.put("bodyDigest", bodyDigest);
        sortedParams.put("nonce", nonce);
        sortedParams.put("timestamp", timestamp);
        sortedParams.putAll(queryParams);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        log.warn("Signature verification failed: {}", message);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            Map<String, Object> body = Map.of(
                    "code", status,
                    "message", message,
                    "success", false
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            log.error("Failed to write error response", e);
        }
    }

    private static class SignatureException extends RuntimeException {
        SignatureException(String message) {
            super(message);
        }
    }
}
