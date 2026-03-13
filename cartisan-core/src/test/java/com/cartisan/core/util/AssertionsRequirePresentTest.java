package com.cartisan.core.util;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Assertions.requirePresent 方法测试。
 */
class AssertionsRequirePresentTest {

    // ========== 快捷版测试 ==========

    @Test
    void should_return_value_when_optional_is_present() {
        // Given
        String expected = "test-value";
        Optional<String> optional = Optional.of(expected);

        // When
        String result = Assertions.requirePresent(optional);

        // Then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void should_throw_DomainException_with_NOT_FOUND_when_optional_is_empty() {
        // Given
        Optional<String> optional = Optional.empty();

        // When & Then
        assertThatThrownBy(() -> Assertions.requirePresent(optional))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException domainEx = (DomainException) ex;
                    assertThat(domainEx.getCodeMessage()).isSameAs(BaseCodeMessage.NOT_FOUND);
                    assertThat(domainEx.getCodeMessage().code()).isEqualTo("NOT_FOUND");
                });
    }

    // ========== 完整版测试 ==========

    @Test
    void should_return_value_when_optional_is_present_with_codeMessage() {
        // Given
        String expected = "test-value";
        Optional<String> optional = Optional.of(expected);
        com.cartisan.core.exception.CodeMessage customCodeMessage = BaseCodeMessage.CONFLICT;

        // When
        String result = Assertions.requirePresent(optional, customCodeMessage);

        // Then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void should_throw_DomainException_with_custom_codeMessage_when_optional_is_empty() {
        // Given
        Optional<String> optional = Optional.empty();
        com.cartisan.core.exception.CodeMessage customCodeMessage = BaseCodeMessage.UNPROCESSABLE_ENTITY;

        // When & Then
        assertThatThrownBy(() -> Assertions.requirePresent(optional, customCodeMessage))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException domainEx = (DomainException) ex;
                    assertThat(domainEx.getCodeMessage()).isSameAs(customCodeMessage);
                    assertThat(domainEx.getCodeMessage().code()).isEqualTo("UNPROCESSABLE_ENTITY");
                });
    }
}
