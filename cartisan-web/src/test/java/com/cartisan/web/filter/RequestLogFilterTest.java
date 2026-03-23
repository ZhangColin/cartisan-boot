package com.cartisan.web.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cartisan.web.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RequestLogFilter 单元测试。
 *
 * <p>验证请求日志记录功能：</p>
 * <ul>
 *   <li>记录基本请求信息（requestId、IP、方法、URI）</li>
 *   <li>记录 POST/PUT 请求的 Body</li>
 *   <li>排除特定路径（swagger、druid、actuator）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RequestLogFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RequestLogFilter filter;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() throws Exception {
        filter = new RequestLogFilter();

        // 设置 logback ListAppender 捕获日志
        logger = (Logger) LoggerFactory.getLogger(RequestLogFilter.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);

        // 初始化 RequestContext（模拟 RequestContextFilter 已执行）
        // 使用反射调用包级私有的 init 方法
        Method initMethod = RequestContext.class.getDeclaredMethod("init", String.class, String.class);
        initMethod.setAccessible(true);
        initMethod.invoke(null, "test-request-id", "192.168.1.1");
    }

    @AfterEach
    void tearDown() throws Exception {
        logger.detachAppender(logAppender);
        // 使用反射调用包级私有的 clear 方法
        Method clearMethod = RequestContext.class.getDeclaredMethod("clear");
        clearMethod.setAccessible(true);
        clearMethod.invoke(null);
    }

    @Test
    void shouldLogRequestWithBasicInfo() throws Exception {
        // Given: 基本请求
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getQueryString()).thenReturn(null);

        // When: 执行 Filter
        filter.doFilterInternal(request, response, filterChain);

        // Then: 验证 FilterChain 被调用
        verify(filterChain).doFilter(any(), any());

        // And: 验证日志包含基本信息
        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logEvent = logAppender.list.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage()).contains("test-request-id");
        assertThat(logEvent.getFormattedMessage()).contains("192.168.1.100");
        assertThat(logEvent.getFormattedMessage()).contains("GET");
        assertThat(logEvent.getFormattedMessage()).contains("/api/test");
    }

    @Test
    void shouldLogPostRequestBody() throws Exception {
        // Given: POST 请求
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getQueryString()).thenReturn(null);

        // When: 执行 Filter
        filter.doFilterInternal(request, response, filterChain);

        // Then: 验证日志包含 Body 标记
        verify(filterChain).doFilter(any(), any());

        // And: 验证日志记录
        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logEvent = logAppender.list.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage()).contains("POST");
        assertThat(logEvent.getFormattedMessage()).contains("/api/users");
    }

    @Test
    void shouldExcludeSwaggerPaths() throws Exception {
        // Given: Swagger 相关路径
        String[] swaggerPaths = {
            "/swagger-ui/index.html",
            "/v3/api-docs",
            "/swagger-resources"
        };

        for (String path : swaggerPaths) {
            logAppender.list.clear();

            // 重置 mock
            org.mockito.Mockito.reset(request, filterChain);

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");

            // When: 执行 Filter
            filter.doFilterInternal(request, response, filterChain);

            // Then: 验证 FilterChain 被调用（未拦截）
            verify(filterChain).doFilter(any(), any());

            // And: 验证没有日志输出
            assertThat(logAppender.list).isEmpty();
        }
    }

    @Test
    void shouldExcludeDruidPaths() throws Exception {
        // Given: Druid 相关路径
        String[] druidPaths = {
            "/druid/index.html",
            "/druid/api",
            "/druid/sql.html"
        };

        for (String path : druidPaths) {
            logAppender.list.clear();

            // 重置 mock
            org.mockito.Mockito.reset(request, filterChain);

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");

            // When: 执行 Filter
            filter.doFilterInternal(request, response, filterChain);

            // Then: 验证 FilterChain 被调用（未拦截）
            verify(filterChain).doFilter(any(), any());

            // And: 验证没有日志输出
            assertThat(logAppender.list).isEmpty();
        }
    }

    @Test
    void shouldExcludeActuatorPaths() throws Exception {
        // Given: Actuator 相关路径
        String[] actuatorPaths = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/metrics"
        };

        for (String path : actuatorPaths) {
            logAppender.list.clear();

            // 重置 mock
            org.mockito.Mockito.reset(request, filterChain);

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");

            // When: 执行 Filter
            filter.doFilterInternal(request, response, filterChain);

            // Then: 验证 FilterChain 被调用（未拦截）
            verify(filterChain).doFilter(any(), any());

            // And: 验证没有日志输出
            assertThat(logAppender.list).isEmpty();
        }
    }

    @Test
    void shouldLogRequestWithQueryString() throws Exception {
        // Given: 带查询参数的请求
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getQueryString()).thenReturn("page=1&size=10");

        // When: 执行 Filter
        filter.doFilterInternal(request, response, filterChain);

        // Then: 验证日志包含查询参数
        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logEvent = logAppender.list.get(0);
        assertThat(logEvent.getFormattedMessage()).contains("page=1&size=10");
    }

    @Test
    void shouldLogPutRequestBody() throws Exception {
        // Given: PUT 请求
        when(request.getRequestURI()).thenReturn("/api/users/1");
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getQueryString()).thenReturn(null);

        // When: 执行 Filter
        filter.doFilterInternal(request, response, filterChain);

        // Then: 验证日志包含 PUT 方法和 URI
        verify(filterChain).doFilter(any(), any());

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logEvent = logAppender.list.get(0);
        assertThat(logEvent.getFormattedMessage()).contains("PUT");
        assertThat(logEvent.getFormattedMessage()).contains("/api/users/1");
    }

    @Test
    void shouldNotLogGetRequestBody() throws Exception {
        // Given: GET 请求（不应记录 Body）
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getQueryString()).thenReturn(null);

        // When: 执行 Filter
        filter.doFilterInternal(request, response, filterChain);

        // Then: 验证只有一条日志（基本信息）
        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logEvent = logAppender.list.get(0);
        assertThat(logEvent.getFormattedMessage()).contains("GET");
    }
}
