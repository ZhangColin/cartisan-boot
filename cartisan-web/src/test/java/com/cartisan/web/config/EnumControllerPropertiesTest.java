package com.cartisan.web.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumControllerPropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        EnumControllerProperties properties = new EnumControllerProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getPath()).isEqualTo("/api/enums");
    }

    @Test
    void shouldSetProperties() {
        EnumControllerProperties properties = new EnumControllerProperties();
        properties.setEnabled(false);
        properties.setPath("/api/v2/enums");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getPath()).isEqualTo("/api/v2/enums");
    }
}