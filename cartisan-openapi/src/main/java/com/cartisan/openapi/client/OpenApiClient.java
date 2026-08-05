package com.cartisan.openapi.client;

import com.cartisan.core.context.RequestContext;
import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.cartisan.openapi.signature.SignatureCalculator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 签名请求客户端，自动签名 + 跨服务 RequestContext 传递。
 */
public class OpenApiClient {

    private final CartisanOpenapiProperties properties;
    private final SignatureCalculator signatureCalculator;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenApiClient(CartisanOpenapiProperties properties,
                          SignatureCalculator signatureCalculator,
                          ObjectMapper objectMapper) {
        this.properties = properties;
        this.signatureCalculator = signatureCalculator;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeout().getConnectSeconds()))
                .build();
    }

    /**
     * 发送 POST 请求，自动签名和传递上下文。
     */
    public <T> T post(String url, Object body, TypeReference<T> responseType) {
        return sendWithBody("POST", url, body, responseType);
    }

    /**
     * 发送 PUT 请求，自动签名和传递上下文。
     */
    public <T> T put(String url, Object body, TypeReference<T> responseType) {
        return sendWithBody("PUT", url, body, responseType);
    }

    private <T> T sendWithBody(String method, String url, Object body, TypeReference<T> responseType) {
        try {
            byte[] bodyBytes = body != null ? objectMapper.writeValueAsBytes(body) : new byte[0];
            Map<String, String> headers = buildHeaders(method, bodyBytes, null);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(properties.getTimeout().getReadSeconds()));

            if ("POST".equals(method)) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
            } else if ("PUT".equals(method)) {
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
            } else {
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

            headers.forEach(requestBuilder::header);
            requestBuilder.header("Content-Type", "application/json");

            HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            validateResponse(response);
            return objectMapper.readValue(response.body(), responseType);
        } catch (OpenApiClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenApiClient " + method + " failed: " + url, e);
        }
    }

    /**
     * 发送 GET 请求，自动签名和传递上下文。
     */
    public <T> T get(String url, TypeReference<T> responseType) {
        try {
            URI uri = URI.create(url);
            Map<String, String> queryParams = extractQueryParams(uri.getQuery());
            Map<String, String> headers = buildHeaders("GET", new byte[0], queryParams);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(properties.getTimeout().getReadSeconds()));

            headers.forEach(requestBuilder::header);

            HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            validateResponse(response);
            return objectMapper.readValue(response.body(), responseType);
        } catch (OpenApiClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenApiClient GET failed: " + url, e);
        }
    }

    void validateResponse(HttpResponse<String> response) {
        if (response.statusCode() >= 400) {
            throw new OpenApiClientException(response.statusCode(), response.body());
        }
    }

    private Map<String, String> buildHeaders(String method, byte[] body, Map<String, String> queryParams) {
        Map<String, String> headers = new TreeMap<>();

        String apiKey = properties.getSelf().getApiKey();
        String apiSecret = properties.getSelf().getApiSecret();

        // Signature headers
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String bodyDigest = calculateBodyDigest(body);

        // Build string to sign
        TreeMap<String, String> signParams = new TreeMap<>();
        signParams.put("apiKey", apiKey);
        signParams.put("bodyDigest", bodyDigest);
        signParams.put("nonce", nonce);
        signParams.put("timestamp", timestamp);
        if (queryParams != null) {
            signParams.putAll(queryParams);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : signParams.entrySet()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }

        String sign = signatureCalculator.calculate(sb.toString(), apiSecret);

        headers.put("X-Api-Key", apiKey);
        headers.put("X-Timestamp", timestamp);
        headers.put("X-Nonce", nonce);
        headers.put("X-Body-Digest", bodyDigest);
        headers.put("X-Sign", sign);

        // Cross-service context propagation (non-caller fields)
        RequestContext ctx = RequestContext.CONTEXT.orElse(null);
        if (ctx != null) {
            if (ctx.requestId() != null) headers.put("X-Request-Id", ctx.requestId());
            if (ctx.clientIp() != null) headers.put("X-Client-Ip", ctx.clientIp());
            if (ctx.userId() != null) headers.put("X-User-Id", String.valueOf(ctx.userId()));
            if (ctx.userName() != null) headers.put("X-User-Name", URLEncoder.encode(ctx.userName(), StandardCharsets.UTF_8));
            if (ctx.tenantId() != null) headers.put("X-Tenant-Id", String.valueOf(ctx.tenantId()));
            if (ctx.tenantName() != null) headers.put("X-Tenant-Name", URLEncoder.encode(ctx.tenantName(), StandardCharsets.UTF_8));
        }

        return headers;
    }

    private Map<String, String> extractQueryParams(String query) {
        Map<String, String> params = new TreeMap<>();
        if (query != null && !query.isEmpty()) {
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    params.put(kv[0], kv[1]);
                }
            }
        }
        return params;
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
}
