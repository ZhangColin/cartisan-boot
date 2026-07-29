package com.cartisan.openapi.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyInfoTest {

    @Test
    void shouldReturnTrue_whenStatusIsActive() {
        ApiKeyInfo info = new ApiKeyInfo("app1", "App1", "secret", "ACTIVE");
        assertThat(info.isActive()).isTrue();
    }

    @Test
    void shouldReturnFalse_whenStatusIsInactive() {
        ApiKeyInfo info = new ApiKeyInfo("app1", "App1", "secret", "DISABLED");
        assertThat(info.isActive()).isFalse();
    }

    @Test
    void shouldIgnoreCase_whenCheckStatus() {
        // status 校验对大小写不敏感（"ACTIVE" / "active" 均视为激活）
        assertThat(new ApiKeyInfo("app1", "App1", "secret", "active").isActive()).isTrue();
        assertThat(new ApiKeyInfo("app1", "App1", "secret", "Active").isActive()).isTrue();
    }
}
