package com.cartisan.openapi.provider;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyInfoTest {

    @Test
    void shouldReturnTrue_whenStatusIsActive() {
        ApiKeyInfo info = new ApiKeyInfo("app1", "App1", "secret", Set.of("read"), "ACTIVE");
        assertThat(info.isActive()).isTrue();
    }

    @Test
    void shouldReturnFalse_whenStatusIsInactive() {
        ApiKeyInfo info = new ApiKeyInfo("app1", "App1", "secret", Set.of("read"), "DISABLED");
        assertThat(info.isActive()).isFalse();
    }

    @Test
    void shouldCheckPermission() {
        ApiKeyInfo info = new ApiKeyInfo("app1", "App1", "secret", Set.of("payment:create", "order:read"), "ACTIVE");

        assertThat(info.hasPermission("payment:create")).isTrue();
        assertThat(info.hasPermission("admin:delete")).isFalse();
    }

    @Test
    void shouldHandleNullPermissions() {
        ApiKeyInfo info = new ApiKeyInfo("app1", "App1", "secret", null, "ACTIVE");
        assertThat(info.hasPermission("any")).isFalse();
    }
}
