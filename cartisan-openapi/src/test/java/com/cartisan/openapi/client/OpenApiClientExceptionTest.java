package com.cartisan.openapi.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiClientExceptionTest {

    @Test
    void shouldCreateException_withStatusCodeAndBody() {
        OpenApiClientException ex = new OpenApiClientException(400, "{\"error\":\"bad request\"}");

        assertThat(ex.getStatusCode()).isEqualTo(400);
        assertThat(ex.getBody()).isEqualTo("{\"error\":\"bad request\"}");
    }

    @Test
    void shouldTruncateLongBody_inGetMessage() {
        String longBody = "x".repeat(300);
        OpenApiClientException ex = new OpenApiClientException(500, longBody);

        String message = ex.getMessage();

        assertThat(message).contains("500");
        assertThat(message).hasSizeLessThan(300 + 20); // body truncated + prefix text
        assertThat(message).contains("x".repeat(200));
        assertThat(message).contains("...");
    }
}
