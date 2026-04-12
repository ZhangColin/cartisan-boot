package com.cartisan.openapi.config;

import com.cartisan.openapi.client.OpenApiClient;
import com.cartisan.openapi.filter.CachingRequestBodyFilter;
import com.cartisan.openapi.filter.SignatureVerificationFilter;
import com.cartisan.openapi.interceptor.SignatureVerificationInterceptor;
import com.cartisan.openapi.nonce.NonceRepository;
import com.cartisan.openapi.provider.ApiKeyProvider;
import com.cartisan.openapi.provider.RemoteApiKeyProvider;
import com.cartisan.openapi.signature.HmacSha256SignatureCalculator;
import com.cartisan.openapi.signature.SignatureCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * cartisan-openapi 自动配置。
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(CartisanOpenapiProperties.class)
public class CartisanOpenapiAutoConfiguration implements WebMvcConfigurer {

    private final CartisanOpenapiProperties properties;
    private final NonceRepository nonceRepository;
    private final ObjectMapper objectMapper;

    public CartisanOpenapiAutoConfiguration(CartisanOpenapiProperties properties,
                                              NonceRepository nonceRepository,
                                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.nonceRepository = nonceRepository;
        this.objectMapper = objectMapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public SignatureCalculator signatureCalculator() {
        return new HmacSha256SignatureCalculator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiKeyProvider apiKeyProvider() {
        return new RemoteApiKeyProvider(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public CachingRequestBodyFilter cachingRequestBodyFilter() {
        return new CachingRequestBodyFilter(properties);
    }

    @Bean
    public FilterRegistrationBean<CachingRequestBodyFilter> cachingRequestBodyFilterRegistration(
            CachingRequestBodyFilter filter) {
        FilterRegistrationBean<CachingRequestBodyFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public SignatureVerificationFilter signatureVerificationFilter(
            SignatureCalculator signatureCalculator,
            ApiKeyProvider apiKeyProvider) {
        return new SignatureVerificationFilter(
                signatureCalculator, apiKeyProvider, nonceRepository, properties, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<SignatureVerificationFilter> signatureVerificationFilterRegistration(
            SignatureVerificationFilter filter) {
        FilterRegistrationBean<SignatureVerificationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 15);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public SignatureVerificationInterceptor signatureVerificationInterceptor() {
        return new SignatureVerificationInterceptor(objectMapper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(signatureVerificationInterceptor())
                .addPathPatterns("/**");
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenApiClient openApiClient(SignatureCalculator signatureCalculator) {
        return new OpenApiClient(properties, signatureCalculator, objectMapper);
    }
}
