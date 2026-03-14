package com.cartisan.web.exception;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.CartisanException;
import com.cartisan.web.response.ApiResponse;
import com.cartisan.web.response.FieldError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
                .body(ApiResponse.error(ex.getCodeMessage()));
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
                .body(ApiResponse.validationError(errors));
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
                .body(ApiResponse.validationError(errors));
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
                .body(ApiResponse.validationError(errors));
    }

    // ========== 权限异常 ==========

    /**
     * 处理 IllegalArgumentException 作为权限拒绝示例。
     * 实际项目中应使用 Spring Security 的 AccessDeniedException。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(IllegalArgumentException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Access denied")) {
            log.warn("Access denied: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(BaseCodeMessage.FORBIDDEN));
        }
        // 其他 IllegalArgumentException 作为通用 400 处理
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, ex.getMessage()));
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
                        (supportedMethods.isEmpty() ? "" : ". Supported: " + supportedMethods)));
    }

    @ExceptionHandler({NoHandlerFoundException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(BaseCodeMessage.NOT_FOUND));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Media type not supported: {}", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(415, "Media type not supported"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Request body not readable");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Malformed request body"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Missing required parameter: " + ex.getParameterName()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("Missing header: {}", ex.getHeaderName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Missing required header: " + ex.getHeaderName()));
    }

    // ========== 兜底处理 ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(BaseCodeMessage.INTERNAL_SERVER_ERROR));
    }

    // ========== 辅助方法 ==========

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
