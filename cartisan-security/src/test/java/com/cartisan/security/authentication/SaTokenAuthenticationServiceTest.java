package com.cartisan.security.authentication;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * SaTokenAuthenticationService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SaTokenAuthenticationServiceTest {

    @InjectMocks
    private SaTokenAuthenticationService authService;

    @Nested
    @DisplayName("authenticate")
    class AuthenticateTests {

        @Test
        @DisplayName("AC4: 默认实现抛出 UnsupportedOperationException")
        void given_defaultAuthenticate_when_call_then_throwException() {
            // When & Then
            assertThatThrownBy(() -> authService.authenticate("admin", "123"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Authentication not implemented");
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("AC1: login() 创建会话并返回 TokenInfo")
        void given_validLoginId_when_login_then_returnTokenInfo() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                Long loginId = 123L;
                String userName = "alice";
                String token = "test-token-abc";
                long timeoutSeconds = 7200L;

                // Mock StpUtil（getSession 供写入 userName，仅作 setup，不断言内部调用序列）
                stpUtilMock.when(() -> StpUtil.login(loginId)).then(invocation -> null);
                SaSession session = mock(SaSession.class);
                stpUtilMock.when(StpUtil::getSession).thenReturn(session);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn(token);
                stpUtilMock.when(StpUtil::getTokenTimeout).thenReturn(timeoutSeconds);

                // When
                TokenInfo result = authService.login(loginId, userName);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.token()).isEqualTo(token);
                assertThat(result.loginId()).isEqualTo(loginId);
                assertThat(result.expireTime()).isAfter(Instant.now());

                // Verify login was called
                stpUtilMock.verify(() -> StpUtil.login(loginId));
            }
        }

        @Test
        @DisplayName("loginId 为 null 时抛出 NullPointerException")
        void given_nullLoginId_when_login_then_throwNullPointerException() {
            // When & Then（loginId 校验在写 userName 之前，无需 mock session）
            assertThatThrownBy(() -> authService.login(null, "alice"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("loginId");
        }
    }

    @Nested
    @DisplayName("login with custom timeout")
    class LoginWithTimeoutTests {

        @Test
        @DisplayName("login(loginId, timeout, userName) 创建会话并返回 TokenInfo")
        void given_validLoginIdAndTimeout_when_loginWithTimeout_then_returnTokenInfo() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                Long loginId = 123L;
                String userName = "bob";
                String token = "test-token-custom-timeout";
                long timeoutSeconds = 86400L; // 1天

                // Mock StpUtil（getSession 供写入 userName，仅作 setup）
                stpUtilMock.when(() -> StpUtil.login(loginId, timeoutSeconds)).then(invocation -> null);
                SaSession session = mock(SaSession.class);
                stpUtilMock.when(StpUtil::getSession).thenReturn(session);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn(token);
                stpUtilMock.when(StpUtil::getTokenTimeout).thenReturn(timeoutSeconds);

                // When
                TokenInfo result = authService.login(loginId, timeoutSeconds, userName);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.token()).isEqualTo(token);
                assertThat(result.loginId()).isEqualTo(loginId);
                assertThat(result.expireTime()).isAfter(Instant.now());
                assertThat(result.expireTime()).isBefore(Instant.now().plusSeconds(timeoutSeconds + 10));

                // Verify login was called with timeout
                stpUtilMock.verify(() -> StpUtil.login(loginId, timeoutSeconds));
            }
        }

        @Test
        @DisplayName("loginId 为 null 时抛出 NullPointerException")
        void given_nullLoginId_when_loginWithTimeout_then_throwNullPointerException() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // When & Then（loginId 校验在写 userName 之前，无需 mock session）
                assertThatThrownBy(() -> authService.login(null, 3600, "bob"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("loginId");
            }
        }

        @Test
        @DisplayName("timeoutSeconds <= 0 时抛出 IllegalArgumentException")
        void given_zeroOrNegativeTimeout_when_loginWithTimeout_then_throwIllegalArgumentException() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // When & Then - zero（超时校验在写 userName 之前，无需 mock session）
                assertThatThrownBy(() -> authService.login(123L, 0, "bob"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("timeoutSeconds must be positive");

                // When & Then - negative
                assertThatThrownBy(() -> authService.login(123L, -100, "bob"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("timeoutSeconds must be positive");
            }
        }

        @Test
        @DisplayName("expireTime 计算正确（1天后）")
        void given_oneDayTimeout_when_loginWithTimeout_then_expireTimeIsCorrect() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                Long loginId = 123L;
                String userName = "carol";
                String token = "test-token";
                long timeoutSeconds = 86400L; // 1天
                Instant beforeLogin = Instant.now();

                stpUtilMock.when(() -> StpUtil.login(loginId, timeoutSeconds)).then(invocation -> null);
                SaSession session = mock(SaSession.class);
                stpUtilMock.when(StpUtil::getSession).thenReturn(session);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn(token);

                // When
                TokenInfo result = authService.login(loginId, timeoutSeconds, userName);

                // Then
                Instant afterLogin = Instant.now();
                assertThat(result.expireTime()).isAfter(beforeLogin.plusSeconds(timeoutSeconds - 10));
                assertThat(result.expireTime()).isBefore(afterLogin.plusSeconds(timeoutSeconds + 10));
            }
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("AC2: logout() 调用 StpUtil.logout()")
        void given_loggedIn_when_logout_then_sessionDestroyed() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // When
                authService.logout();

                // Then
                stpUtilMock.verify(StpUtil::logout);
            }
        }

        @Test
        @DisplayName("未登录时 logout() 不抛异常")
        void given_notLoggedIn_when_logout_then_noException() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given - StpUtil.logout() 在未登录时也不抛异常
                stpUtilMock.when(StpUtil::logout).then(invocation -> null);

                // When & Then
                assertThatCode(() -> authService.logout()).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("getTokenInfo")
    class GetTokenInfoTests {

        @Test
        @DisplayName("AC3: 已登录时返回 TokenInfo")
        void given_loggedIn_when_getTokenInfo_then_returnTokenInfo() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                Long loginId = 456L;
                String token = "test-token-xyz";
                long timeoutSeconds = 3600L;

                stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn(token);
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(loginId);
                stpUtilMock.when(StpUtil::getTokenTimeout).thenReturn(timeoutSeconds);

                // When
                TokenInfo result = authService.getTokenInfo();

                // Then
                assertThat(result).isNotNull();
                assertThat(result.token()).isEqualTo(token);
                assertThat(result.loginId()).isEqualTo(loginId);
                assertThat(result.expireTime()).isAfter(Instant.now());
            }
        }

        @Test
        @DisplayName("AC6: 未登录时返回 null")
        void given_notLoggedIn_when_getTokenInfo_then_returnNull() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

                // When
                TokenInfo result = authService.getTokenInfo();

                // Then
                assertThat(result).isNull();
            }
        }
    }

    @Nested
    @DisplayName("TokenInfo")
    class TokenInfoTests {

        @Test
        @DisplayName("AC5: TokenInfo 所有字段非空")
        void given_tokenInfo_when_create_then_allFieldsNotNull() {
            // Given
            String token = "test-token";
            Long loginId = 123L;
            Instant expireTime = Instant.now().plusSeconds(3600);

            // When
            TokenInfo tokenInfo = new TokenInfo(token, loginId, expireTime);

            // Then
            assertThat(tokenInfo.token()).isEqualTo(token);
            assertThat(tokenInfo.loginId()).isEqualTo(loginId);
            assertThat(tokenInfo.expireTime()).isEqualTo(expireTime);
        }

        @Test
        @DisplayName("token 为 null 时抛出 NullPointerException")
        void given_nullToken_when_create_then_throwNullPointerException() {
            // When & Then
            assertThatThrownBy(() -> new TokenInfo(null, 123L, Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("token");
        }

        @Test
        @DisplayName("loginId 为 null 时抛出 NullPointerException")
        void given_nullLoginId_when_create_then_throwNullPointerException() {
            // When & Then
            assertThatThrownBy(() -> new TokenInfo("token", null, Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("loginId");
        }

        @Test
        @DisplayName("expireTime 为 null 时抛出 NullPointerException")
        void given_nullExpireTime_when_create_then_throwNullPointerException() {
            // When & Then
            assertThatThrownBy(() -> new TokenInfo("token", 123L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("expireTime");
        }
    }
}
