package com.cartisan.web.response;

import com.cartisan.core.exception.CodeMessage;
import com.cartisan.core.exception.BaseCodeMessage;

import java.text.MessageFormat;
import java.util.List;

/**
 * 统一 API 响应体。
 *
 * <p>使用 Record 实现，不可变对象。</p>
 *
 * @param <T> 响应数据类型
 */
public record ApiResponse<T>(
        /** HTTP 状态码/业务错误码 */
        int code,

        /** 响应消息 */
        String message,

        /** 响应数据，成功时为业务数据，错误时为 null */
        T data,

        /** 请求追踪 ID，可选字段（由 F02-03 填充） */
        String requestId,

        /** 字段级错误信息，校验失败时填充 */
        List<FieldError> errors
) {

    /**
     * 构造成功响应（带数据）。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return code=200, message="Success", data=传入值, requestId=null, errors=null
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(
                BaseCodeMessage.SUCCESS.httpStatus(),
                BaseCodeMessage.SUCCESS.message(),
                data,
                null,
                null
        );
    }

    /**
     * 构造成功响应（无数据）。
     *
     * @return code=200, message="Success", data=null, requestId=null, errors=null
     */
    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    /**
     * 构造错误响应（使用错误码枚举）。
     *
     * @param codeMessage 错误码枚举
     * @return code=枚举.httpStatus(), message=枚举.message(), data=null, errors=null
     */
    public static ApiResponse<Void> error(CodeMessage codeMessage) {
        return new ApiResponse<>(
                codeMessage.httpStatus(),
                codeMessage.message(),
                null,
                null,
                null
        );
    }

    /**
     * 构造错误响应（支持参数化消息）。
     *
     * <p>消息格式使用 {@link MessageFormat#format(String, Object...)}，
     * 支持占位符：{@code {0}}、{@code {1}} 等。</p>
     *
     * @param codeMessage 错误码枚举
     * @param args        消息参数（可为空）
     * @return 格式化后的错误响应
     */
    public static ApiResponse<Void> error(CodeMessage codeMessage, Object... args) {
        String formattedMessage;
        if (args == null || args.length == 0) {
            formattedMessage = codeMessage.message();
        } else {
            formattedMessage = MessageFormat.format(codeMessage.message(), args);
        }
        return new ApiResponse<>(
                codeMessage.httpStatus(),
                formattedMessage,
                null,
                null,
                null
        );
    }

    /**
     * 构造错误响应（自定义错误码和消息）。
     *
     * <p>用于第三方异常转换、临时错误等场景。</p>
     *
     * @param code    自定义错误码
     * @param message 自定义错误消息
     * @return 自定义错误响应
     */
    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null, null, null);
    }

    /**
     * 构造校验失败响应（带字段级错误）。
     *
     * @param errors 字段级错误列表
     * @return code=400, message="Parameter validation failed", data=null, errors=传入值
     */
    public static ApiResponse<Void> validationError(List<FieldError> errors) {
        return new ApiResponse<>(
                BaseCodeMessage.BAD_REQUEST.httpStatus(),
                "Parameter validation failed",
                null,
                null,
                errors
        );
    }
}
