package com.cartisan.openapi.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyInfoTest {

    @Test
    void shouldConstructWithThreeFields() {
        ApiKeyInfo info = new ApiKeyInfo("app1", "App1", "secret");

        assertThat(info.apiKey()).isEqualTo("app1");
        assertThat(info.appName()).isEqualTo("App1");
        assertThat(info.apiSecret()).isEqualTo("secret");
    }

    @Test
    void shouldBeEqual_whenSameFields() {
        ApiKeyInfo info1 = new ApiKeyInfo("app1", "App1", "secret");
        ApiKeyInfo info2 = new ApiKeyInfo("app1", "App1", "secret");

        assertThat(info1).isEqualTo(info2);
    }

    @Test
    void shouldNotBeEqual_whenDifferentFields() {
        ApiKeyInfo info1 = new ApiKeyInfo("app1", "App1", "secret");
        ApiKeyInfo info2 = new ApiKeyInfo("app2", "App2", "other");

        assertThat(info1).isNotEqualTo(info2);
    }
}
