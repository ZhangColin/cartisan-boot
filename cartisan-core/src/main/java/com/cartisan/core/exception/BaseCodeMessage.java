package com.cartisan.core.exception;

/**
 * 基础错误码枚举。
 *
 * <p>提供 HTTP 规范错误码和最小通用业务错误码。</p>
 *
 * <h2>HTTP 规范错误码</h2>
 * <ul>
 *   <li>{@link #SUCCESS} - 200</li>
 *   <li>{@link #BAD_REQUEST} - 400</li>
 *   <li>{@link #UNAUTHORIZED} - 401</li>
 *   <li>{@link #FORBIDDEN} - 403</li>
 *   <li>{@link #NOT_FOUND} - 404</li>
 *   <li>{@link #METHOD_NOT_ALLOWED} - 405</li>
 *   <li>{@link #CONFLICT} - 409</li>
 *   <li>{@link #UNSUPPORTED_MEDIA_TYPE} - 415</li>
 *   <li>{@link #UNPROCESSABLE_ENTITY} - 422</li>
 *   <li>{@link #TOO_MANY_REQUESTS} - 429</li>
 *   <li>{@link #INTERNAL_SERVER_ERROR} - 500</li>
 *   <li>{@link #SERVICE_UNAVAILABLE} - 503</li>
 * </ul>
 *
 * <h2>通用业务错误码</h2>
 * <ul>
 *   <li>{@link #UNKNOWN_ERROR} - 兜底错误</li>
 *   <li>{@link #INVALID_PARAMETER} - 参数校验失败（支持占位符 {@code {0}}）</li>
 *   <li>{@link #RESOURCE_NOT_FOUND} - 资源不存在（支持占位符 {@code {0}}）</li>
 *   <li>{@link #DUPLICATE} - 重复冲突（支持占位符 {@code {0}}）</li>
 *   <li>{@link #THIRD_PARTY_ERROR} - 第三方服务错误（支持占位符 {@code {0}}）</li>
 * </ul>
 *
 * @since 0.1.0
 */
public enum BaseCodeMessage implements CodeMessage {

    // ========== HTTP 规范错误码 ==========

    /**
     * 200 OK - 请求成功。
     */
    SUCCESS(200, "success", "Success"),

    /**
     * 400 Bad Request - 请求参数无效。
     */
    BAD_REQUEST(400, "BAD_REQUEST", "Invalid request"),

    /**
     * 401 Unauthorized - 未认证。
     */
    UNAUTHORIZED(401, "UNAUTHORIZED", "Authentication required"),

    /**
     * 403 Forbidden - 无权限访问。
     */
    FORBIDDEN(403, "FORBIDDEN", "Access denied"),

    /**
     * 404 Not Found - 资源不存在。
     */
    NOT_FOUND(404, "NOT_FOUND", "Resource not found"),

    /**
     * 405 Method Not Allowed - HTTP 方法不支持。
     */
    METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "Method not allowed"),

    /**
     * 409 Conflict - 资源冲突。
     */
    CONFLICT(409, "CONFLICT", "Resource conflict"),

    /**
     * 415 Unsupported Media Type - 媒体类型不支持。
     */
    UNSUPPORTED_MEDIA_TYPE(415, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type"),

    /**
     * 422 Unprocessable Entity - 请求格式正确但语义错误。
     */
    UNPROCESSABLE_ENTITY(422, "UNPROCESSABLE_ENTITY", "Unprocessable entity"),

    /**
     * 429 Too Many Requests - 请求过多。
     */
    TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", "Too many requests"),

    /**
     * 500 Internal Server Error - 服务器内部错误。
     */
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "Internal server error"),

    /**
     * 503 Service Unavailable - 服务不可用。
     */
    SERVICE_UNAVAILABLE(503, "SERVICE_UNAVAILABLE", "Service unavailable"),

    // ========== 通用业务错误码 ==========

    /**
     * 未知错误 - 兜底错误码。
     */
    UNKNOWN_ERROR(500, "UNKNOWN_ERROR", "Unknown error occurred"),

    /**
     * 参数无效 - 支持占位符 {@code {0}} 指定参数名。
     * <p>示例：{@code "Invalid parameter: email"}</p>
     */
    INVALID_PARAMETER(400, "INVALID_PARAMETER", "Invalid parameter: {0}"),

    /**
     * 资源未找到 - 支持占位符 {@code {0}} 指定资源标识。
     * <p>示例：{@code "Resource not found: user/123"}</p>
     */
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "Resource not found: {0}"),

    /**
     * 资源重复 - 支持占位符 {@code {0}} 指定重复的资源。
     * <p>示例：{@code "Duplicate resource: username"}</p>
     */
    DUPLICATE(409, "DUPLICATE", "Duplicate resource: {0}"),

    /**
     * 第三方服务错误 - 支持占位符 {@code {0}} 指定错误详情。
     * <p>示例：{@code "Third-party service error: OpenAI API returned 429"}</p>
     */
    THIRD_PARTY_ERROR(502, "THIRD_PARTY_ERROR", "Third-party service error: {0}");

    private final int httpStatus;
    private final String code;
    private final String message;

    BaseCodeMessage(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return this.code;
    }

    @Override
    public String message() {
        return this.message;
    }

    @Override
    public int httpStatus() {
        return this.httpStatus;
    }
}
