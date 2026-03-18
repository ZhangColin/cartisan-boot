package com.cartisan.security.config;

import com.cartisan.security.annotation.CurrentUserMethodArgumentResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * {@link CurrentUserMethodArgumentResolver} 的自动配置类。
 * <p>
 * 将 Resolver 注册到 Spring MVC 参数解析器链中。
 *
 * @since 0.3.0
 */
@Configuration
@ConditionalOnClass(CurrentUserMethodArgumentResolver.class)
public class CurrentUserArgumentResolverConfig implements WebMvcConfigurer {

    private final CurrentUserMethodArgumentResolver resolver;

    /**
     * 构造器注入 Resolver Bean。
     *
     * @param resolver Resolver 实例（由 @Component 扫描创建）
     */
    public CurrentUserArgumentResolverConfig(CurrentUserMethodArgumentResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 注册 Resolver 到 MVC 容器。
     *
     * @param resolvers 参数解析器列表
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolver);
    }
}
