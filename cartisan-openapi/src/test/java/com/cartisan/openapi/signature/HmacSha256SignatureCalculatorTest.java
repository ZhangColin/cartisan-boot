package com.cartisan.openapi.signature;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSha256SignatureCalculatorTest {

    private final SignatureCalculator calculator = new HmacSha256SignatureCalculator();

    @Test
    void shouldCalculateCorrectSignature() {
        String result = calculator.calculate("appId=test&nonce=abc&timestamp=123", "my-secret");

        // Result should be a hex string
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(64); // SHA-256 hex = 64 chars
    }

    @Test
    void shouldProduceSameResultForSameInput() {
        String input = "appId=test&nonce=abc&timestamp=123";
        String secret = "my-secret";

        String result1 = calculator.calculate(input, secret);
        String result2 = calculator.calculate(input, secret);

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void shouldProduceDifferentResultForDifferentSecret() {
        String input = "appId=test&nonce=abc&timestamp=123";

        String result1 = calculator.calculate(input, "secret1");
        String result2 = calculator.calculate(input, "secret2");

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void shouldHandleEmptyString() {
        String result = calculator.calculate("", "secret");

        assertThat(result).isNotEmpty();
    }
}
