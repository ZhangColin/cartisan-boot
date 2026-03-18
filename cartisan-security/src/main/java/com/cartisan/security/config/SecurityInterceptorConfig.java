package com.cartisan.security.config;

import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC 拦截器配置，将 {@link SecurityInterceptor} 注册到拦截器链。
 * <p>
 * 同时负责声明 {@link SecurityInterceptor} Bean，不依赖 @Component 扫描。
 * 使用 {@link ObjectProvider} 注入以避免与自身 @Bean 方法产生循环依赖。
 * </p>
 */
@Configuration
public class SecurityInterceptorConfig implements WebMvcConfigurer {

    private final ObjectProvider<SecurityInterceptor> interceptorProvider;
    private final CartisanSecurityProperties properties;

    public SecurityInterceptorConfig(ObjectProvider<SecurityInterceptor> interceptorProvider,
                                     CartisanSecurityProperties properties) {
        this.interceptorProvider = interceptorProvider;
        this.properties = properties;
    }

    /**
     * 声明 {@link SecurityInterceptor} Bean。
     * <p>
     * 若业务项目已提供自定义实现，则此方法不执行（{@code @ConditionalOnMissingBean}）。
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityInterceptor securityInterceptor() {
        return new SecurityInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> pathPatterns = properties.getPathPatterns();
        List<String> excludePathPatterns = properties.getExcludePathPatterns();

        registry.addInterceptor(interceptorProvider.getObject())
            .addPathPatterns(pathPatterns.toArray(new String[0]))
            .excludePathPatterns(excludePathPatterns.toArray(new String[0]));
    }
}
