package com.cartisan.security.config;

import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartisanSecurityPropertiesTest {

    @Test
    void given_newInstance_when_getPathPatterns_then_returnDefault() {
        CartisanSecurityProperties properties = new CartisanSecurityProperties();

        assertThat(properties.getPathPatterns()).containsExactly("/**");
    }

    @Test
    void given_newInstance_when_getExcludePathPatterns_then_returnDefault() {
        CartisanSecurityProperties properties = new CartisanSecurityProperties();

        assertThat(properties.getExcludePathPatterns()).containsExactly("/error", "/actuator/**");
    }

    @Test
    void given_setPathPatterns_when_getPathPatterns_then_returnCustom() {
        CartisanSecurityProperties properties = new CartisanSecurityProperties();
        properties.setPathPatterns(List.of("/api/**", "/admin/**"));

        assertThat(properties.getPathPatterns()).containsExactly("/api/**", "/admin/**");
    }

    @Test
    void given_setExcludePathPatterns_when_getExcludePathPatterns_then_returnCustom() {
        CartisanSecurityProperties properties = new CartisanSecurityProperties();
        properties.setExcludePathPatterns(List.of("/api/public/**"));

        assertThat(properties.getExcludePathPatterns()).containsExactly("/api/public/**");
    }
}
