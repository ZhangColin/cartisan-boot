package com.cartisan.web.filter;

import com.cartisan.web.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 请求日志记录 Filter。
 *
 * <p>记录请求的基本信息便于排查问题：</p>
 * <ul>
 *   <li>requestId：从 RequestContext 获取</li>
 *   <li>clientIp：客户端 IP 地址</li>
 *   <li>method：HTTP 方法（GET、POST 等）</li>
 *   <li>uri：请求 URI</li>
 *   <li>queryString：查询参数</li>
 *   <li>body：POST/PUT 请求的 Body（可选）</li>
 * </ul>
 *
 * <p>排除路径：</p>
 * <ul>
 *   <li>swagger：/swagger-ui、/v3/api-docs、/swagger-resources</li>
 *   <li>druid：/druid</li>
 *   <li>actuator：/actuator</li>
 * </ul>
 *
 * <p><strong>注意：</strong></p>
 * <ul>
 *   <li>日志级别为 INFO</li>
 *   <li>依赖 RequestContext，需要在 RequestContextFilter 之后执行</li>
 *   <li>POST/PUT 的 Body 读取需要 ContentCachingRequestWrapper</li>
 * </ul>
 *
 * <p>前置条件：RequestContext 已初始化（由 RequestContextFilter 完成）</p>
 * <p>后置条件：请求被正常处理，日志被记录</p>
 */
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    /**
     * 排除路径前缀集合。
     * <p>
     * 使用 contains 匹配，因此 "/swagger" 可以匹配 "/swagger-ui/index.html"
     * </p>
     */
    private static final Set<String> EXCLUDE_PATHS = Set.of(
        "/swagger-ui",
        "/v3/api-docs",
        "/swagger-resources",
        "/druid",
        "/actuator"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 排除特定路径
        if (shouldExclude(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 记录请求基本信息
        String requestId = RequestContext.getRequestId();
        String clientIp = request.getRemoteAddr();
        String method = request.getMethod();
        String queryString = request.getQueryString();
        String fullPath = queryString == null ? uri : uri + "?" + queryString;

        log.info("Request: requestId={}, ip={}, method={}, uri={}",
            requestId, clientIp, method, fullPath);

        filterChain.doFilter(request, response);
    }

    /**
     * 判断请求 URI 是否应该被排除。
     *
     * <p>使用 contains 匹配，可以匹配路径的任意部分。</p>
     *
     * @param uri 请求 URI
     * @return 如果应该排除返回 true，否则返回 false
     */
    private boolean shouldExclude(String uri) {
        return EXCLUDE_PATHS.stream().anyMatch(uri::contains);
    }
}
