package com.cartisan.web.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
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
 *   <li>请求结束时清理 ThreadLocal</li>
 * </ol>
 *
 * <p>容错策略：</p>
 * <ul>
 *   <li>初始化失败时使用 null 值，请求继续</li>
 *   <li>记录 WARN 日志便于排查</li>
 * </ul>
 */
/**
 * 请求上下文初始化 Filter。
 *
 * <p>执行顺序：HIGHEST_PRECEDENCE（最早执行）</p>
 *
 * <p>职责：</p>
 * <ol>
 *   <li>requestId：优先从 X-Request-Id Header 读取，否则生成 UUID</li>
 *   <li>clientIp：按 X-Forwarded-For → X-Real-IP → RemoteAddr 优先级</li>
 *   <li>请求结束时清理 ThreadLocal</li>
 * </ol>
 *
 * <p>容错策略：</p>
 * <ul>
 *   <li>初始化失败时使用 null 值，请求继续</li>
 *   <li>记录 WARN 日志便于排查</li>
 * </ul>
 *
 * <p><strong>Bean 命名</strong>：使用 {@code cartisanRequestContextFilter} 作为 bean 名称，
 * 避免与 Spring Boot 自动配置的 {@code requestContextFilter} 冲突。</p>
 */
@Component("cartisanRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    /** X-Request-Id Header 名称 */
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    /** X-Forwarded-For Header 名称 */
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /** X-Real-IP Header 名称 */
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String requestId = extractRequestId(request);
            String clientIp = extractClientIp(request);
            RequestContext.init(requestId, clientIp);
        } catch (Exception e) {
            // 容错：初始化失败时使用 null，请求继续
            log.warn("RequestContext init failed, continuing with null values", e);
            try {
                RequestContext.init(null, null);
            } catch (Exception ex) {
                // 如果连 init(null, null) 都失败，记录日志但继续
                log.warn("Failed to initialize RequestContext with null values", ex);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 无论成功还是异常，都清理 ThreadLocal
            RequestContext.clear();
        }
    }

    /**
     * 提取 requestId。
     *
     * <p>优先从 X-Request-Id Header 读取，否则生成 UUID。</p>
     *
     * @param request HTTP 请求
     * @return requestId，不为 null
     */
    private String extractRequestId(HttpServletRequest request) {
        String header = request.getHeader(HEADER_REQUEST_ID);
        if (header != null && header.trim().length() > 0) {
            return header.trim();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * 提取 clientIp。
     *
     * <p>按 X-Forwarded-For → X-Real-IP → RemoteAddr 优先级。</p>
     *
     * @param request HTTP 请求
     * @return clientIp，可能为 null
     */
    private String extractClientIp(HttpServletRequest request) {
        // 1. 尝试 X-Forwarded-For
        String xff = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xff != null && xff.trim().length() > 0) {
            String[] ips = xff.split(",");
            if (ips.length > 0) {
                String firstIp = ips[0].trim();
                if (firstIp.length() > 0) {
                    return firstIp;
                }
            }
        }

        // 2. 尝试 X-Real-IP
        String realIp = request.getHeader(HEADER_X_REAL_IP);
        if (realIp != null && realIp.trim().length() > 0) {
            return realIp.trim();
        }

        // 3. 回退到 RemoteAddr
        return request.getRemoteAddr();
    }
}
