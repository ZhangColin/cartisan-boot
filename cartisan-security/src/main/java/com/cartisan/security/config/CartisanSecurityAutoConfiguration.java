package com.cartisan.security.config;

import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.security.annotation.CurrentUserMethodArgumentResolver;
import com.cartisan.security.authentication.AuthenticationService;
import com.cartisan.security.authentication.SaTokenAuthenticationService;
import com.cartisan.security.config.properties.CartisanSecurityProperties;
import com.cartisan.security.context.TenantContextFilter;
import com.cartisan.security.permission.DefaultPermissionScanner;
import com.cartisan.security.permission.PermissionScanner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

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
public class CartisanSecurityAutoConfiguration implements WebMvcConfigurer {

    private final ObjectProvider<SecurityInterceptor> interceptorProvider;
    private final ObjectProvider<CurrentUserMethodArgumentResolver> resolverProvider;
    private final CartisanSecurityProperties properties;

    public CartisanSecurityAutoConfiguration(
            ObjectProvider<SecurityInterceptor> interceptorProvider,
            ObjectProvider<CurrentUserMethodArgumentResolver> resolverProvider,
            CartisanSecurityProperties properties) {
        this.interceptorProvider = interceptorProvider;
        this.resolverProvider = resolverProvider;
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
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> pathPatterns = properties.getPathPatterns();
        List<String> excludePathPatterns = properties.getExcludePathPatterns();

        registry.addInterceptor(interceptorProvider.getObject())
            .addPathPatterns(pathPatterns.toArray(new String[0]))
            .excludePathPatterns(excludePathPatterns.toArray(new String[0]));
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolverProvider.getObject());
    }

    /**
     * 注册 Sa-Token 请求上下文 Filter。
     *
     * <p>将当前 HTTP 请求绑定到 Sa-Token 上下文，使 {@code StpUtil.isLogin()} 等方法
     * 能在 Filter 和 Interceptor 中正确读取当前请求（包括从 Header/Cookie 提取 token）。
     * 必须在 {@link TenantContextFilter}（order = HIGHEST_PRECEDENCE+10）
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

    /**
     * 注册 Sa-Token 异常处理器。
     * <p>
     * {@link SecurityExceptionHandler} 上的 {@code @ControllerAdvice} 由 Spring MVC
     * 在 Bean 注册后自动识别，无需依赖组件扫描。
     */
    @Bean
    @ConditionalOnMissingBean(SecurityExceptionHandler.class)
    public SecurityExceptionHandler securityExceptionHandler() {
        return new SecurityExceptionHandler();
    }

    /**
     * 注册默认 {@link AuthenticationService} 实现。
     * <p>
     * 若业务项目已提供自定义 {@link AuthenticationService}，则此方法不执行。
     */
    @Bean
    @ConditionalOnMissingBean(AuthenticationService.class)
    public AuthenticationService authenticationService() {
        return new SaTokenAuthenticationService();
    }

    /**
     * 注册 {@link com.cartisan.security.permission.PermissionScanner} Bean。
     * <p>
     * 业务系统注入此 Bean 以扫描代码中的权限定义。
     * </p>
     */
    @Bean
    @ConditionalOnBean(RequestMappingHandlerMapping.class)
    @ConditionalOnMissingBean(PermissionScanner.class)
    public PermissionScanner permissionScanner(
        RequestMappingHandlerMapping requestMappingHandlerMapping) {
        return new DefaultPermissionScanner(requestMappingHandlerMapping);
    }

    /**
     * 注册 {@link TenantContextFilter}。
     * <p>
     * 解析 X-Tenant-Id Header 或 Sa-Token Session 中的租户 ID，绑定到 {@link com.cartisan.security.context.TenantContext}。
     * order = HIGHEST_PRECEDENCE+10，在 saTokenContextFilter（HIGHEST_PRECEDENCE+5）之后执行。
     */
    @Bean
    @ConditionalOnMissingBean(name = "tenantContextFilter")
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter() {
        FilterRegistrationBean<TenantContextFilter> registration =
            new FilterRegistrationBean<>(new TenantContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
