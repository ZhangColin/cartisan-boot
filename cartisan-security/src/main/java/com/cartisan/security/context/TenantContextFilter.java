package com.cartisan.security.context;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 租户上下文过滤器。
 * <p>
 * 从 HTTP 请求中解析租户 ID 并绑定到 {@link TenantContext}。
 * 解析优先级：Header（X-Tenant-Id） > Sa-Token Session。
 * </p>
 *
 * <h3>解析规则</h3>
 * <ol>
 *   <li>优先读取 {@code X-Tenant-Id} Header，若格式错误则记录 WARN 日志并忽略</li>
 *   <li>若 Header 不存在或无效，且用户已登录，从 Sa-Token Session 读取 {@code tenantId}</li>
 *   <li>都不存在时，{@link TenantContext#getCurrentTenantId()} 返回 {@code null}</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 在业务代码中获取当前租户
 * Long tenantId = TenantContext.getCurrentTenantId();
 * if (tenantId != null) {
 *     // 使用 tenantId 进行租户级数据隔离
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
@Component("cartisanTenantContextFilter")
public final class TenantContextFilter implements Filter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String TENANT_ID_SESSION_KEY = "tenantId";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 无初始化逻辑
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 解析租户 ID
        Long tenantId = resolveTenantId(httpRequest);

        // 使用 ScopedValue 绑定租户上下文，执行后续过滤器
        TenantContext.runWithTenant(tenantId, () -> {
            try {
                chain.doFilter(request, response);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        });
        // 作用域结束，租户上下文自动清理
    }

    @Override
    public void destroy() {
        // 无销毁逻辑
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    /**
     * 解析租户 ID，按 Header > Session 优先级。
     *
     * @param request HTTP 请求
     * @return 租户 ID，不存在时返回 null
     */
    private Long resolveTenantId(HttpServletRequest request) {
        // 1. 优先从 Header 解析
        Long tenantId = parseTenantIdFromHeader(request);
        if (tenantId != null) {
            return tenantId;
        }

        // 2. Header 不存在或无效，从 Session 读取
        return parseTenantIdFromSession();
    }

    /**
     * 从 X-Tenant-Id Header 解析租户 ID。
     *
     * @param request HTTP 请求
     * @return 租户 ID，Header 不存在或格式错误时返回 null
     */
    private Long parseTenantIdFromHeader(HttpServletRequest request) {
        String headerValue = request.getHeader(TENANT_ID_HEADER);

        // 空白检查
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        // 解析数字
        try {
            Long tenantId = Long.parseLong(headerValue.trim());
            log.debug("Resolved tenantId: {} from header", tenantId);
            return tenantId;
        } catch (NumberFormatException e) {
            log.warn("Invalid X-Tenant-Id header value: {}", headerValue);
            return null;
        }
    }

    /**
     * 从 Sa-Token Session 读取租户 ID。
     * <p>
     * 支持两种模式：
     * <ol>
     *   <li>正常模式：Sa-Token 上下文已初始化，直接通过 StpUtil 获取</li>
     *   <li>测试模式：上下文未初始化，从请求头读取 token 后手动查询 Session</li>
     * </ol>
     *
     * @return 租户 ID，用户未登录、Session 无租户信息或 Sa-Token 未初始化时返回 null
     */
    private Long parseTenantIdFromSession() {
        try {
            // 优先尝试正常模式（Sa-Token 上下文已初始化）
            if (StpUtil.isLogin()) {
                Object sessionTenantId = StpUtil.getSession().get(TENANT_ID_SESSION_KEY);
                if (sessionTenantId != null) {
                    try {
                        Long tenantId = Long.parseLong(sessionTenantId.toString());
                        log.debug("Resolved tenantId: {} from session (context mode)", tenantId);
                        return tenantId;
                    } catch (NumberFormatException e) {
                        log.warn("Invalid tenantId in session: {}", sessionTenantId);
                    }
                }
                return null;
            }
        } catch (cn.dev33.satoken.exception.SaTokenContextException e) {
            // 上下文未初始化，继续尝试测试模式
            log.trace("SaToken context not initialized, trying token-based lookup");
        }

        // 测试模式：从当前请求读取 token 并手动查询 Session
        // 这处理 MockMvc 测试场景，SaServletFilter 未执行的情况
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return null;
            }

            String token = extractSaToken(request);
            if (token == null || token.isBlank()) {
                return null;
            }

            // 通过 token 直接获取 loginId（不依赖已初始化的上下文）
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return null;
            }

            // 通过 loginId 获取 Session 并读取 tenantId
            Object sessionTenantId = StpUtil.getSessionByLoginId(loginId).get(TENANT_ID_SESSION_KEY);
            if (sessionTenantId == null) {
                return null;
            }

            Long tenantId = Long.parseLong(sessionTenantId.toString());
            log.debug("Resolved tenantId: {} from session (token mode)", tenantId);
            return tenantId;

        } catch (cn.dev33.satoken.exception.NotLoginException e) {
            log.trace("Token invalid or session expired: {}", e.getMessage());
            return null;
        } catch (NumberFormatException e) {
            log.warn("Invalid tenantId in session: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Unexpected error reading tenant from session: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前 HTTP 请求。
     * <p>
     * 使用 RequestContextHolder 获取当前线程绑定的请求。
     * </p>
     *
     * @return 当前请求，无法获取时返回 null
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            org.springframework.web.context.request.RequestAttributes attrs =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                return sra.getRequest();
            }
        } catch (Exception e) {
            log.trace("Unable to get current request from RequestContextHolder: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从请求中提取 Sa-Token。
     * <p>
     * 按优先级从 header 或 parameter 中提取。
     * </p>
     *
     * @param request HTTP 请求
     * @return token 值，不存在时返回 null
     */
    private String extractSaToken(HttpServletRequest request) {
        // 1. 从 header 读取
        String token = request.getHeader("satoken");
        if (token != null && !token.isBlank()) {
            return token;
        }

        // 2. 从 parameter 读取
        token = request.getParameter("satoken");
        if (token != null && !token.isBlank()) {
            return token;
        }

        return null;
    }
}
