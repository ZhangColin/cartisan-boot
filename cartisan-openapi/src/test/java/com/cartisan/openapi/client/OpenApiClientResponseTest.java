package com.cartisan.openapi.client;

import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.cartisan.openapi.signature.HmacSha256SignatureCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenApiClientResponseTest {

    @Test
    void shouldThrowException_whenStatusCodeIs4xx() {
        OpenApiClient client = createClient();

        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn("{\"error\":\"bad request\"}");

        assertThatThrownBy(() -> invokeValidateResponse(client, response))
                .isInstanceOf(OpenApiClientException.class)
                .satisfies(ex -> {
                    OpenApiClientException oce = (OpenApiClientException) ex;
                    assertThat(oce.getStatusCode()).isEqualTo(400);
                    assertThat(oce.getBody()).isEqualTo("{\"error\":\"bad request\"}");
                });
    }

    @Test
    void shouldThrowException_whenStatusCodeIs5xx() {
        OpenApiClient client = createClient();

        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn("internal server error");

        assertThatThrownBy(() -> invokeValidateResponse(client, response))
                .isInstanceOf(OpenApiClientException.class)
                .satisfies(ex -> {
                    OpenApiClientException oce = (OpenApiClientException) ex;
                    assertThat(oce.getStatusCode()).isEqualTo(500);
                    assertThat(oce.getBody()).isEqualTo("internal server error");
                });
    }

    @Test
    void shouldNotThrow_whenStatusCodeIs2xx() {
        OpenApiClient client = createClient();

        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"result\":\"ok\"}");

        assertThatCode(() -> invokeValidateResponse(client, response))
                .doesNotThrowAnyException();
    }

    private OpenApiClient createClient() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setAppId("test-app");
        props.getSelf().setAppSecret("test-secret");
        return new OpenApiClient(props, new HmacSha256SignatureCalculator(), new ObjectMapper());
    }

    private void invokeValidateResponse(OpenApiClient client, HttpResponse<String> response) {
        try {
            Method method = OpenApiClient.class.getDeclaredMethod("validateResponse", HttpResponse.class);
            method.setAccessible(true);
            method.invoke(client, response);
        } catch (Exception e) {
            if (e.getCause() != null) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }
}
