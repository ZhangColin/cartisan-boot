package com.cartisan.web.response;

/**
 * 字段级错误信息。
 *
 * <p>用于参数校验失败时的结构化错误响应。</p>
 *
 * @param field 字段路径，支持嵌套，如 {@code "user.email"} 或 {@code "user.addresses[0].city"}
 * @param message 可读错误说明
 * @param errorCode 约束类型简称，如 {@code "Email"}、{@code "NotNull"}、{@code "Size"}
 */
public record FieldError(
        String field,
        String message,
        String errorCode
) {
}
