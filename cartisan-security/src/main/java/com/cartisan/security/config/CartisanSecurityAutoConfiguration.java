package com.cartisan.security.config;

import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.security.authentication.AuthenticationService;
import com.cartisan.security.authentication.SaTokenAuthenticationService;
import com.cartisan.security.config.properties.CartisanSecurityProperties;
import com.cartisan.security.context.SecurityFilter;
import com.cartisan.security.context.TenantFilter;
import com.cartisan.security.permission.DefaultPermissionScanner;
import com.cartisan.security.permission.PermissionScanner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * cartisan-security 自动配置主类。
 *
 * <p>注册网关认证相关组件：SecurityFilter、TenantFilter、SecurityInterceptor。</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(StpUtil.class)
@EnableConfigurationProperties(CartisanSecurityProperties.class)
public class CartisanSecurityAutoConfiguration implements WebMvcConfigurer {

    private final ObjectProvider<SecurityInterceptor> interceptorProvider;
    private final CartisanSecurityProperties properties;

    public CartisanSecurityAutoConfiguration(
            ObjectProvider<SecurityInterceptor> interceptorProvider,
            CartisanSecurityProperties properties) {
        this.interceptorProvider = interceptorProvider;
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityInterceptor securityInterceptor() {
        return new SecurityInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        java.util.List<String> pathPatterns = properties.getPathPatterns();
        java.util.List<String> excludePathPatterns = properties.getExcludePathPatterns();

        registry.addInterceptor(interceptorProvider.getObject())
            .addPathPatterns(pathPatterns.toArray(new String[0]))
            .excludePathPatterns(excludePathPatterns.toArray(new String[0]));
    }

    /**
     * 注册 Sa-Token 请求上下文 Filter。
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
     * 注册 SecurityFilter，从 Sa-Token 读取用户身份写入 RequestContext。
     * order = HIGHEST_PRECEDENCE + 5，与 SaTokenContextFilter 同级（在其后执行）。
     */
    @Bean
    public FilterRegistrationBean<SecurityFilter> securityFilter() {
        FilterRegistrationBean<SecurityFilter> registration =
            new FilterRegistrationBean<>(new SecurityFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 6);
        return registration;
    }

    /**
     * 注册 TenantFilter，从 Header/Session 读取租户信息写入 RequestContext。
     * order = HIGHEST_PRECEDENCE + 10。
     */
    @Bean
    @ConditionalOnMissingBean(name = "tenantFilter")
    public FilterRegistrationBean<TenantFilter> tenantFilter() {
        FilterRegistrationBean<TenantFilter> registration =
            new FilterRegistrationBean<>(new TenantFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(SecurityExceptionHandler.class)
    public SecurityExceptionHandler securityExceptionHandler() {
        return new SecurityExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationService.class)
    public AuthenticationService authenticationService() {
        return new SaTokenAuthenticationService();
    }

    @Bean
    @ConditionalOnBean(RequestMappingHandlerMapping.class)
    @ConditionalOnMissingBean(PermissionScanner.class)
    public PermissionScanner permissionScanner(
        RequestMappingHandlerMapping requestMappingHandlerMapping) {
        return new DefaultPermissionScanner(requestMappingHandlerMapping);
    }
}
