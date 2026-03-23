package com.cartisan.web.response;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 自动响应包装 Advice。
 *
 * <p>当 {@link AutoResponseConfiguration#enabled} 为 true 时，
 * 自动将 Controller 返回值包装为 {@link ApiResponse}。</p>
 *
 * <p>此类作为 ResponseBodyAdvice 实现，由 AutoResponseConfiguration 条件注册。</p>
 */
@RestControllerAdvice
@ConditionalOnProperty(prefix = "cartisan.web.auto-response", name = "enabled", havingValue = "true", matchIfMissing = false)
public class AutoResponseAdvice implements ResponseBodyAdvice<Object> {

    private final AutoResponseConfiguration configuration;

    public AutoResponseAdvice(AutoResponseConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                           Class<? extends HttpMessageConverter<?>> converterType) {
        return configuration.isEnabled();
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                 MethodParameter returnType,
                                 MediaType selectedContentType,
                                 Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                 ServerHttpRequest request,
                                 ServerHttpResponse response) {
        // TODO: 实现响应包装逻辑（后续任务）
        return body;
    }
}
