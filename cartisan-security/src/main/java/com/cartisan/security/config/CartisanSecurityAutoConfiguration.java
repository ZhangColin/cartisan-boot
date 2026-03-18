package com.cartisan.security.config;

import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * cartisan-security 自动配置主类。
 * <p>
 * 零配置引入 cartisan-security 模块的入口类。
 * </p>
 *
 * <h3>条件装配</h3>
 * <ul>
 *   <li>Web 应用（@ConditionalOnWebApplication）</li>
 *   <li>classpath 存在 Sa-Token（@ConditionalOnClass(StpUtil.class)）</li>
 * </ul>
 *
 * <h3>配置项</h3>
 * <pre>
 * cartisan:
 *   security:
 *     interceptor:
 *       path-patterns: ["/**"]
 *       exclude-path-patterns: ["/error", "/actuator/**"]
 * </pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(StpUtil.class)
@EnableConfigurationProperties(CartisanSecurityProperties.class)
@Import({
    SecurityInterceptorConfig.class,
    CurrentUserArgumentResolverConfig.class
})
public class CartisanSecurityAutoConfiguration {

    /**
     * 注册 Sa-Token 请求上下文 Filter。
     *
     * <p>将当前 HTTP 请求绑定到 Sa-Token 上下文，使 {@code StpUtil.isLogin()} 等方法
     * 能在 Filter 和 Interceptor 中正确读取当前请求（包括从 Header/Cookie 提取 token）。
     * 必须在 {@link com.cartisan.security.context.TenantContextFilter}（order = HIGHEST_PRECEDENCE+10）
     * 之前运行，因此 order 设为 HIGHEST_PRECEDENCE+5。</p>
     */
    @Bean
    public FilterRegistrationBean<SaTokenContextFilterForJakartaServlet> saTokenContextFilter() {
        FilterRegistrationBean<SaTokenContextFilterForJakartaServlet> registration =
            new FilterRegistrationBean<>(new SaTokenContextFilterForJakartaServlet());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return registration;
    }
}
