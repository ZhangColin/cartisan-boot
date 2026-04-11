package com.cartisan.security.context;

import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.core.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户上下文 Filter，从 Header 或 Sa-Token Session 读取租户信息并写入 RequestContext。
 *
 * <p>执行顺序：HIGHEST_PRECEDENCE + 10（在 SecurityFilter 之后）</p>
 *
 * <p>解析优先级：X-Tenant-Id Header > Sa-Token Session > null</p>
 */
public class TenantFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String TENANT_NAME_HEADER = "X-Tenant-Name";
    private static final String TENANT_ID_SESSION_KEY = "tenantId";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Long tenantId = resolveTenantId(request);
        String tenantName = request.getHeader(TENANT_NAME_HEADER);

        if (tenantId != null) {
            RequestContext current = RequestContext.CONTEXT.orElse(null);
            RequestContext enriched = current != null
                    ? current.withTenant(tenantId, tenantName)
                    : new RequestContext(null, null, null, null, null, null, tenantId, tenantName);

            RequestContext.run(enriched, () -> {
                try {
                    filterChain.doFilter(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private Long resolveTenantId(HttpServletRequest request) {
        // 1. Header
        Long tenantId = parseTenantIdFromHeader(request);
        if (tenantId != null) {
            return tenantId;
        }

        // 2. Session
        return parseTenantIdFromSession();
    }

    private Long parseTenantIdFromHeader(HttpServletRequest request) {
        String headerValue = request.getHeader(TENANT_ID_HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(headerValue.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid X-Tenant-Id header value: {}", headerValue);
            return null;
        }
    }

    private Long parseTenantIdFromSession() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            Object sessionTenantId = StpUtil.getSession().get(TENANT_ID_SESSION_KEY);
            if (sessionTenantId == null) {
                return null;
            }
            return Long.parseLong(sessionTenantId.toString());
        } catch (NumberFormatException e) {
            log.warn("Invalid tenantId in session: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Unexpected error reading tenant from session: {}", e.getMessage());
            return null;
        }
    }
}
