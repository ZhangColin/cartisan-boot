package com.cartisan.security.context;

import cn.dev33.satoken.session.SaSession;
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
 * 安全上下文 Filter，从 Sa-Token 读取用户身份并写入 RequestContext。
 *
 * <p>执行顺序：HIGHEST_PRECEDENCE + 5（在 RequestContextFilter 之后）</p>
 *
 * <p>职责：</p>
 * <ul>
 *   <li>若用户已登录，将 userId/userName 写入 RequestContext</li>
 *   <li>若用户未登录，RequestContext 中 userId/userName 保持为 null</li>
 * </ul>
 */
public class SecurityFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        RequestContext current = RequestContext.CONTEXT.orElse(null);

        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            SaSession session = StpUtil.getSession();
            String userName = session != null ? (String) session.get("userName") : null;
            RequestContext enriched = current != null
                    ? current.withUser(userId, userName)
                    : new RequestContext(null, null, null, null, userId, userName, null, null);

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
}
