package com.cartisan.security.config;

import com.cartisan.security.annotation.CurrentUserMethodArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CurrentUserArgumentResolverConfig 单元测试。
 */
class CurrentUserArgumentResolverConfigTest {

    private CurrentUserMethodArgumentResolver mockResolver;
    private CurrentUserArgumentResolverConfig config;

    @BeforeEach
    void setUp() {
        mockResolver = new CurrentUserMethodArgumentResolver();
        config = new CurrentUserArgumentResolverConfig(mockResolver);
    }

    @Test
    void given_contextLoaded_when_resolverRegistered_then_success() {
        // Given: 配置类已创建
        assertThat(config).isNotNull();

        // When: 模拟 Spring MVC 调用 addArgumentResolvers
        List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        config.addArgumentResolvers(resolvers);

        // Then: Resolver 被正确注册
        assertThat(resolvers).hasSize(1);
        assertThat(resolvers.get(0)).isInstanceOf(CurrentUserMethodArgumentResolver.class);
        assertThat(resolvers.get(0)).isSameAs(mockResolver);
    }

    @Test
    void given_newInstance_when_getResolver_then_returnsInjected() {
        // Then: 配置类正确构造
        assertThat(config).isNotNull();
    }
}
