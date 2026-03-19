package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenUsageTest {

    @Test
    void shouldCreateTokenUsage() {
        // Given & When
        TokenUsage usage = new TokenUsage(10, 20, 30);

        // Then
        assertThat(usage.promptTokens()).isEqualTo(10);
        assertThat(usage.completionTokens()).isEqualTo(20);
        assertThat(usage.totalTokens()).isEqualTo(30);
    }

    @Test
    void shouldAllowZeroTokens() {
        // Given & When
        TokenUsage usage = new TokenUsage(0, 0, 0);

        // Then
        assertThat(usage.promptTokens()).isZero();
        assertThat(usage.completionTokens()).isZero();
        assertThat(usage.totalTokens()).isZero();
    }

    @Test
    void shouldThrowException_whenPromptTokensNegative() {
        assertThatThrownBy(() -> new TokenUsage(-1, 20, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token counts must not be negative");
    }

    @Test
    void shouldThrowException_whenCompletionTokensNegative() {
        assertThatThrownBy(() -> new TokenUsage(10, -1, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token counts must not be negative");
    }

    @Test
    void shouldThrowException_whenTotalTokensNegative() {
        assertThatThrownBy(() -> new TokenUsage(10, 20, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token counts must not be negative");
    }

    @Test
    void shouldBeEqual_whenSameValues() {
        // Given
        TokenUsage usage1 = new TokenUsage(10, 20, 30);
        TokenUsage usage2 = new TokenUsage(10, 20, 30);

        // Then
        assertThat(usage1).isEqualTo(usage2);
        assertThat(usage1.hashCode()).isEqualTo(usage2.hashCode());
    }
}
