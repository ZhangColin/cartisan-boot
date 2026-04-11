package com.cartisan.openapi.config;

import com.cartisan.openapi.client.OpenApiClient;
import com.cartisan.openapi.filter.CachingRequestBodyFilter;
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

    public CartisanOpenapiAutoConfiguration(CartisanOpenapiProperties properties,
                                              NonceRepository nonceRepository) {
        this.properties = properties;
        this.nonceRepository = nonceRepository;
    }

    @Bean
    @ConditionalOnMissingBean
    public SignatureCalculator signatureCalculator() {
        return new HmacSha256SignatureCalculator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiKeyProvider apiKeyProvider(ObjectMapper objectMapper) {
        return new RemoteApiKeyProvider(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public CachingRequestBodyFilter cachingRequestBodyFilter() {
        return new CachingRequestBodyFilter();
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
    public SignatureVerificationInterceptor signatureVerificationInterceptor(
            SignatureCalculator signatureCalculator,
            ApiKeyProvider apiKeyProvider,
            ObjectMapper objectMapper) {
        return new SignatureVerificationInterceptor(
                signatureCalculator, apiKeyProvider, nonceRepository, properties, objectMapper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor is added manually or via bean
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenApiClient openApiClient(SignatureCalculator signatureCalculator, ObjectMapper objectMapper) {
        return new OpenApiClient(properties, signatureCalculator, objectMapper);
    }
}
