package com.cartisan.web.response;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    /**
     * 需要排除的路径前缀。
     */
    private static final String[] EXCLUDE_PATHS = {
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator"
    };

    public AutoResponseAdvice(AutoResponseConfiguration configuration, ObjectMapper objectMapper) {
        this.configuration = configuration;
        this.objectMapper = objectMapper;
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
        // 排除特定路径
        String path = request.getURI().getPath();
        if (shouldExclude(path)) {
            return body;
        }

        // 避免重复包装 ApiResponse
        if (body instanceof ApiResponse) {
            return body;
        }

        // 包装为 ApiResponse
        ApiResponse<Object> apiResponse = ApiResponse.ok(body);

        // String 返回类型需要特殊处理，否则会被再次序列化
        // 需要检查返回类型而不是 body 实例类型，因为 body 可能是 null
        if (returnType.getParameterType().equals(String.class)) {
            try {
                return objectMapper.writeValueAsString(apiResponse);
            } catch (Exception e) {
                return body;
            }
        }

        return apiResponse;
    }

    /**
     * 判断路径是否应该被排除。
     *
     * @param path 请求路径
     * @return true 表示排除，false 表示不排除
     */
    private boolean shouldExclude(String path) {
        if (path == null) {
            return false;
        }
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.startsWith(excludePath)) {
                return true;
            }
        }
        return false;
    }
}
