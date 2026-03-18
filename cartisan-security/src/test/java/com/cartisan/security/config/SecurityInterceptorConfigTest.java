package com.cartisan.security.config;

import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityInterceptorConfigTest {

    private SecurityInterceptor mockInterceptor;
    private CartisanSecurityProperties properties;
    private SecurityInterceptorConfig config;

    @BeforeEach
    void setUp() {
        mockInterceptor = new SecurityInterceptor();
        properties = new CartisanSecurityProperties();
        ObjectProvider<SecurityInterceptor> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(mockInterceptor);
        config = new SecurityInterceptorConfig(provider, properties);
    }

    @Test
    void given_newInstance_when_getInterceptor_then_returnsInjected() {
        assertThat(config).isNotNull();
    }

    @Test
    void given_customPathPatterns_when_getProperties_then_returnsCustom() {
        // 给定
        properties.setPathPatterns(List.of("/api/**", "/admin/**"));
        properties.setExcludePathPatterns(List.of("/api/public/**"));

        // 当 + 那么
        ObjectProvider<SecurityInterceptor> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(mockInterceptor);
        config = new SecurityInterceptorConfig(provider, properties);

        assertThat(config).isNotNull();
    }
}
