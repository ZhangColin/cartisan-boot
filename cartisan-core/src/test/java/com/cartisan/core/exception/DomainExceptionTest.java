package com.cartisan.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DomainException 领域层异常测试。
 */
class DomainExceptionTest {

    @Test
    void shouldExtendCartisanException() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.CONFLICT;

        // When
        DomainException exception = new DomainException(codeMessage);

        // Then
        assertThat(exception).isInstanceOf(CartisanException.class);
    }

    @Test
    void shouldSupportConstructorWithoutCause() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.DUPLICATE;

        // When
        DomainException exception = new DomainException(codeMessage, "username");

        // Then
        assertThat(exception.getMessage()).isEqualTo("Duplicate resource: username");
        assertThat(exception.getCodeMessage()).isSameAs(codeMessage);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldSupportConstructorWithCause() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.INTERNAL_SERVER_ERROR;
        Throwable cause = new RuntimeException("Database error");

        // When
        DomainException exception = new DomainException(codeMessage, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo("Internal server error");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldFormatMessageCorrectly() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.RESOURCE_NOT_FOUND;

        // When
        DomainException exception = new DomainException(codeMessage, "order/123");

        // Then
        assertThat(exception.getMessage()).isEqualTo("Resource not found: order/123");
    }
}
