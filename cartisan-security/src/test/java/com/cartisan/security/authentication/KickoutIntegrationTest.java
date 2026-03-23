package com.cartisan.security.authentication;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

/**
 * SaTokenAuthenticationService 踢出功能集成测试。
 * <p>
 * 测试 kickout 和 kickoutByUsername 方法的行为。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kickout Integration Tests")
class KickoutIntegrationTest {

    private final SaTokenAuthenticationService authService = new SaTokenAuthenticationService();

    @Nested
    @DisplayName("kickout(Long loginId)")
    class KickoutByLoginIdTests {

        @Test
        @DisplayName("shouldKickoutUserByLoginId - 成功踢出用户")
        void shouldKickoutUserByLoginId() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                Long loginId = 123L;
                stpUtilMock.when(() -> StpUtil.kickout(loginId)).then(invocation -> null);

                // When & Then - 应该不抛异常
                assertThatCode(() -> authService.kickout(loginId))
                    .doesNotThrowAnyException();

                // Verify kickout was called
                stpUtilMock.verify(() -> StpUtil.kickout(loginId));
            }
        }

        @Test
        @DisplayName("shouldHandleNonExistentUser - 踢出不存在的用户不抛异常")
        void shouldHandleNonExistentUser() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                Long nonExistentLoginId = 999999L;
                stpUtilMock.when(() -> StpUtil.kickout(nonExistentLoginId)).then(invocation -> null);

                // When & Then - Sa-Token 对不存在的用户也静默处理
                assertThatCode(() -> authService.kickout(nonExistentLoginId))
                    .doesNotThrowAnyException();

                // Verify kickout was called
                stpUtilMock.verify(() -> StpUtil.kickout(nonExistentLoginId));
            }
        }

        @Test
        @DisplayName("loginId 为 null 时抛出 NullPointerException")
        void givenNullLoginId_whenKickout_thenThrowNullPointerException() {
            // When & Then
            assertThatThrownBy(() -> authService.kickout(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("loginId");
        }
    }

    @Nested
    @DisplayName("kickoutByUsername(String username)")
    class KickoutByUsernameTests {

        @Test
        @DisplayName("shouldKickoutUserByUsername - 默认实现抛出 UnsupportedOperationException")
        void shouldKickoutUserByUsername() {
            // Given
            String username = "admin";

            // When & Then - 默认实现应该抛出异常
            assertThatThrownBy(() -> authService.kickoutByUsername(username))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Kickout by username not implemented");
        }

        @Test
        @DisplayName("username 为 null 时抛出异常")
        void givenNullUsername_whenKickoutByUsername_thenThrowException() {
            // When & Then - null username 也会触发 UnsupportedOperationException
            assertThatThrownBy(() -> authService.kickoutByUsername(null))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("空字符串 username 也抛出 UnsupportedOperationException")
        void givenEmptyUsername_whenKickoutByUsername_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> authService.kickoutByUsername(""))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
