package com.cartisan.security.config;

import com.cartisan.security.config.properties.CartisanSecurityProperties;
import com.cartisan.security.permission.PermissionScanner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {CartisanSecurityAutoConfiguration.class, CartisanSecurityAutoConfigurationTest.TestConfig.class})
class CartisanSecurityAutoConfigurationTest {

    @Autowired(required = false)
    private CartisanSecurityProperties properties;

    @Autowired(required = false)
    private SecurityInterceptor securityInterceptor;

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
    void given_autoConfig_when_contextLoads_then_securityFilterBeanExists() {
        assertThat(applicationContext.getBean("securityFilter")).isNotNull();
    }

    @Test
    void given_autoConfig_when_contextLoads_then_tenantFilterBeanExists() {
        assertThat(applicationContext.getBean("tenantFilter")).isNotNull();
    }

    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
    static class TestConfig {
    }
}
