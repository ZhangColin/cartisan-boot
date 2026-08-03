package com.cartisan.openapi.client;

import com.cartisan.core.context.RequestContext;
import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.cartisan.openapi.signature.HmacSha256SignatureCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiClientHeaderTest {

    @Test
    void shouldIncludeSignatureHeaders() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");

        OpenApiClient client = new OpenApiClient(props,
                new HmacSha256SignatureCalculator(), new ObjectMapper());

        // Use reflection to call buildHeaders
        Map<String, String> headers = invokeBuildHeaders(client, new byte[0], null);

        assertThat(headers).containsKey("X-Api-Key");
        assertThat(headers).containsKey("X-Timestamp");
        assertThat(headers).containsKey("X-Nonce");
        assertThat(headers).containsKey("X-Body-Digest");
        assertThat(headers).containsKey("X-Sign");
        assertThat(headers.get("X-Api-Key")).isEqualTo("test-app");
    }

    @Test
    void shouldIncludeContextHeaders_whenRequestContextBound() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");

        OpenApiClient client = new OpenApiClient(props,
                new HmacSha256SignatureCalculator(), new ObjectMapper());

        RequestContext ctx = new RequestContext(
                "req-1", "10.0.0.1",
                "caller-key", "CallerApp",   // caller info present — must NOT leak
                42L, "Alice",
                100L, "TenantX");

        RequestContext.run(ctx, () -> {
            Map<String, String> headers = invokeBuildHeaders(client, new byte[0], null);

            assertThat(headers.get("X-Request-Id")).isEqualTo("req-1");
            assertThat(headers.get("X-Client-Ip")).isEqualTo("10.0.0.1");
            assertThat(headers.get("X-User-Id")).isEqualTo("42");
            assertThat(headers.get("X-User-Name")).isEqualTo("Alice");
            assertThat(headers.get("X-Tenant-Id")).isEqualTo("100");
            assertThat(headers.get("X-Tenant-Name")).isEqualTo("TenantX");
            // Should NOT propagate caller identity — downstream must verify its own
            assertThat(headers).doesNotContainValue("caller-key");
            assertThat(headers).doesNotContainValue("CallerApp");
        });
    }

    @Test
    void shouldNotIncludeContextHeaders_whenNoRequestContext() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");

        OpenApiClient client = new OpenApiClient(props,
                new HmacSha256SignatureCalculator(), new ObjectMapper());

        Map<String, String> headers = invokeBuildHeaders(client, new byte[0], null);

        assertThat(headers).doesNotContainKey("X-Request-Id");
        assertThat(headers).doesNotContainKey("X-User-Id");
    }

    @Test
    void shouldGenerateDifferentSign_whenUrlHasQueryParams() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");

        OpenApiClient client = new OpenApiClient(props,
                new HmacSha256SignatureCalculator(), new ObjectMapper());

        Map<String, String> headersWithParams = invokeBuildHeaders(client, new byte[0], Map.of("key", "value"));
        Map<String, String> headersWithoutParams = invokeBuildHeaders(client, new byte[0], null);

        assertThat(headersWithParams.get("X-Sign")).isNotEqualTo(headersWithoutParams.get("X-Sign"));
    }

    @Test
    void shouldHandleEmptyQueryString() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");

        OpenApiClient client = new OpenApiClient(props,
                new HmacSha256SignatureCalculator(), new ObjectMapper());

        Map<String, String> params = invokeExtractQueryParams(client, "");

        assertThat(params).isEmpty();
    }

    @Test
    void shouldHandleNullQueryParams() {
        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("test-app");
        props.getSelf().setApiSecret("test-secret");

        OpenApiClient client = new OpenApiClient(props,
                new HmacSha256SignatureCalculator(), new ObjectMapper());

        Map<String, String> params = invokeExtractQueryParams(client, null);

        assertThat(params).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> invokeBuildHeaders(OpenApiClient client, byte[] body, Map<String, String> queryParams) {
        try {
            var method = OpenApiClient.class.getDeclaredMethod("buildHeaders", String.class, byte[].class, Map.class);
            method.setAccessible(true);
            return (Map<String, String>) method.invoke(client, "POST", body, queryParams);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> invokeExtractQueryParams(OpenApiClient client, String query) {
        try {
            var method = OpenApiClient.class.getDeclaredMethod("extractQueryParams", String.class);
            method.setAccessible(true);
            return (Map<String, String>) method.invoke(client, query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
