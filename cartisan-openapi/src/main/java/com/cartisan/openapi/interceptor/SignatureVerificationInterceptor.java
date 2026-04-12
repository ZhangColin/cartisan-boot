package com.cartisan.openapi.interceptor;

import com.cartisan.openapi.annotation.NoSignature;
import com.cartisan.openapi.annotation.RequireSignature;
import com.cartisan.openapi.filter.SignatureVerificationFilter;
import com.cartisan.openapi.provider.ApiKeyInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 验签权限拦截器（精简版）。
 *
 * <p>签名验证已迁移到 {@link com.cartisan.openapi.filter.SignatureVerificationFilter}，
 * 本拦截器仅负责注解驱动的权限检查。</p>
 *
 * <p>从 request attribute 中读取 {@link ApiKeyInfo}（由 Filter 在验签成功后写入），
 * 根据 @RequireSignature 和 @NoSignature 注解决定是否放行。</p>
 */
public class SignatureVerificationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SignatureVerificationInterceptor.class);

    private final ObjectMapper objectMapper;

    public SignatureVerificationInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Check @NoSignature (exclusion)
        if (handlerMethod.getMethodAnnotation(NoSignature.class) != null) {
            return true;
        }

        // Check @RequireSignature
        RequireSignature methodAnnotation = handlerMethod.getMethodAnnotation(RequireSignature.class);
        RequireSignature classAnnotation = handlerMethod.getBeanType().getAnnotation(RequireSignature.class);
        if (methodAnnotation == null && classAnnotation == null) {
            return true;
        }

        RequireSignature effectiveAnnotation = methodAnnotation != null ? methodAnnotation : classAnnotation;

        // Read ApiKeyInfo from request attribute (set by SignatureVerificationFilter)
        ApiKeyInfo apiKeyInfo = (ApiKeyInfo) request.getAttribute(SignatureVerificationFilter.API_KEY_INFO_ATTR);
        if (apiKeyInfo == null) {
            writeError(response, 401, "Signature required");
            return false;
        }

        // Check permission
        String requiredPermission = effectiveAnnotation.permission();
        if (!requiredPermission.isEmpty() && !apiKeyInfo.hasPermission(requiredPermission)) {
            writeError(response, 403, "Permission denied");
            return false;
        }

        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        log.warn("Signature access control rejected: {}", message);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = Map.of(
                "code", status,
                "message", message,
                "success", false
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
