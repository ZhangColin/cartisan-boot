package com.cartisan.security.config;

import com.cartisan.security.annotation.CurrentUserMethodArgumentResolver;
import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动配置集成测试，验证所有组件正确装配。
 */
@SpringBootTest(classes = {
        SecurityInterceptor.class,
        SecurityInterceptorConfig.class,
        CurrentUserMethodArgumentResolver.class,
        CurrentUserArgumentResolverConfig.class,
        CartisanSecurityAutoConfiguration.class
})
class CartisanSecurityAutoConfigurationTest {

    @Autowired(required = false)
    private CartisanSecurityProperties properties;

    @Autowired(required = false)
    private SecurityInterceptor securityInterceptor;

    @Autowired(required = false)
    private CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void given_context_when_getProperties_then_loadedWithDefaults() {
        assertThat(properties).isNotNull();
        assertThat(properties.getPathPatterns()).containsExactly("/**");
        assertThat(properties.getExcludePathPatterns()).containsExactly("/error", "/actuator/**");
    }

    @Test
    void given_context_when_getSecurityInterceptor_then_exists() {
        assertThat(securityInterceptor).isNotNull();
    }

    @Test
    void given_context_when_getSecurityInterceptorConfig_then_exists() {
        SecurityInterceptorConfig config = applicationContext.getBean(SecurityInterceptorConfig.class);
        assertThat(config).isNotNull();
    }

    @Test
    void given_context_when_getCurrentUserMethodArgumentResolver_then_exists() {
        assertThat(currentUserMethodArgumentResolver).isNotNull();
    }

    @Test
    void given_context_when_getCurrentUserArgumentResolverConfig_then_exists() {
        CurrentUserArgumentResolverConfig config = applicationContext.getBean(CurrentUserArgumentResolverConfig.class);
        assertThat(config).isNotNull();
    }
}
