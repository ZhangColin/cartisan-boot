package com.cartisan.core.exception;

import java.text.MessageFormat;
import java.util.Objects;

/**
 * Cartisan 异常基类。
 *
 * <p>核心特性：</p>
 * <ul>
 *   <li>携带 {@link CodeMessage} 结构化错误信息</li>
 *   <li>支持参数化消息（使用 {@link MessageFormat} 占位符）</li>
 *   <li>支持异常链（保留原始异常）</li>
 *   <li>继承 {@link RuntimeException}，为非受检异常</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 无参数
 * throw new DomainException(BaseCodeMessage.NOT_FOUND);
 *
 * // 带参数
 * throw new DomainException(BaseCodeMessage.INVALID_PARAMETER, "email");
 *
 * // 带异常链
 * try {
 *     // ...
 * } catch (SQLException e) {
 *     throw new ApplicationException(BaseCodeMessage.INTERNAL_SERVER_ERROR, e);
 * }
 * }</pre>
 *
 * @see CodeMessage
 * @see MessageFormat
 * @since 0.1.0
 */
public abstract class CartisanException extends RuntimeException {

    private final CodeMessage codeMessage;
    private final String formattedMessage;

    /**
     * 构造器 - 无异常链。
     *
     * @param codeMessage 错误码信息，不能为 {@code null}
     * @param args 消息参数，用于格式化 {@link CodeMessage#message()}
     */
    protected CartisanException(CodeMessage codeMessage, Object... args) {
        super(formatMessage(
                Objects.requireNonNull(codeMessage, "codeMessage cannot be null"),
                args
        ));
        this.codeMessage = codeMessage;
        this.formattedMessage = super.getMessage();
    }

    /**
     * 构造器 - 带异常链。
     *
     * @param codeMessage 错误码信息，不能为 {@code null}
     * @param cause 原始异常
     * @param args 消息参数，用于格式化 {@link CodeMessage#message()}
     */
    protected CartisanException(CodeMessage codeMessage, Throwable cause, Object... args) {
        super(formatMessage(
                Objects.requireNonNull(codeMessage, "codeMessage cannot be null"),
                args
        ), cause);
        this.codeMessage = codeMessage;
        this.formattedMessage = super.getMessage();
    }

    /**
     * 获取错误码信息。
     *
     * @return 结构化的错误码信息
     */
    public CodeMessage getCodeMessage() {
        return this.codeMessage;
    }

    /**
     * 获取格式化后的消息。
     *
     * <p>重写 {@link Throwable#getMessage()}，返回经过 {@link MessageFormat} 格式化的消息。</p>
     *
     * @return 格式化后的消息文本
     */
    @Override
    public String getMessage() {
        return this.formattedMessage;
    }

    /**
     * 格式化消息。
     *
     * <p>使用 {@link MessageFormat#format(String, Object...)} 格式化消息模板。
     * 如果 {@code args} 为 {@code null} 或空数组，返回原始模板。</p>
     *
     * @param codeMessage 错误码信息
     * @param args 消息参数
     * @return 格式化后的消息
     */
    private static String formatMessage(CodeMessage codeMessage, Object... args) {
        if (args == null || args.length == 0) {
            return codeMessage.message();
        }
        return MessageFormat.format(codeMessage.message(), args);
    }
}
