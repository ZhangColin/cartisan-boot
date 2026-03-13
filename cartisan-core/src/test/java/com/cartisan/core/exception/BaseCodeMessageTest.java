package com.cartisan.core.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseCodeMessage 枚举测试。
 *
 * <p>验证所有枚举值实现 CodeMessage 接口，且 httpStatus 在有效范围内。</p>
 */
class BaseCodeMessageTest {

    @Test
    void shouldImplementCodeMessageInterface() {
        // Given & When
        Class<?> enumClass = BaseCodeMessage.class;

        // Then
        assertThat(CodeMessage.class.isAssignableFrom(enumClass))
                .as("BaseCodeMessage 应实现 CodeMessage 接口")
                .isTrue();
    }

    @ParameterizedTest(name = "{0} 的 httpStatus 应在 400-599 之间")
    @EnumSource(BaseCodeMessage.class)
    void shouldHaveValidHttpStatus(BaseCodeMessage codeMessage) {
        // When
        int httpStatus = codeMessage.httpStatus();

        // Then
        assertThat(httpStatus)
                .as("HTTP 状态码应在 400-599 之间")
                .isGreaterThanOrEqualTo(400)
                .isLessThanOrEqualTo(599);
    }

    @Test
    void shouldHaveCorrectHttpStatusCodeValues() {
        // Then - 验证 HTTP 规范错误码
        assertThat(BaseCodeMessage.BAD_REQUEST.httpStatus()).isEqualTo(400);
        assertThat(BaseCodeMessage.UNAUTHORIZED.httpStatus()).isEqualTo(401);
        assertThat(BaseCodeMessage.FORBIDDEN.httpStatus()).isEqualTo(403);
        assertThat(BaseCodeMessage.NOT_FOUND.httpStatus()).isEqualTo(404);
        assertThat(BaseCodeMessage.METHOD_NOT_ALLOWED.httpStatus()).isEqualTo(405);
        assertThat(BaseCodeMessage.CONFLICT.httpStatus()).isEqualTo(409);
        assertThat(BaseCodeMessage.UNSUPPORTED_MEDIA_TYPE.httpStatus()).isEqualTo(415);
        assertThat(BaseCodeMessage.UNPROCESSABLE_ENTITY.httpStatus()).isEqualTo(422);
        assertThat(BaseCodeMessage.TOO_MANY_REQUESTS.httpStatus()).isEqualTo(429);
        assertThat(BaseCodeMessage.INTERNAL_SERVER_ERROR.httpStatus()).isEqualTo(500);
        assertThat(BaseCodeMessage.SERVICE_UNAVAILABLE.httpStatus()).isEqualTo(503);
    }

    @Test
    void shouldHaveCorrectCodeValues() {
        // Then - 验证 code() 返回值
        assertThat(BaseCodeMessage.BAD_REQUEST.code()).isEqualTo("BAD_REQUEST");
        assertThat(BaseCodeMessage.UNAUTHORIZED.code()).isEqualTo("UNAUTHORIZED");
        assertThat(BaseCodeMessage.FORBIDDEN.code()).isEqualTo("FORBIDDEN");
        assertThat(BaseCodeMessage.NOT_FOUND.code()).isEqualTo("NOT_FOUND");
        assertThat(BaseCodeMessage.METHOD_NOT_ALLOWED.code()).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(BaseCodeMessage.CONFLICT.code()).isEqualTo("CONFLICT");
        assertThat(BaseCodeMessage.UNSUPPORTED_MEDIA_TYPE.code()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
        assertThat(BaseCodeMessage.UNPROCESSABLE_ENTITY.code()).isEqualTo("UNPROCESSABLE_ENTITY");
        assertThat(BaseCodeMessage.TOO_MANY_REQUESTS.code()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(BaseCodeMessage.INTERNAL_SERVER_ERROR.code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(BaseCodeMessage.SERVICE_UNAVAILABLE.code()).isEqualTo("SERVICE_UNAVAILABLE");

        // 验证通用业务错误码
        assertThat(BaseCodeMessage.UNKNOWN_ERROR.code()).isEqualTo("UNKNOWN_ERROR");
        assertThat(BaseCodeMessage.INVALID_PARAMETER.code()).isEqualTo("INVALID_PARAMETER");
        assertThat(BaseCodeMessage.RESOURCE_NOT_FOUND.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(BaseCodeMessage.DUPLICATE.code()).isEqualTo("DUPLICATE");
    }

    @Test
    void shouldHaveCorrectMessageValues() {
        // Then - 验证 HTTP 规范错误码消息
        assertThat(BaseCodeMessage.BAD_REQUEST.message()).isEqualTo("Invalid request");
        assertThat(BaseCodeMessage.UNAUTHORIZED.message()).isEqualTo("Authentication required");
        assertThat(BaseCodeMessage.FORBIDDEN.message()).isEqualTo("Access denied");
        assertThat(BaseCodeMessage.NOT_FOUND.message()).isEqualTo("Resource not found");
        assertThat(BaseCodeMessage.METHOD_NOT_ALLOWED.message()).isEqualTo("Method not allowed");
        assertThat(BaseCodeMessage.CONFLICT.message()).isEqualTo("Resource conflict");
        assertThat(BaseCodeMessage.UNSUPPORTED_MEDIA_TYPE.message()).isEqualTo("Unsupported media type");
        assertThat(BaseCodeMessage.UNPROCESSABLE_ENTITY.message()).isEqualTo("Unprocessable entity");
        assertThat(BaseCodeMessage.TOO_MANY_REQUESTS.message()).isEqualTo("Too many requests");
        assertThat(BaseCodeMessage.INTERNAL_SERVER_ERROR.message()).isEqualTo("Internal server error");
        assertThat(BaseCodeMessage.SERVICE_UNAVAILABLE.message()).isEqualTo("Service unavailable");

        // 验证通用业务错误码消息
        assertThat(BaseCodeMessage.UNKNOWN_ERROR.message()).isEqualTo("Unknown error occurred");
        assertThat(BaseCodeMessage.INVALID_PARAMETER.message()).isEqualTo("Invalid parameter: {0}");
        assertThat(BaseCodeMessage.RESOURCE_NOT_FOUND.message()).isEqualTo("Resource not found: {0}");
        assertThat(BaseCodeMessage.DUPLICATE.message()).isEqualTo("Duplicate resource: {0}");
    }

    @Test
    void shouldContainPlaceholder_inBusinessErrorCodeMessages() {
        // Then - 验证通用业务错误码包含占位符
        assertThat(BaseCodeMessage.INVALID_PARAMETER.message()).contains("{0}");
        assertThat(BaseCodeMessage.RESOURCE_NOT_FOUND.message()).contains("{0}");
        assertThat(BaseCodeMessage.DUPLICATE.message()).contains("{0}");

        // UNKNOWN_ERROR 不应有占位符
        assertThat(BaseCodeMessage.UNKNOWN_ERROR.message()).doesNotContain("{");
    }
}
