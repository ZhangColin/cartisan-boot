package com.cartisan.web.context;

import com.cartisan.core.context.RequestContext;
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

    private String capturedRequestId;
    private String capturedClientIp;

    @AfterEach
    void tearDown() {
        MDC.clear();
        capturedRequestId = null;
        capturedClientIp = null;
    }

    @Test
    void shouldGenerateUuid_whenNoRequestIdHeader() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedRequestId).isNotNull();
        assertThat(capturedRequestId).matches("[a-f0-9\\-]{36}");
    }

    @Test
    void shouldUseHeader_whenRequestIdHeaderPresent() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("trace-123");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedRequestId).isEqualTo("trace-123");
    }

    @Test
    void shouldGenerateUuid_whenRequestIdHeaderBlank() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("   ");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedRequestId).isNotNull();
        assertThat(capturedRequestId).matches("[a-f0-9\\-]{36}");
    }

    @Test
    void shouldReturnFirstIp_whenXffHeaderPresent() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedClientIp).isEqualTo("1.2.3.4");
    }

    @Test
    void shouldReturnRealIp_whenNoXffHeader() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedClientIp).isEqualTo("10.0.0.1");
    }

    @Test
    void shouldReturnRemoteAddr_whenNoProxyHeaders() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedClientIp).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldReadCrossServiceHeaders() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("req-1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(request.getHeader("X-User-Id")).thenReturn("42");
        when(request.getHeader("X-User-Name")).thenReturn("Alice");
        when(request.getHeader("X-Tenant-Id")).thenReturn("100");
        when(request.getHeader("X-Tenant-Name")).thenReturn("TenantX");

        Long[] capturedUserId = new Long[1];
        String[] capturedUserName = new String[1];
        Long[] capturedTenantId = new Long[1];
        String[] capturedTenantName = new String[1];

        org.mockito.Mockito.doAnswer(invocation -> {
            capturedRequestId = RequestContext.getRequestId();
            capturedClientIp = RequestContext.getClientIp();
            capturedUserId[0] = RequestContext.getUserId();
            capturedUserName[0] = RequestContext.getUserName();
            capturedTenantId[0] = RequestContext.getTenantId();
            capturedTenantName[0] = RequestContext.getTenantName();
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedRequestId).isEqualTo("req-1");
        assertThat(capturedClientIp).isEqualTo("10.0.0.1");
        assertThat(capturedUserId[0]).isEqualTo(42L);
        assertThat(capturedUserName[0]).isEqualTo("Alice");
        assertThat(capturedTenantId[0]).isEqualTo(100L);
        assertThat(capturedTenantName[0]).isEqualTo("TenantX");
    }

    @Test
    void shouldDecodeUrlEncodedCrossServiceHeaders() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("req-1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(request.getHeader("X-User-Id")).thenReturn("42");
        when(request.getHeader("X-User-Name")).thenReturn("%E8%B6%85%E7%BA%A7%E7%AE%A1%E7%90%86%E5%91%98");
        when(request.getHeader("X-Tenant-Id")).thenReturn("100");
        when(request.getHeader("X-Tenant-Name")).thenReturn("%E6%B5%8B%E8%AF%95%E7%A7%9F%E6%88%B7");

        String[] capturedUserName = new String[1];
        String[] capturedTenantName = new String[1];

        org.mockito.Mockito.doAnswer(invocation -> {
            capturedRequestId = RequestContext.getRequestId();
            capturedClientIp = RequestContext.getClientIp();
            capturedUserName[0] = RequestContext.getUserName();
            capturedTenantName[0] = RequestContext.getTenantName();
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedUserName[0]).isEqualTo("超级管理员");
        assertThat(capturedTenantName[0]).isEqualTo("测试租户");
    }

    @Test
    void shouldPassNullHeaderValuesThrough() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("req-1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Name")).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn(null);
        when(request.getHeader("X-Tenant-Name")).thenReturn(null);

        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedRequestId).isEqualTo("req-1");
        assertThat(capturedClientIp).isEqualTo("10.0.0.1");
    }

    @Test
    void shouldCleanContextAfterChain() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("test-123");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedRequestId).isEqualTo("test-123");
        // After filter returns, context is cleaned (ScopedValue scope ended)
        assertThat(RequestContext.getRequestId()).isNull();
        assertThat(RequestContext.getClientIp()).isNull();
    }

    @Test
    void shouldPutRequestIdToMDC() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("mdc-test-123");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String[] capturedMDCRequestId = new String[1];
        org.mockito.Mockito.doAnswer(invocation -> {
            capturedMDCRequestId[0] = MDC.get("requestId");
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedMDCRequestId[0]).isEqualTo("mdc-test-123");
    }

    @Test
    void shouldClearMDCAfterRequest() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("mdc-test-456");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        setupCaptureInChain();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void shouldAddRequestIdToResponseHeader() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String[] generatedRequestId = new String[1];
        org.mockito.Mockito.doAnswer(invocation -> {
            generatedRequestId[0] = RequestContext.getRequestId();
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        org.mockito.Mockito.verify(response).setHeader("X-Request-Id", generatedRequestId[0]);
    }

    private void setupCaptureInChain() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            capturedRequestId = RequestContext.getRequestId();
            capturedClientIp = RequestContext.getClientIp();
            return null;
        }).when(filterChain).doFilter(any(), any());
    }
}
