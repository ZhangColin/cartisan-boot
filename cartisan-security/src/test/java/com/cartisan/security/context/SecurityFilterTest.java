package com.cartisan.security.context;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.core.context.RequestContext;
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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SecurityFilter 单元测试。
 * <p>
 * 验证 SecurityFilter 从 Sa-Token Session 正确读取 userName 并写入 RequestContext。
 */
@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    private SecurityFilter securityFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private MockedStatic<StpUtil> mockedStpUtil;

    @BeforeEach
    void setUp() {
        securityFilter = new SecurityFilter();
        mockedStpUtil = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        mockedStpUtil.close();
    }

    @Nested
    @DisplayName("已登录场景")
    class LoggedInTests {

        @Test
        @DisplayName("shouldWriteUserIdAndUserName when logged in with userName in session")
        void shouldWriteUserIdAndUserName_whenLoggedIn() throws ServletException, IOException {
            // Given
            mockedStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockedStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(42L);

            SaSession session = mock(SaSession.class);
            when(session.get("userName")).thenReturn("Alice");
            mockedStpUtil.when(StpUtil::getSession).thenReturn(session);

            // Capture context values from within the filter chain
            AtomicReference<Long> capturedUserId = new AtomicReference<>();
            AtomicReference<String> capturedUserName = new AtomicReference<>();
            AtomicReference<String> capturedRequestId = new AtomicReference<>();
            doAnswer(invocation -> {
                capturedUserId.set(RequestContext.getUserId());
                capturedUserName.set(RequestContext.getUserName());
                capturedRequestId.set(RequestContext.getRequestId());
                return null;
            }).when(filterChain).doFilter(any(), any());

            // Simulate RequestContextFilter already set a base context
            RequestContext baseCtx = new RequestContext("req-1", "127.0.0.1", null, null, null, null, null, null);

            // When
            RequestContext.run(baseCtx, () -> {
                try {
                    securityFilter.doFilterInternal(request, response, filterChain);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Then
            assertThat(capturedUserId.get()).isEqualTo(42L);
            assertThat(capturedUserName.get()).isEqualTo("Alice");
            assertThat(capturedRequestId.get()).isEqualTo("req-1");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("shouldWriteUserIdWithNullUserName when session has no userName key")
        void shouldWriteUserIdWithNullUserName_whenSessionHasNoUserName() throws ServletException, IOException {
            // Given
            mockedStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockedStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(99L);

            SaSession session = mock(SaSession.class);
            when(session.get("userName")).thenReturn(null);
            mockedStpUtil.when(StpUtil::getSession).thenReturn(session);

            AtomicReference<Long> capturedUserId = new AtomicReference<>();
            AtomicReference<String> capturedUserName = new AtomicReference<>();
            doAnswer(invocation -> {
                capturedUserId.set(RequestContext.getUserId());
                capturedUserName.set(RequestContext.getUserName());
                return null;
            }).when(filterChain).doFilter(any(), any());

            RequestContext baseCtx = new RequestContext(null, null, null, null, null, null, null, null);

            // When
            RequestContext.run(baseCtx, () -> {
                try {
                    securityFilter.doFilterInternal(request, response, filterChain);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Then
            assertThat(capturedUserId.get()).isEqualTo(99L);
            assertThat(capturedUserName.get()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("shouldWriteUserIdWithNullUserName when session is null")
        void shouldWriteUserIdWithNullUserName_whenSessionIsNull() throws ServletException, IOException {
            // Given
            mockedStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockedStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            mockedStpUtil.when(StpUtil::getSession).thenReturn(null);

            AtomicReference<Long> capturedUserId = new AtomicReference<>();
            AtomicReference<String> capturedUserName = new AtomicReference<>();
            doAnswer(invocation -> {
                capturedUserId.set(RequestContext.getUserId());
                capturedUserName.set(RequestContext.getUserName());
                return null;
            }).when(filterChain).doFilter(any(), any());

            RequestContext baseCtx = new RequestContext(null, null, null, null, null, null, null, null);

            // When
            RequestContext.run(baseCtx, () -> {
                try {
                    securityFilter.doFilterInternal(request, response, filterChain);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Then
            assertThat(capturedUserId.get()).isEqualTo(7L);
            assertThat(capturedUserName.get()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("未登录场景")
    class NotLoggedInTests {

        @Test
        @DisplayName("shouldPassThrough when not logged in")
        void shouldPassThrough_whenNotLoggedIn() throws ServletException, IOException {
            // Given
            mockedStpUtil.when(StpUtil::isLogin).thenReturn(false);

            AtomicReference<Long> capturedUserId = new AtomicReference<>();
            AtomicReference<String> capturedUserName = new AtomicReference<>();
            AtomicReference<String> capturedRequestId = new AtomicReference<>();
            doAnswer(invocation -> {
                capturedUserId.set(RequestContext.getUserId());
                capturedUserName.set(RequestContext.getUserName());
                capturedRequestId.set(RequestContext.getRequestId());
                return null;
            }).when(filterChain).doFilter(any(), any());

            RequestContext baseCtx = new RequestContext("req-2", "10.0.0.1", null, null, null, null, null, null);

            // When
            RequestContext.run(baseCtx, () -> {
                try {
                    securityFilter.doFilterInternal(request, response, filterChain);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Then
            assertThat(capturedUserId.get()).isNull();
            assertThat(capturedUserName.get()).isNull();
            assertThat(capturedRequestId.get()).isEqualTo("req-2");
            verify(filterChain).doFilter(request, response);
        }
    }
}
