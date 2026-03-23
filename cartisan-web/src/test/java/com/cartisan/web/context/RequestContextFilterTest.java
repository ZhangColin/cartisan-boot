package com.cartisan.web.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * RequestContextFilter 单元测试。
 *
 * <p>测试命名遵循 given_*_when_*_then_* 格式</p>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RequestContextFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final RequestContextFilter filter = new RequestContextFilter();

    // 在 chain 执行期间捕获的上下文值
    private String capturedRequestId;
    private String capturedClientIp;

    @AfterEach
    void tearDown() {
        RequestContext.clear();
        MDC.clear();
        capturedRequestId = null;
        capturedClientIp = null;
    }

    // ========== requestId 提取测试 ==========

    @Test
    void given_noRequestIdHeader_when_doFilter_then_generatesUuid() throws Exception {
        // Given: 没有 X-Request-Id Header
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter，在 chain 执行期间捕获上下文
        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: 验证 requestId 为 UUID 格式
        assertThat(capturedRequestId).isNotNull();
        assertThat(capturedRequestId).matches("[a-f0-9\\-]{36}"); // UUID 格式
    }

    @Test
    void given_requestIdHeaderWithValue_when_doFilter_then_usesHeaderValue() throws Exception {
        // Given: 有 X-Request-Id Header
        when(request.getHeader("X-Request-Id")).thenReturn("trace-123");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter
        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: 使用 Header 的值
        assertThat(capturedRequestId).isEqualTo("trace-123");
    }

    @Test
    void given_requestIdHeaderWithBlank_when_doFilter_then_generatesUuid() throws Exception {
        // Given: X-Request-Id Header 为空白
        when(request.getHeader("X-Request-Id")).thenReturn("   ");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter
        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: 生成 UUID，不使用空白值
        assertThat(capturedRequestId).isNotNull();
        assertThat(capturedRequestId).matches("[a-f0-9\\-]{36}");
    }

    @Test
    void given_requestIdHeaderWithSpaces_when_doFilter_then_trimsValue() throws Exception {
        // Given: X-Request-Id Header 带前后空格
        when(request.getHeader("X-Request-Id")).thenReturn("  trace-456  ");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter
        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: 返回 trim 后的值
        assertThat(capturedRequestId).isEqualTo("trace-456");
    }

    // ========== clientIp 提取测试 ==========

    @Test
    void given_xffHeaderWithSingleIp_when_doFilter_then_returnsFirstIp() throws Exception {
        // Given: X-Forwarded-For 为单个 IP
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter
        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: 返回 XFF 的值
        assertThat(capturedClientIp).isEqualTo("192.168.1.1");
    }

    @Test
    void given_xffHeaderWithMultipleIps_when_doFilter_then_returnsFirstIp() throws Exception {
        // Given: X-Forwarded-For 为多个 IP（逗号分隔）
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8, 9.10.11.12");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter
        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: 返回第一个 IP
        assertThat(capturedClientIp).isEqualTo("1.2.3.4");
    }

    @Test
    void given_xffHeaderWithSpaces_when_doFilter_then_trimsFirstIp() throws Exception {
        // Given: X-Forwarded-For IP 带空格
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("  192.168.1.1  , 10.0.0.1");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter
        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: 返回 trim 后的第一个 IP
        assertThat(capturedClientIp).isEqualTo("192.168.1.1");
    }

    @Test
    void given_noXffHeader_when_doFilter_then_returnsRealIp() throws Exception {
        // Given: 没有 X-Forwarded-For
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter
        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: 返回 X-Real-IP 的值
        assertThat(capturedClientIp).isEqualTo("10.0.0.1");
    }

    @Test
    void given_noXffAndNoRealIpHeader_when_doFilter_then_returnsRemoteAddr() throws Exception {
        // Given: 没有代理 Header
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter
        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: 返回 RemoteAddr
        assertThat(capturedClientIp).isEqualTo("127.0.0.1");
    }

    @Test
    void given_xffHeaderIsEmpty_when_doFilter_then_returnsRealIp() throws Exception {
        // Given: X-Forwarded-For 为空字符串
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter
        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: 跳过 XFF，使用 X-Real-IP
        assertThat(capturedClientIp).isEqualTo("10.0.0.1");
    }

    // ========== 清理测试 ==========

    @Test
    void given_filterExecutes_when_chainThrowsException_then_contextStillCleared() throws Exception {
        // Given: filterChain 会抛出异常
        when(request.getHeader("X-Request-Id")).thenReturn("test-123");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        org.mockito.Mockito.doThrow(new RuntimeException("Test exception"))
                .when(filterChain).doFilter(request, response);

        // When: 执行 Filter（会抛异常）
        try {
            filter.doFilterInternal(request, response, filterChain);
        } catch (RuntimeException e) {
            // 预期的异常
        }

        // Then: RequestContext 仍被清理
        assertThat(RequestContext.getRequestId()).isNull();
    }

    @Test
    void given_filterExecutes_when_doFilter_then_contextClearedAfterChain() throws Exception {
        // Given: 正常请求
        when(request.getHeader("X-Request-Id")).thenReturn("test-123");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        // When: 执行 Filter
        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: chain 执行期间上下文有值，之后被清理
        assertThat(capturedRequestId).isEqualTo("test-123");
        assertThat(capturedClientIp).isEqualTo("192.168.1.1");
        // Filter 返回后，上下文已被清理
        assertThat(RequestContext.getRequestId()).isNull();
        assertThat(RequestContext.getClientIp()).isNull();
    }

    /**
     * 设置 FilterChain 在执行时捕获 RequestContext 的值。
     * 这样可以在 chain 执行期间（清理之前）验证上下文。
     */
    private void setupCaptureInChain() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            capturedRequestId = RequestContext.getRequestId();
            capturedClientIp = RequestContext.getClientIp();
            return null;
        }).when(filterChain).doFilter(any(), any());
    }

    // ========== MDC 集成测试 ==========

    @Test
    void shouldPutRequestIdToMDC() throws Exception {
        // Given: 有 X-Request-Id Header
        when(request.getHeader("X-Request-Id")).thenReturn("mdc-test-123");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter，在 chain 执行期间捕获 MDC 值
        setupCaptureMDCInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: MDC 中包含 requestId
        assertThat(capturedMDCRequestId).isEqualTo("mdc-test-123");
    }

    @Test
    void shouldClearMDCAfterRequest() throws Exception {
        // Given: 正常请求
        when(request.getHeader("X-Request-Id")).thenReturn("mdc-test-456");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter
        setupCaptureMDCInChain();
        filter.doFilterInternal(request, response, filterChain);

        // Then: chain 执行期间 MDC 有值，之后被清理
        assertThat(capturedMDCRequestId).isEqualTo("mdc-test-456");
        // Filter 返回后，MDC 已被清理
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void shouldAddRequestIdToResponseHeader() throws Exception {
        // Given: 没有 X-Request-Id Header（会生成 UUID）
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: 执行 Filter，捕获生成的 requestId
        String[] generatedRequestId = new String[1];
        org.mockito.Mockito.doAnswer(invocation -> {
            generatedRequestId[0] = RequestContext.getRequestId();
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        // Then: 响应头包含 X-Request-Id
        org.mockito.Mockito.verify(response).setHeader("X-Request-Id", generatedRequestId[0]);
    }

    // 用于捕获 MDC 值的变量
    private String capturedMDCRequestId;

    /**
     * 设置 FilterChain 在执行时捕获 MDC 的值。
     */
    private void setupCaptureMDCInChain() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            capturedMDCRequestId = MDC.get("requestId");
            return null;
        }).when(filterChain).doFilter(any(), any());
    }
}
