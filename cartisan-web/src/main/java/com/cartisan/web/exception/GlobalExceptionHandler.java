package com.cartisan.web.exception;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.CartisanException;
import com.cartisan.core.context.RequestContext;
import com.cartisan.web.config.EnumErrorProperties;
import com.cartisan.web.resubmit.ResubmitException;
import com.cartisan.web.response.ApiResponse;
import com.cartisan.web.response.FieldError;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * 将各类异常统一映射为 {@link ApiResponse} 响应，确保：
 * <ul>
 *   <li>正确的 HTTP 状态码（4xx vs 5xx）</li>
 *   <li>统一的响应体结构</li>
 *   <li>合理的日志策略（4xx → WARN，5xx → ERROR）</li>
 * </ul>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final EnumErrorProperties enumErrorProperties;

    public GlobalExceptionHandler() {
        this(new EnumErrorProperties());
    }

    public GlobalExceptionHandler(EnumErrorProperties enumErrorProperties) {
        this.enumErrorProperties = enumErrorProperties;
    }

    // ========== 业务异常 ==========

    @ExceptionHandler(CartisanException.class)
    public ResponseEntity<ApiResponse<Void>> handleCartisanException(CartisanException ex) {
        int status = ex.getCodeMessage().httpStatus();

        if (status >= 500) {
            log.error("Business error: {}", ex.getMessage(), ex);
        } else {
            log.warn("Business error: {}", ex.getMessage());
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getCodeMessage()).withRequestId(currentRequestId()));
    }

    @ExceptionHandler(ResubmitException.class)
    public ResponseEntity<ApiResponse<Void>> handleResubmitException(ResubmitException ex) {
        log.warn("Resubmit blocked: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, ex.getMessage()).withRequestId(currentRequestId()));
    }

    // ========== 数据库完整性冲突 ==========

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException ex) {
        // 唯一/重复键冲突是客户端冲突（4xx），非服务故障。DB 原始消息含 constraint/列名等 schema 细节，只入日志、不进响应体。
        log.warn("Duplicate key violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(BaseCodeMessage.CONFLICT).withRequestId(currentRequestId()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // 其余完整性冲突（外键 / check / not-null）：客户端提交的数据违反 DB 约束 → 400。
        // DuplicateKeyException 是本异常的子类，Spring MVC 按"最具体匹配"优先走上面的 409 handler，故二者共存不冲突。
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(BaseCodeMessage.BAD_REQUEST).withRequestId(currentRequestId()));
    }

    // ========== 校验异常 ==========

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .map(cv -> new FieldError(
                        cv.getPropertyPath().toString(),
                        cv.getMessage(),
                        cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
                ))
                .collect(Collectors.toList());

        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError(errors).withRequestId(currentRequestId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        extractCode(fe)
                ))
                .collect(Collectors.toList());

        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError(errors).withRequestId(currentRequestId()));
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(org.springframework.validation.BindException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        extractCode(fe)
                ))
                .collect(Collectors.toList());

        log.warn("Binding failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError(errors).withRequestId(currentRequestId()));
    }

    // ========== HTTP 方法/媒体类型异常 ==========

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        Set<HttpMethod> supported = ex.getSupportedHttpMethods();
        String supportedMethods = (supported != null)
                ? supported.stream().map(HttpMethod::name).collect(Collectors.joining(", "))
                : "";
        log.warn("Method not supported: {} (supported: {})", ex.getMethod(), supportedMethods);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(405, "Method " + ex.getMethod() + " not supported" +
                        (supportedMethods.isEmpty() ? "" : ". Supported: " + supportedMethods))
                        .withRequestId(currentRequestId()));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(BaseCodeMessage.NOT_FOUND).withRequestId(currentRequestId()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String paramValue = ex.getValue() != null ? ex.getValue().toString() : "null";

        // BaseEnum 取值非法：组装 参数名 + 非法值 + 合法取值表，并应用业务码覆盖
        InvalidEnumValueException enumEx = findCause(ex, InvalidEnumValueException.class);
        if (enumEx != null) {
            log.warn("Invalid enum value: {} = {}", paramName, paramValue);
            return enumErrorResponse(paramName, enumEx);
        }

        // 其他类型不匹配（如路径变量应为 Long 传了 abc）：不暴露转换细节，返回 404。
        // 注：NumberFormatException 等 IllegalArgumentException 子类同样走此分支，
        // BaseEnum 场景已由上方专用异常分支覆盖。
        log.warn("Type mismatch: {} = {}", paramName, paramValue);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(BaseCodeMessage.NOT_FOUND).withRequestId(currentRequestId()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Media type not supported: {}", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(415, "Media type not supported").withRequestId(currentRequestId()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        // BaseEnum 字段取值非法：从 Jackson 包装的 cause 链提取专用异常，
        // 组装 字段名 + 非法值 + 合法取值表（替代一句 Malformed request body）
        InvalidEnumValueException enumEx = findCause(ex, InvalidEnumValueException.class);
        if (enumEx != null) {
            log.warn("Invalid enum value in request body: {}", enumEx.getMessage());
            return enumErrorResponse(extractJsonFieldName(ex.getCause()), enumEx);
        }

        log.warn("Request body not readable");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Malformed request body").withRequestId(currentRequestId()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Missing required parameter: " + ex.getParameterName()).withRequestId(currentRequestId()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("Missing header: {}", ex.getHeaderName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Missing required header: " + ex.getHeaderName()).withRequestId(currentRequestId()));
    }

    // ========== 兜底处理 ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(BaseCodeMessage.INTERNAL_SERVER_ERROR).withRequestId(currentRequestId()));
    }

    // ========== 辅助方法 ==========

    private String currentRequestId() {
        return RequestContext.getRequestId();
    }

    /**
     * 沿 cause 链查找指定类型的异常（限制深度，防循环引用）。
     *
     * @param throwable 起始异常
     * @param type 目标异常类型
     * @return 命中的异常，未命中返回 null
     */
    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < 10; depth++) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * 从 Jackson 的 {@link JsonMappingException} 中提取出错字段名（取路径最深一层）。
     *
     * @param cause HttpMessageNotReadableException 的 cause
     * @return 字段名，提取不到返回 null
     */
    private String extractJsonFieldName(Throwable cause) {
        if (cause instanceof JsonMappingException mappingEx) {
            return mappingEx.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse(null);
        }
        return null;
    }

    /**
     * 组装 BaseEnum 取值错误的 400 信封（query/path 与 JSON body 两条路径共用）。
     *
     * <p>message 为 {@code 名字 取值 X 非法，合法取值：code=名称对照表}；
     * 名字为参数名（query/path）或字段名（JSON body），提取不到时回退枚举简单名。</p>
     *
     * @param name 参数名或字段名，可为 null
     * @param ex BaseEnum 取值异常
     * @return 400 响应
     */
    private ResponseEntity<ApiResponse<Void>> enumErrorResponse(String name, InvalidEnumValueException ex) {
        String message = InvalidEnumValueException.message(name, ex.getEnumType(), ex.getInvalidValue());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(resolveEnumErrorCode(ex), message)
                        .withRequestId(currentRequestId()));
    }

    /**
     * 解析 BaseEnum 取值错误的响应业务码。
     *
     * <p>默认 400；命中 {@code cartisan.web.enum-error.codes}（FQN 或简单类名）时
     * 返回配置的业务码，HTTP 状态保持 400。</p>
     *
     * @param ex BaseEnum 取值异常
     * @return 业务码
     */
    private int resolveEnumErrorCode(InvalidEnumValueException ex) {
        Class<?> enumType = ex.getEnumType();
        Map<String, Integer> codes = enumErrorProperties.getCodes();
        Integer code = codes.get(enumType.getName());
        if (code == null) {
            code = codes.get(enumType.getSimpleName());
        }
        return code != null ? code : BaseCodeMessage.BAD_REQUEST.httpStatus();
    }

    /**
     * 从 Spring FieldError 提取错误码。
     *
     * @param fe Spring FieldError
     * @return 错误码，或 "Invalid" 作为兜底
     */
    private String extractCode(org.springframework.validation.FieldError fe) {
        String code = fe.getCode();
        return code != null ? code : "Invalid";
    }
}
