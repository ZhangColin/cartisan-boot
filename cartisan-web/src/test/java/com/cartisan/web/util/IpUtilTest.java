package com.cartisan.web.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link IpUtil} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class IpUtilTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void should_returnRemoteAddr_when_noXForwardedFor() {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("192.168.1.100");
    }

    @Test
    void should_returnFirstIp_fromXForwardedFor_when_trustedProxy() {
        // Given
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1, 127.0.0.1");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("203.0.113.1");
    }

    @Test
    void should_returnRemoteAddr_when_untrustedProxy() {
        // Given
        when(request.getRemoteAddr()).thenReturn("8.8.8.8");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("8.8.8.8");
    }

    @Test
    void should_returnRemoteAddr_when_xForwardedForIsEmpty() {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("192.168.1.1");
    }

    @Test
    void should_returnDefault_when_remoteAddrIsNull() {
        // Given
        when(request.getRemoteAddr()).thenReturn(null);

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("127.0.0.1");
    }

    @Test
    void should_trustLocalhost_127() {
        // Given
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("203.0.113.1");
    }

    @Test
    void should_trustIPv6Loopback() {
        // Given
        when(request.getRemoteAddr()).thenReturn("::1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("203.0.113.1");
    }

    @Test
    void should_trustIPv6FullLoopback() {
        // Given
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("203.0.113.1");
    }

    @Test
    void should_trustPrivateNetwork_10() {
        // Given
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("203.0.113.1");
    }

    @Test
    void should_trustPrivateNetwork_192_168() {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("203.0.113.1");
    }

    @Test
    void should_trustPrivateNetwork_172_16_to_31() {
        // Given: 测试 172.16.0.0/12 范围的边界值
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("203.0.113.1");
    }

    @Test
    void should_trustPrivateNetwork_172_31() {
        // Given
        when(request.getRemoteAddr()).thenReturn("172.31.255.255");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("203.0.113.1");
    }

    @Test
    void should_notTrustPrivateNetwork_172_15() {
        // Given: 172.15.x.x 不在私有范围内
        when(request.getRemoteAddr()).thenReturn("172.15.255.255");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("172.15.255.255");
    }

    @Test
    void should_notTrustPrivateNetwork_172_32() {
        // Given: 172.32.x.x 不在私有范围内
        when(request.getRemoteAddr()).thenReturn("172.32.0.1");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("172.32.0.1");
    }

    @Test
    void should_trimWhitespace_fromXForwardedFor() {
        // Given
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.1  ,  10.0.0.1  ");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("203.0.113.1");
    }

    @Test
    void should_handleSingleIp_inXForwardedFor() {
        // Given
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

        // When
        String clientIp = IpUtil.getClientIp(request);

        // Then
        assertThat(clientIp).isEqualTo("203.0.113.1");
    }
}
