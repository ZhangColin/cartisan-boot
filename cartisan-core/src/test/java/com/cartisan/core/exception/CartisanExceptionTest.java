package com.cartisan.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CartisanException 抽象类测试。
 *
 * <p>由于 CartisanException 是抽象类，测试时使用具体子类。</p>
 */
class CartisanExceptionTest {

    /**
     * 测试用具体异常类 - 用于实例化抽象的 CartisanException。
     */
    private static class TestCartisanException extends CartisanException {
        TestCartisanException(CodeMessage codeMessage, Object... args) {
            super(codeMessage, args);
        }

        TestCartisanException(CodeMessage codeMessage, Throwable cause, Object... args) {
            super(codeMessage, cause, args);
        }
    }

    @Test
    void shouldStoreCodeMessage() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.NOT_FOUND;

        // When
        TestCartisanException exception = new TestCartisanException(codeMessage);

        // Then
        assertThat(exception.getCodeMessage()).isSameAs(codeMessage);
    }

    @Test
    void shouldFormatMessageWithoutPlaceholder() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.BAD_REQUEST;

        // When
        TestCartisanException exception = new TestCartisanException(codeMessage);

        // Then
        assertThat(exception.getMessage()).isEqualTo("Invalid request");
    }

    @Test
    void shouldFormatMessageWithSinglePlaceholder() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.INVALID_PARAMETER;

        // When
        TestCartisanException exception = new TestCartisanException(codeMessage, "email");

        // Then
        assertThat(exception.getMessage()).isEqualTo("Invalid parameter: email");
    }

    @Test
    void shouldFormatMessageWithMultiplePlaceholders() {
        // Given - 使用带多个占位符的自定义 CodeMessage
        CodeMessage codeMessage = new CodeMessage() {
            @Override
            public String code() {
                return "TEST_MULTI";
            }

            @Override
            public String message() {
                return "Error {0} at {1}";
            }

            @Override
            public int httpStatus() {
                return 400;
            }
        };

        // When
        TestCartisanException exception = new TestCartisanException(codeMessage, "type", "location");

        // Then
        assertThat(exception.getMessage()).isEqualTo("Error type at location");
    }

    @Test
    void shouldHandleEmptyArgs() {
        // Given - 带占位符的 CodeMessage
        CodeMessage codeMessage = BaseCodeMessage.INVALID_PARAMETER;

        // When - 不传参数
        TestCartisanException exception = new TestCartisanException(codeMessage);

        // Then - MessageFormat 会返回原始模板
        assertThat(exception.getMessage()).isEqualTo("Invalid parameter: {0}");
    }

    @Test
    void shouldPreserveCause() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.INTERNAL_SERVER_ERROR;
        Throwable cause = new IllegalStateException("Database connection failed");

        // When
        TestCartisanException exception = new TestCartisanException(codeMessage, cause);

        // Then
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("Database connection failed");
    }

    @Test
    void shouldPreserveCauseWithFormattedMessage() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.INVALID_PARAMETER;
        Throwable cause = new IllegalArgumentException("Invalid email format");

        // When
        TestCartisanException exception = new TestCartisanException(codeMessage, cause, "email");

        // Then
        assertThat(exception.getMessage()).isEqualTo("Invalid parameter: email");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldBeRuntimeException() {
        // Given
        CodeMessage codeMessage = BaseCodeMessage.NOT_FOUND;

        // When
        TestCartisanException exception = new TestCartisanException(codeMessage);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldThrowNPE_whenCodeMessageIsNull() {
        // Given & When & Then
        assertThatThrownBy(() -> new TestCartisanException(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("codeMessage");
    }
}
