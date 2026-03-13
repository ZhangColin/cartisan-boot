package com.cartisan.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Assertions.ensure 方法测试。
 */
class AssertionsEnsureTest {

    @Test
    void should_pass_silently_when_condition_is_true() {
        // Given
        boolean trueCondition = true;
        String message = "operation should succeed";

        // When - 静默通过，不抛出异常
        Assertions.ensure(trueCondition, message);

        // Then - 无异常
        assertThat(true).isTrue();
    }

    @Test
    void should_throw_IllegalStateException_when_condition_is_false() {
        // Given
        boolean falseCondition = false;
        String message = "invariant violated";

        // When & Then
        assertThatThrownBy(() -> Assertions.ensure(falseCondition, message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Postcondition violated")
                .hasMessageContaining("invariant violated");
    }

    @Test
    void should_include_message_prefix_in_exception() {
        // Given
        boolean falseCondition = false;
        String message = "list should contain item";

        // When & Then
        assertThatThrownBy(() -> Assertions.ensure(falseCondition, message))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(ex -> {
                    String exceptionMessage = ex.getMessage();
                    assertThat(exceptionMessage).startsWith("Postcondition violated: ");
                    assertThat(exceptionMessage).endsWith("list should contain item");
                });
    }
}
