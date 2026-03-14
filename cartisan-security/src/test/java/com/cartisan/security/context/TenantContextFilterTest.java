package com.cartisan.security.context;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.Ordered;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TenantContextFilter 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class TenantContextFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private TenantContextFilter filter;

    @AfterEach
    void tearDown() {
        // 确保每个测试后租户上下文被清理
        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }

    @Nested
    @DisplayName("parseTenantIdFromHeader")
    class ParseTenantIdFromHeaderTests {

        @Test
        @DisplayName("AC1: Header 存在且有效 → 返回租户 ID")
        void given_validHeader_when_parseFromHeader_then_returnTenantId() throws Exception {
            // Given
            when(request.getHeader("X-Tenant-Id")).thenReturn("123");

            // When
            Long tenantId = invokeParseFromHeader(filter, request);

            // Then
            assertThat(tenantId).isEqualTo(123L);
        }

        @Test
        @DisplayName("AC5: Header 为空字符串 → 返回 null")
        void given_emptyHeader_when_parseFromHeader_then_returnNull() throws Exception {
            // Given
            when(request.getHeader("X-Tenant-Id")).thenReturn("");

            // When
            Long tenantId = invokeParseFromHeader(filter, request);

            // Then
            assertThat(tenantId).isNull();
        }

        @Test
        @DisplayName("AC6: Header 格式错误 → 返回 null（不抛异常）")
        void given_invalidHeaderFormat_when_parseFromHeader_then_returnNull() throws Exception {
            // Given
            when(request.getHeader("X-Tenant-Id")).thenReturn("abc");

            // When
            Long tenantId = invokeParseFromHeader(filter, request);

            // Then
            assertThat(tenantId).isNull();
        }

        @Test
        @DisplayName("Header 不存在 → 返回 null")
        void given_noHeader_when_parseFromHeader_then_returnNull() throws Exception {
            // Given
            when(request.getHeader("X-Tenant-Id")).thenReturn(null);

            // When
            Long tenantId = invokeParseFromHeader(filter, request);

            // Then
            assertThat(tenantId).isNull();
        }

        @Test
        @DisplayName("Header 只有空白字符 → 返回 null")
        void given_blankHeader_when_parseFromHeader_then_returnNull() throws Exception {
            // Given
            when(request.getHeader("X-Tenant-Id")).thenReturn("   ");

            // When
            Long tenantId = invokeParseFromHeader(filter, request);

            // Then
            assertThat(tenantId).isNull();
        }
    }

    @Nested
    @DisplayName("parseTenantIdFromSession")
    class ParseTenantIdFromSessionTests {

        @Test
        @DisplayName("AC2: 用户已登录且 Session 有 tenantId → 返回租户 ID")
        void given_loggedInWithTenantId_when_parseFromSession_then_returnTenantId() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
                SaSession mockSession = mock(SaSession.class);
                when(mockSession.get("tenantId")).thenReturn(456L);
                stpUtilMock.when(StpUtil::getSession).thenReturn(mockSession);

                // When
                Long tenantId = invokeParseFromSession(filter);

                // Then
                assertThat(tenantId).isEqualTo(456L);
            }
        }

        @Test
        @DisplayName("用户未登录 → 返回 null")
        void given_notLoggedIn_when_parseFromSession_then_returnNull() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

                // When
                Long tenantId = invokeParseFromSession(filter);

                // Then
                assertThat(tenantId).isNull();
            }
        }

        @Test
        @DisplayName("Session 中无 tenantId → 返回 null")
        void given_sessionWithoutTenantId_when_parseFromSession_then_returnNull() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
                SaSession mockSession = mock(SaSession.class);
                when(mockSession.get("tenantId")).thenReturn(null);
                stpUtilMock.when(StpUtil::getSession).thenReturn(mockSession);

                // When
                Long tenantId = invokeParseFromSession(filter);

                // Then
                assertThat(tenantId).isNull();
            }
        }

        @Test
        @DisplayName("Session 中 tenantId 格式错误 → 返回 null")
        void given_invalidSessionTenantId_when_parseFromSession_then_returnNull() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
                SaSession mockSession = mock(SaSession.class);
                when(mockSession.get("tenantId")).thenReturn("not-a-number");
                stpUtilMock.when(StpUtil::getSession).thenReturn(mockSession);

                // When
                Long tenantId = invokeParseFromSession(filter);

                // Then
                assertThat(tenantId).isNull();
            }
        }
    }

    @Nested
    @DisplayName("resolveTenantId")
    class ResolveTenantIdTests {

        @Test
        @DisplayName("AC4: Header 和 Session 都存在 → Header 优先")
        void given_bothHeaderAndSession_when_resolveTenantId_then_headerPriority() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                when(request.getHeader("X-Tenant-Id")).thenReturn("123");
                // Session 相关的 stub 不需要设置，因为 Header 优先，Session 不会被访问

                // When
                Long tenantId = invokeResolveTenantId(filter, request);

                // Then
                assertThat(tenantId).isEqualTo(123L);  // Header 值优先
                // Session 不应该被访问（isLogin 不应该被调用）
                stpUtilMock.verifyNoInteractions();
            }
        }

        @Test
        @DisplayName("AC2: Header 不存在，Session 有租户 → 从 Session 读取")
        void given_noHeaderButSessionHasTenant_when_resolveTenantId_then_fromSession() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                when(request.getHeader("X-Tenant-Id")).thenReturn(null);
                stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
                SaSession mockSession = mock(SaSession.class);
                when(mockSession.get("tenantId")).thenReturn(456L);
                stpUtilMock.when(StpUtil::getSession).thenReturn(mockSession);

                // When
                Long tenantId = invokeResolveTenantId(filter, request);

                // Then
                assertThat(tenantId).isEqualTo(456L);
            }
        }

        @Test
        @DisplayName("AC3: Header 和 Session 都不存在 → 返回 null")
        void given_noHeaderAndNoSession_when_resolveTenantId_then_returnNull() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                when(request.getHeader("X-Tenant-Id")).thenReturn(null);
                stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

                // When
                Long tenantId = invokeResolveTenantId(filter, request);

                // Then
                assertThat(tenantId).isNull();
            }
        }
    }

    @Nested
    @DisplayName("doFilter")
    class DoFilterTests {

        @Test
        @DisplayName("有租户 ID 时，租户上下文被正确设置")
        void given_tenantId_when_doFilter_then_contextIsSet() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                when(request.getHeader("X-Tenant-Id")).thenReturn("123");
                stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

                // When - 在 doFilter 执行期间检查上下文
                final Long[] capturedTenantId = new Long[1];
                filter.doFilter(request, response, (req, res) -> {
                    capturedTenantId[0] = TenantContext.getCurrentTenantId();
                });

                // Then
                assertThat(capturedTenantId[0]).isEqualTo(123L);
                // doFilter 结束后上下文应该被清理
                assertThat(TenantContext.getCurrentTenantId()).isNull();
            }
        }

        @Test
        @DisplayName("无租户 ID 时，租户上下文为 null")
        void given_noTenantId_when_doFilter_then_contextIsNull() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                when(request.getHeader("X-Tenant-Id")).thenReturn(null);
                stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

                // When
                final Long[] capturedTenantId = new Long[1];
                filter.doFilter(request, response, (req, res) -> {
                    capturedTenantId[0] = TenantContext.getCurrentTenantId();
                });

                // Then
                assertThat(capturedTenantId[0]).isNull();
            }
        }

        @Test
        @DisplayName("AC7: 请求处理中抛异常，上下文仍被清理")
        void given_exceptionInChain_when_doFilter_then_contextIsCleared() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                when(request.getHeader("X-Tenant-Id")).thenReturn("123");
                stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

                // When & Then
                assertThat(org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    filter.doFilter(request, response, (req, res) -> {
                        throw new RuntimeException("Test exception");
                    })
                ).isInstanceOf(RuntimeException.class).hasMessage("Test exception"));

                // 上下文仍被清理
                assertThat(TenantContext.getCurrentTenantId()).isNull();
            }
        }

        @Test
        @DisplayName("AC8: Filter 不抛异常（格式错误场景）")
        void given_invalidHeader_when_doFilter_then_noException() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                when(request.getHeader("X-Tenant-Id")).thenReturn("abc");
                stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

                // When & Then
                assertThatNoException().isThrownBy(() ->
                    filter.doFilter(request, response, chain)
                );
            }
        }
    }

    @Nested
    @DisplayName("Order")
    class OrderTests {

        @Test
        @DisplayName("AC12: Filter 有 @Order 注解或实现 Ordered 接口")
        void given_filterClass_when_checkOrder_then_implementsOrdered() {
            // Given & When & Then
            assertThat(filter).isInstanceOf(Ordered.class);
            assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 10);
        }
    }

    // ========== 反射调用私有方法 ==========

    private Long invokeParseFromHeader(TenantContextFilter filter, HttpServletRequest request)
            throws Exception {
        var method = TenantContextFilter.class.getDeclaredMethod(
            "parseTenantIdFromHeader", HttpServletRequest.class);
        method.setAccessible(true);
        return (Long) method.invoke(filter, request);
    }

    private Long invokeParseFromSession(TenantContextFilter filter) throws Exception {
        var method = TenantContextFilter.class.getDeclaredMethod("parseTenantIdFromSession");
        method.setAccessible(true);
        return (Long) method.invoke(filter);
    }

    private Long invokeResolveTenantId(TenantContextFilter filter, HttpServletRequest request)
            throws Exception {
        var method = TenantContextFilter.class.getDeclaredMethod(
            "resolveTenantId", HttpServletRequest.class);
        method.setAccessible(true);
        return (Long) method.invoke(filter, request);
    }
}
