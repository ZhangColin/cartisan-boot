package com.cartisan.security.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.cartisan.web.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * cartisan-security 模块的异常处理器。
 * <p>
 * 将 Sa-Token 的鉴权异常转换为标准的 {@link ApiResponse} 格式。
 * <p>
 * 异常映射：
 * <ul>
 *   <li>{@link NotLoginException} → 401 UNAUTHORIZED</li>
 *   <li>{@link NotRoleException} → 403 FORBIDDEN</li>
 *   <li>{@link NotPermissionException} → 403 FORBIDDEN</li>
 * </ul>
 */
@ControllerAdvice
public class SecurityExceptionHandler {

    /**
     * 处理未登录异常。
     *
     * @param ex NotLoginException
     * @return 401 + ApiResponse
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "未登录或登录已过期"));
    }

    /**
     * 处理无角色异常。
     *
     * @param ex NotRoleException
     * @return 403 + ApiResponse
     */
    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotRole(NotRoleException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, "无权限访问"));
    }

    /**
     * 处理无权限异常。
     *
     * @param ex NotPermissionException
     * @return 403 + ApiResponse
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotPermission(NotPermissionException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, "无权限访问"));
    }
}
