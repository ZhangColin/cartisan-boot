package com.cartisan.openapi.client;

import com.cartisan.core.context.RequestContext;
import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.cartisan.openapi.signature.SignatureCalculator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
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
            return readBody(response, responseType);
        } catch (OpenApiClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenApiClient " + method + " failed: " + url, e);
        }
    }

    /**
     * 发送 GET 请求下载二进制响应，自动签名和传递上下文。
     *
     * <p>响应全量缓冲为 byte[]（不做字符解码，非 UTF-8 字节原样到达）；响应头保留在
     * {@link BinaryResponse} 中供 BFF 透传。≥400 时响应体以 UTF-8 解码进
     * {@link OpenApiClientException}（错误信封为 JSON 文本）。</p>
     */
    public BinaryResponse download(String url) {
        try {
            URI uri = URI.create(url);
            Map<String, String> queryParams = extractQueryParams(uri.getQuery());
            Map<String, String> headers = buildHeaders("GET", new byte[0], queryParams);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .timeout(Duration.ofSeconds(properties.getTimeout().getReadSeconds()));

            headers.forEach(requestBuilder::header);

            HttpResponse<byte[]> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() >= 400) {
                throw new OpenApiClientException(response.statusCode(),
                        new String(response.body(), StandardCharsets.UTF_8));
            }
            return new BinaryResponse(response.statusCode(), response.headers(), response.body());
        } catch (OpenApiClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenApiClient download failed: " + url, e);
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
            return readBody(response, responseType);
        } catch (OpenApiClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenApiClient GET failed: " + url, e);
        }
    }

    /**
     * 发送 DELETE 请求，自动签名和传递上下文。
     *
     * <p>无请求体：空 body digest 入签、query 参数入签（同 {@link #get}）。回执按
     * {@code TypeReference} 反序列化——透传型删除端点的回执常是 {@code ApiResponse<T>}
     * JSON 信封（data＝删除前终态 DTO），调用方以信封类型取 data，而非只拿状态码。</p>
     */
    public <T> T delete(String url, TypeReference<T> responseType) {
        try {
            URI uri = URI.create(url);
            Map<String, String> queryParams = extractQueryParams(uri.getQuery());
            Map<String, String> headers = buildHeaders("DELETE", new byte[0], queryParams);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .DELETE()
                    .timeout(Duration.ofSeconds(properties.getTimeout().getReadSeconds()));

            headers.forEach(requestBuilder::header);

            HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            validateResponse(response);
            return readBody(response, responseType);
        } catch (OpenApiClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenApiClient DELETE failed: " + url, e);
        }
    }

    void validateResponse(HttpResponse<String> response) {
        if (response.statusCode() >= 400) {
            throw new OpenApiClientException(response.statusCode(), response.body());
        }
    }

    /**
     * 读取并反序列化响应 body。容忍空 body（如 204 No Content、空 200）：
     * body 为 null 或 blank 时跳过反序列化、返回 null。由 body 内容驱动，不特判状态码。
     * 仍在调用方的 try 块内执行，非空 body 的真实反序列化异常沿用原有包装语义。
     */
    private <T> T readBody(HttpResponse<String> response, TypeReference<T> responseType) throws IOException {
        String body = response.body();
        if (body == null || body.isBlank()) {
            return null;
        }
        return objectMapper.readValue(body, responseType);
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
