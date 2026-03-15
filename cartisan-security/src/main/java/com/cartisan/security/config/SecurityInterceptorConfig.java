package com.cartisan.security.config;

import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC 拦截器配置，将 {@link SecurityInterceptor} 注册到拦截器链。
 * <p>
 * 仅在 {@link SecurityInterceptor} Bean 存在时生效，由自动配置类导入。
 * </p>
 */
@Configuration
@ConditionalOnBean(SecurityInterceptor.class)
public class SecurityInterceptorConfig implements WebMvcConfigurer {

    private final SecurityInterceptor securityInterceptor;
    private final CartisanSecurityProperties properties;

    /**
     * 构造器注入依赖。
     *
     * @param securityInterceptor 已有的 SecurityInterceptor Bean（由 @Component 扫描创建）
     * @param properties 配置属性
     */
    public SecurityInterceptorConfig(SecurityInterceptor securityInterceptor,
                                      CartisanSecurityProperties properties) {
        this.securityInterceptor = securityInterceptor;
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> pathPatterns = properties.getPathPatterns();
        List<String> excludePathPatterns = properties.getExcludePathPatterns();

        registry.addInterceptor(securityInterceptor)
            .addPathPatterns(pathPatterns.toArray(new String[0]))
            .excludePathPatterns(excludePathPatterns.toArray(new String[0]));
    }
}
