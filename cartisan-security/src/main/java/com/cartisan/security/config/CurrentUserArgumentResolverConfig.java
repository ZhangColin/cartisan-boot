package com.cartisan.security.config;

import com.cartisan.security.annotation.CurrentUserMethodArgumentResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * {@link CurrentUserMethodArgumentResolver} 的自动配置类。
 * <p>
 * 将 Resolver 注册到 Spring MVC 参数解析器链中。
 * <p>
 * 自行声明 {@link CurrentUserMethodArgumentResolver} Bean，不依赖组件扫描，
 * 确保在任何 {@code @SpringBootApplication} 扫描范围下都能正确装配。
 * <p>
 * 使用 {@link ObjectProvider} 注入以避免与自身 {@code @Bean} 方法产生循环依赖——
 * Spring 始终能提供 Provider，无需提前创建目标 Bean。
 *
 * @since 0.3.0
 */
@Configuration
@ConditionalOnClass(CurrentUserMethodArgumentResolver.class)
public class CurrentUserArgumentResolverConfig implements WebMvcConfigurer {

    private final ObjectProvider<CurrentUserMethodArgumentResolver> resolverProvider;

    public CurrentUserArgumentResolverConfig(ObjectProvider<CurrentUserMethodArgumentResolver> resolverProvider) {
        this.resolverProvider = resolverProvider;
    }

    /**
     * 创建 {@link CurrentUserMethodArgumentResolver} Bean。
     * <p>
     * 若应用已通过组件扫描创建了该 Bean，则此方法不执行（{@code @ConditionalOnMissingBean}）。
     */
    @Bean
    @ConditionalOnMissingBean
    public CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver() {
        return new CurrentUserMethodArgumentResolver();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolverProvider.getObject());
    }
}
