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
 * 验签拦截器（精简版）。
 *
 * <p>签名验证已迁移到 {@link com.cartisan.openapi.filter.SignatureVerificationFilter}，
 * 本拦截器仅负责注解驱动的"必须验签"闸：标了 @RequireSignature 的端点，
 * 若 request attribute 中无验签成功写入的 {@link ApiKeyInfo}，则 401。</p>
 *
 * <p>签名 = 认证，不做 per-key 权限 ACL（已移除 permissions）。</p>
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

        // Read ApiKeyInfo from request attribute (set by SignatureVerificationFilter on verify success)
        ApiKeyInfo apiKeyInfo = (ApiKeyInfo) request.getAttribute(SignatureVerificationFilter.API_KEY_INFO_ATTR);
        if (apiKeyInfo == null) {
            writeError(response, 401, "Signature required");
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
