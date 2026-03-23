package com.cartisan.security.authentication;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * AuthenticationService 接口 default 方法测试。
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private final AuthenticationService authService = new SaTokenAuthenticationService();

    @Nested
    @DisplayName("getCurrentUserId")
    class GetCurrentUserIdTests {

        @AfterEach
        void tearDown() {
            try {
                StpUtil.logout();
            } catch (Exception e) {
                // ignore if not logged in
            }
        }

        @Test
        @DisplayName("未登录时返回 Optional.empty()")
        void given_notLoggedIn_when_getCurrentUserId_then_returnEmpty() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

                // When
                var result = authService.getCurrentUserId();

                // Then
                assertThat(result).isEmpty();
            }
        }

        @Test
        @DisplayName("已登录时返回 Optional.of(loginId)")
        void given_loggedIn_when_getCurrentUserId_then_returnLoginId() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                Long loginId = 789L;
                String token = "test-token-get-current-user";
                long timeoutSeconds = 3600L;

                stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn(token);
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(loginId);
                stpUtilMock.when(StpUtil::getTokenTimeout).thenReturn(timeoutSeconds);

                // When
                var result = authService.getCurrentUserId();

                // Then
                assertThat(result).isPresent();
                assertThat(result.get()).isEqualTo(loginId);
            }
        }
    }
}
