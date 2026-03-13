package com.cartisan.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApplicationException 应用层异常测试。
 */
class ApplicationExceptionTest {

    @Test
    void shouldExtendCartisanException() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.FORBIDDEN;

        // When
        ApplicationException exception = new ApplicationException(codeMessage);

        // Then
        assertThat(exception).isInstanceOf(CartisanException.class);
    }

    @Test
    void shouldSupportConstructorWithoutCause() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.INVALID_PARAMETER;

        // When
        ApplicationException exception = new ApplicationException(codeMessage, "userId");

        // Then
        assertThat(exception.getMessage()).isEqualTo("Invalid parameter: userId");
        assertThat(exception.getCodeMessage()).isSameAs(codeMessage);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldSupportConstructorWithCause() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.UNAUTHORIZED;
        Throwable cause = new SecurityException("Token expired");

        // When
        ApplicationException exception = new ApplicationException(codeMessage, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo("Authentication required");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldFormatMessageCorrectly() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.RESOURCE_NOT_FOUND;

        // When
        ApplicationException exception = new ApplicationException(codeMessage, "user/profile");

        // Then
        assertThat(exception.getMessage()).isEqualTo("Resource not found: user/profile");
    }
}
