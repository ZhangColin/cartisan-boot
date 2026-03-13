package com.cartisan.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CodeMessage 接口测试。
 *
 * <p>验证 CodeMessage 接口定义了 code()、message()、httpStatus() 方法。</p>
 */
class CodeMessageTest {

    /**
     * 测试用 CodeMessage 实现 - 使用枚举。
     */
    private enum TestCodeMessage implements CodeMessage {
        TEST_ERROR(404, "TEST_ERROR", "Test resource not found: {0}");

        private final int httpStatus;
        private final String code;
        private final String message;

        TestCodeMessage(int httpStatus, String code, String message) {
            this.httpStatus = httpStatus;
            this.code = code;
            this.message = message;
        }

        @Override
        public String code() {
            return this.code;
        }

        @Override
        public String message() {
            return this.message;
        }

        @Override
        public int httpStatus() {
            return this.httpStatus;
        }
    }

    @Test
    void shouldReturnCode_whenCodeMethodCalled() {
        // Given
        CodeMessage codeMessage = TestCodeMessage.TEST_ERROR;

        // When
        String code = codeMessage.code();

        // Then
        assertThat(code).isEqualTo("TEST_ERROR");
    }

    @Test
    void shouldReturnMessage_whenMessageMethodCalled() {
        // Given
        CodeMessage codeMessage = TestCodeMessage.TEST_ERROR;

        // When
        String message = codeMessage.message();

        // Then
        assertThat(message).isEqualTo("Test resource not found: {0}");
    }

    @Test
    void shouldReturnHttpStatus_whenHttpStatusMethodCalled() {
        // Given
        CodeMessage codeMessage = TestCodeMessage.TEST_ERROR;

        // When
        int httpStatus = codeMessage.httpStatus();

        // Then
        assertThat(httpStatus).isEqualTo(404);
    }

    @Test
    void shouldSupportPlaceholder_inMessage() {
        // Given
        CodeMessage codeMessage = TestCodeMessage.TEST_ERROR;

        // When
        String message = codeMessage.message();

        // Then - MessageFormat 占位符格式为 {0}
        assertThat(message).contains("{0}");
    }
}
