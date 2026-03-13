package com.cartisan.core.util;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.CodeMessage;
import com.cartisan.core.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Assertions.require 方法测试。
 */
class AssertionsRequireTest {

    @Test
    void should_pass_silently_when_condition_is_true() {
        // Given
        boolean trueCondition = true;
        CodeMessage codeMessage = BaseCodeMessage.CONFLICT;

        // When - 静默通过，不抛出异常
        Assertions.require(trueCondition, codeMessage);

        // Then - 无异常
        assertThat(true).isTrue(); // 占位断言，表示"到达这里"
    }

    @Test
    void should_throw_DomainException_when_condition_is_false() {
        // Given
        boolean falseCondition = false;
        CodeMessage codeMessage = BaseCodeMessage.CONFLICT;

        // When & Then
        assertThatThrownBy(() -> Assertions.require(falseCondition, codeMessage))
                .isInstanceOf(DomainException.class)
                .hasMessage("Resource conflict");
    }

    @Test
    void should_include_codeMessage_in_exception() {
        // Given
        boolean falseCondition = false;
        CodeMessage codeMessage = BaseCodeMessage.INVALID_PARAMETER;
        String parameterName = "email";

        // When & Then
        assertThatThrownBy(() -> Assertions.require(falseCondition, codeMessage, parameterName))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException domainEx = (DomainException) ex;
                    assertThat(domainEx.getCodeMessage()).isSameAs(codeMessage);
                });
    }

    @Test
    void should_throw_exception_when_attempting_instantiation_via_reflection() throws Exception {
        // Given
        java.lang.reflect.Constructor<Assertions> constructor =
                Assertions.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When & Then - 尝试通过反射实例化应该抛出异常
        assertThatThrownBy(constructor::newInstance)
                .hasCauseExactlyInstanceOf(UnsupportedOperationException.class)
                .satisfies(ex -> {
                    Throwable cause = ex.getCause();
                    assertThat(cause).isInstanceOf(UnsupportedOperationException.class);
                    assertThat(cause.getMessage()).contains("Utility class cannot be instantiated");
                });
    }
}
