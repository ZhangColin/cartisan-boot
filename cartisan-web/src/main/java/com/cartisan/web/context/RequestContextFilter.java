package com.cartisan.web.context;

import com.cartisan.core.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求上下文初始化 Filter。
 *
 * <p>执行顺序：HIGHEST_PRECEDENCE（最早执行）</p>
 *
 * <p>职责：</p>
 * <ol>
 *   <li>requestId：优先从 X-Request-Id Header 读取，否则生成 UUID</li>
 *   <li>clientIp：按 X-Forwarded-For → X-Real-IP → RemoteAddr 优先级</li>
 *   <li>跨服务传递 headers：X-User-Id, X-User-Name, X-Tenant-Id, X-Tenant-Name</li>
 *   <li>使用 ScopedValue 绑定上下文，请求结束自动清理</li>
 * </ol>
 *
 * <p><strong>Bean 命名</strong>：使用 {@code cartisanRequestContextFilter} 作为 bean 名称，
 * 避免与 Spring Boot 自动配置的 {@code requestContextFilter} 冲突。</p>
 */
public class RequestContextFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    // 跨服务传递 headers
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_NAME = "X-User-Name";
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_TENANT_NAME = "X-Tenant-Name";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestId = extractRequestId(request);
        String clientIp = extractClientIp(request);
        Long userId = parseLongHeader(request, HEADER_USER_ID);
        String userName = request.getHeader(HEADER_USER_NAME);
        Long tenantId = parseLongHeader(request, HEADER_TENANT_ID);
        String tenantName = request.getHeader(HEADER_TENANT_NAME);

        RequestContext ctx = new RequestContext(
                requestId, clientIp,
                null, null,
                userId, userName,
                tenantId, tenantName);

        // 将 requestId 放入 MDC，便于日志追踪
        MDC.put("requestId", requestId);
        // 将 requestId 添加到响应头
        response.setHeader(HEADER_REQUEST_ID, requestId);

        try {
            RequestContext.run(ctx, () -> {
                try {
                    filterChain.doFilter(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            MDC.clear();
        }
    }

    private String extractRequestId(HttpServletRequest request) {
        String header = request.getHeader(HEADER_REQUEST_ID);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xff != null && !xff.isBlank()) {
            String[] ips = xff.split(",");
            if (ips.length > 0) {
                String firstIp = ips[0].trim();
                if (!firstIp.isEmpty()) {
                    return firstIp;
                }
            }
        }

        String realIp = request.getHeader(HEADER_X_REAL_IP);
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private Long parseLongHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value != null && !value.isBlank()) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid {} header value: {}", headerName, value);
            }
        }
        return null;
    }
}
