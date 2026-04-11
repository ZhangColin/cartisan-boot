package com.cartisan.openapi.client;

/**
 * OpenApiClient 请求失败时抛出的异常，包含 HTTP 状态码和响应体。
 */
public class OpenApiClientException extends RuntimeException {

    private static final int MAX_BODY_LENGTH = 200;

    private final int statusCode;
    private final String body;

    public OpenApiClientException(int statusCode, String body) {
        super(formatMessage(statusCode, body));
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    private static String formatMessage(int statusCode, String body) {
        String truncated = body != null && body.length() > MAX_BODY_LENGTH
                ? body.substring(0, MAX_BODY_LENGTH) + "..."
                : body;
        return "OpenApiClient request failed with status %d: %s".formatted(statusCode, truncated);
    }
}
