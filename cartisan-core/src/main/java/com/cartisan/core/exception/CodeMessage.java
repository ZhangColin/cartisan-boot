package com.cartisan.core.exception;

/**
 * 错误码接口。
 *
 * <p>定义结构化的错误信息契约，包含错误码标识、消息模板和 HTTP 状态码。
 * 消息模板支持 {@link java.text.MessageFormat} 占位符，如 {@code {0}}、{@code {1}} 等。</p>
 *
 * <p>通过将 HTTP 状态码包含在错误码中，使错误语义自包含，
 * 便于全局异常处理器直接使用。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 定义自定义错误码
 * enum UserCodeMessage implements CodeMessage {
 *     USER_NOT_FOUND(404, "USER_NOT_FOUND", "User {0} not found"),
 *     USER_DUPLICATE(409, "USER_DUPLICATE", "User {0} already exists");
 *
 *     private final int httpStatus;
 *     private final String code;
 *     private final String message;
 *
 *     UserCodeMessage(int httpStatus, String code, String message) {
 *         this.httpStatus = httpStatus;
 *         this.code = code;
 *         this.message = message;
 *     }
 *
 *     @Override public String code() { return this.code; }
 *     @Override public String message() { return this.message; }
 *     @Override public int httpStatus() { return this.httpStatus; }
 * }
 *
 * // 使用
 * throw new DomainException(UserCodeMessage.USER_NOT_FOUND, "john@example.com");
 * }</pre>
 *
 * @see java.text.MessageFormat
 * @since 0.1.0
 */
public interface CodeMessage {

    /**
     * 获取错误码标识。
     *
     * @return 错误码标识，如 {@code "USER_NOT_FOUND"}
     */
    String code();

    /**
     * 获取消息模板。
     *
     * <p>消息模板可能包含 {@link java.text.MessageFormat} 占位符，
     * 如 {@code {0}}、{@code {1}} 等，用于参数化消息。</p>
     *
     * @return 消息模板，如 {@code "User {0} not found"}
     */
    String message();

    /**
     * 获取对应的 HTTP 状态码。
     *
     * <p>HTTP 状态码应在 400-599 范围内（客户端错误或服务器错误）。</p>
     *
     * @return HTTP 状态码，如 {@code 404}
     */
    int httpStatus();
}
