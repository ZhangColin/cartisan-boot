package com.cartisan.openapi.interceptor;

import com.cartisan.core.context.RequestContext;
import com.cartisan.openapi.annotation.NoSignature;
import com.cartisan.openapi.annotation.RequireSignature;
import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.cartisan.openapi.filter.CachingRequestBodyFilter;
import com.cartisan.openapi.nonce.NonceRepository;
import com.cartisan.openapi.provider.ApiKeyInfo;
import com.cartisan.openapi.provider.ApiKeyProvider;
import com.cartisan.openapi.signature.SignatureCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * 验签拦截器。
 *
 * <p>处理 @RequireSignature 和 @NoSignature 注解，执行签名验证流程。</p>
 */
public class SignatureVerificationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SignatureVerificationInterceptor.class);

    private final SignatureCalculator signatureCalculator;
    private final ApiKeyProvider apiKeyProvider;
    private final NonceRepository nonceRepository;
    private final CartisanOpenapiProperties properties;
    private final ObjectMapper objectMapper;

    public SignatureVerificationInterceptor(SignatureCalculator signatureCalculator,
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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Check @NoSignature (exclusion)
        if (handlerMethod.getMethodAnnotation(NoSignature.class) != null) {
            return true;
        }

        // Check @RequireSignature
        RequireSignature methodAnnotation = handlerMethod.getMethodAnnotation(RequireSignature.class);
        RequireSignature classAnnotation = handlerMethod.getBeanType().getAnnotation(RequireSignature.class);
        if (methodAnnotation == null && classAnnotation == null) {
            return true;
        }

        RequireSignature effectiveAnnotation = methodAnnotation != null ? methodAnnotation : classAnnotation;

        try {
            // 1. Extract headers
            String appId = getRequiredHeader(request, "X-App-Id");
            String timestamp = getRequiredHeader(request, "X-Timestamp");
            String nonce = getRequiredHeader(request, "X-Nonce");
            String clientBodyDigest = getRequiredHeader(request, "X-Body-Digest");
            String sign = getRequiredHeader(request, "X-Sign");

            // 2. Validate timestamp
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            if (Math.abs(currentTime - requestTime) > properties.getTimestampTolerance()) {
                writeError(response, 401, "Timestamp expired");
                return false;
            }

            // 3. Validate nonce
            if (!nonceRepository.tryAcquire(nonce, Duration.ofSeconds(properties.getNonceTtl()))) {
                writeError(response, 401, "Nonce duplicated");
                return false;
            }

            // 4. Get API Key
            ApiKeyInfo apiKeyInfo = apiKeyProvider.getByAppId(appId);
            if (apiKeyInfo == null || !apiKeyInfo.isActive()) {
                writeError(response, 401, "Invalid app id");
                return false;
            }

            // 5. Calculate server body digest
            byte[] body = getCachedBody(request);
            String serverBodyDigest = calculateBodyDigest(body);

            // 6. Verify body digest
            if (!serverBodyDigest.equals(clientBodyDigest)) {
                writeError(response, 401, "Body digest mismatch");
                return false;
            }

            // 7. Build string to sign
            Map<String, String> queryParams = extractQueryParams(request);
            String stringToSign = buildStringToSign(appId, clientBodyDigest, nonce, timestamp, queryParams);
            String expectedSign = signatureCalculator.calculate(stringToSign, apiKeyInfo.apiSecret());

            // 8. Verify signature
            if (!expectedSign.equals(sign)) {
                writeError(response, 401, "Signature mismatch");
                return false;
            }

            // 9. Check permission
            String requiredPermission = effectiveAnnotation.permission();
            if (!requiredPermission.isEmpty() && !apiKeyInfo.hasPermission(requiredPermission)) {
                writeError(response, 403, "Permission denied");
                return false;
            }

            // 10. Rebind RequestContext with caller info
            RequestContext current = RequestContext.CONTEXT.orElse(null);
            if (current != null) {
                RequestContext enriched = current.withCaller(appId, apiKeyInfo.appName());
                // We need to rebind - but ScopedValue can't be rebound in the same scope
                // Store in request attribute for downstream use
                request.setAttribute("callerAppId", appId);
                request.setAttribute("callerAppName", apiKeyInfo.appName());
            }

            return true;

        } catch (SignatureException e) {
            writeError(response, 401, e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            writeError(response, 401, "Invalid timestamp");
            return false;
        }
    }

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

    private String buildStringToSign(String appId, String bodyDigest, String nonce,
                                      String timestamp, Map<String, String> queryParams) {
        TreeMap<String, String> sortedParams = new TreeMap<>();
        sortedParams.put("appId", appId);
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

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        log.warn("Signature verification failed: {}", message);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = Map.of(
                "code", status,
                "message", message,
                "success", false
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static class SignatureException extends RuntimeException {
        SignatureException(String message) {
            super(message);
        }
    }
}
