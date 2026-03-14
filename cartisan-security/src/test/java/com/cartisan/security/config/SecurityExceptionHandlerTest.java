package com.cartisan.security.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.cartisan.web.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityExceptionHandler 单元测试。
 * <p>
 * 测试命名规范：given_{条件}_when_{操作}_then_{预期结果}
 */
class SecurityExceptionHandlerTest {

    private final SecurityExceptionHandler handler = new SecurityExceptionHandler();

    // ========== AC8: 异常处理器转 401/403 ==========

    @Test
    void given_notLoginException_when_handleNotLogin_then_return401WithApiResponse() {
        // Given: NotLoginException
        NotLoginException ex = new NotLoginException(null, null, null);

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleNotLogin(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(401);
        assertThat(response.getBody().message()).contains("未登录");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void given_notRoleException_when_handleNotRole_then_return403WithApiResponse() {
        // Given: NotRoleException (单参数构造器)
        NotRoleException ex = new NotRoleException("admin");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleNotRole(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(403);
        assertThat(response.getBody().message()).contains("无权限");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void given_notPermissionException_when_handleNotPermission_then_return403WithApiResponse() {
        // Given: NotPermissionException (单参数构造器)
        NotPermissionException ex = new NotPermissionException("user:create");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleNotPermission(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(403);
        assertThat(response.getBody().message()).contains("无权限");
        assertThat(response.getBody().data()).isNull();
    }
}
