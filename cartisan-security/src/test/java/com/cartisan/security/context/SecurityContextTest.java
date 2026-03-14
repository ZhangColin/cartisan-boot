package com.cartisan.security.context;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

/**
 * SecurityContext 单元测试。
 * <p>
 * 测试命名规范：given_{条件}_when_{操作}_then_{预期结果}
 */
@ExtendWith(MockitoExtension.class)
class SecurityContextTest {

    private MockedStatic<StpUtil> mockedStpUtil;

    @BeforeEach
    void setUp() {
        mockedStpUtil = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        mockedStpUtil.close();
    }

    // ========== AC1: getCurrentUserId() 已登录返回用户 ID ==========

    @Test
    void given_userLoggedIn_when_getCurrentUserId_then_returnUserId() {
        // Given: 用户已登录
        mockedStpUtil.when(StpUtil::isLogin).thenReturn(true);
        mockedStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(123L);

        // When
        Long userId = SecurityContext.getCurrentUserId();

        // Then
        assertThat(userId).isEqualTo(123L);
    }

    // ========== AC2: getCurrentUserId() 未登录返回 null ==========

    @Test
    void given_userNotLoggedIn_when_getCurrentUserId_then_returnNull() {
        // Given: 用户未登录
        mockedStpUtil.when(StpUtil::isLogin).thenReturn(false);

        // When
        Long userId = SecurityContext.getCurrentUserId();

        // Then
        assertThat(userId).isNull();
    }

    // ========== AC3: getCurrentUsername() 已登录返回用户名 ==========

    @Test
    void given_userLoggedIn_when_getCurrentUsername_then_returnUsername() {
        // Given: 用户已登录
        mockedStpUtil.when(StpUtil::isLogin).thenReturn(true);
        mockedStpUtil.when(StpUtil::getLoginIdAsString).thenReturn("alice");

        // When
        String username = SecurityContext.getCurrentUsername();

        // Then
        assertThat(username).isEqualTo("alice");
    }

    // ========== AC4: getCurrentUsername() 未登录返回 null ==========

    @Test
    void given_userNotLoggedIn_when_getCurrentUsername_then_returnNull() {
        // Given: 用户未登录
        mockedStpUtil.when(StpUtil::isLogin).thenReturn(false);

        // When
        String username = SecurityContext.getCurrentUsername();

        // Then
        assertThat(username).isNull();
    }

    // ========== AC5: hasRole() 有角色返回 true ==========

    @Test
    void given_userHasRole_when_hasRole_then_returnTrue() {
        // Given: 用户有 admin 角色
        mockedStpUtil.when(() -> StpUtil.hasRole("admin")).thenReturn(true);

        // When
        boolean result = SecurityContext.hasRole("admin");

        // Then
        assertThat(result).isTrue();
    }

    // ========== AC6: hasRole() 无角色返回 false ==========

    @Test
    void given_userHasNoRole_when_hasRole_then_returnFalse() {
        // Given: 用户没有 admin 角色
        mockedStpUtil.when(() -> StpUtil.hasRole("admin")).thenReturn(false);

        // When
        boolean result = SecurityContext.hasRole("admin");

        // Then
        assertThat(result).isFalse();
    }

    // ========== AC7: hasPermission() 有权限返回 true ==========

    @Test
    void given_userHasPermission_when_hasPermission_then_returnTrue() {
        // Given: 用户有 user:create 权限
        mockedStpUtil.when(() -> StpUtil.hasPermission("user:create")).thenReturn(true);

        // When
        boolean result = SecurityContext.hasPermission("user:create");

        // Then
        assertThat(result).isTrue();
    }

    // ========== AC8: hasPermission() 无权限返回 false ==========

    @Test
    void given_userHasNoPermission_when_hasPermission_then_returnFalse() {
        // Given: 用户没有 user:create 权限
        mockedStpUtil.when(() -> StpUtil.hasPermission("user:create")).thenReturn(false);

        // When
        boolean result = SecurityContext.hasPermission("user:create");

        // Then
        assertThat(result).isFalse();
    }

    // ========== AC9: isAuthenticated() 已登录返回 true ==========

    @Test
    void given_userLoggedIn_when_isAuthenticated_then_returnTrue() {
        // Given: 用户已登录
        mockedStpUtil.when(StpUtil::isLogin).thenReturn(true);

        // When
        boolean result = SecurityContext.isAuthenticated();

        // Then
        assertThat(result).isTrue();
    }

    // ========== AC10: isAuthenticated() 未登录返回 false ==========

    @Test
    void given_userNotLoggedIn_when_isAuthenticated_then_returnFalse() {
        // Given: 用户未登录
        mockedStpUtil.when(StpUtil::isLogin).thenReturn(false);

        // When
        boolean result = SecurityContext.isAuthenticated();

        // Then
        assertThat(result).isFalse();
    }

    // ========== AC11: 工具类不可实例化 ==========

    @Test
    void given_reflectionInstantiate_when_throwUnsupportedOperationException() throws Exception {
        // Given: 获取私有构造函数
        Constructor<SecurityContext> constructor = SecurityContext.class.getDeclaredConstructor();

        // When & Then: 通过反射调用构造函数应抛出异常
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .hasCauseExactlyInstanceOf(UnsupportedOperationException.class)
                .satisfies(ex -> {
                    Throwable cause = ex.getCause();
                    assertThat(cause.getMessage()).contains("Utility class");
                });
    }

    // ========== 边界1: hasRole() role 为 null 抛异常 ==========

    @Test
    void given_roleIsNull_when_hasRole_then_throwIllegalArgumentException() {
        // When & Then
        assertThatThrownBy(() -> SecurityContext.hasRole(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role cannot be null or blank");
    }

    // ========== 边界2: hasRole() role 为空白抛异常 ==========

    @Test
    void given_roleIsBlank_when_hasRole_then_throwIllegalArgumentException() {
        // When & Then: 空字符串
        assertThatThrownBy(() -> SecurityContext.hasRole(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role cannot be null or blank");

        // When & Then: 仅空白字符
        assertThatThrownBy(() -> SecurityContext.hasRole("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role cannot be null or blank");
    }

    // ========== 边界3: hasPermission() permission 为 null 抛异常 ==========

    @Test
    void given_permissionIsNull_when_hasPermission_then_throwIllegalArgumentException() {
        // When & Then
        assertThatThrownBy(() -> SecurityContext.hasPermission(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Permission cannot be null or blank");
    }

    // ========== 边界4: hasPermission() permission 为空白抛异常 ==========

    @Test
    void given_permissionIsBlank_when_hasPermission_then_throwIllegalArgumentException() {
        // When & Then: 空字符串
        assertThatThrownBy(() -> SecurityContext.hasPermission(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Permission cannot be null or blank");

        // When & Then: 仅空白字符
        assertThatThrownBy(() -> SecurityContext.hasPermission("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Permission cannot be null or blank");
    }
}
