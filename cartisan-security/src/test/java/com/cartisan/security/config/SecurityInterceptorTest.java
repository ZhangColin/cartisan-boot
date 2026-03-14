package com.cartisan.security.config;

import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.security.annotation.RequireRole;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

/**
 * SecurityInterceptor 单元测试。
 * <p>
 * 测试命名规范：given_{条件}_when_{操作}_then_{预期结果}
 */
@ExtendWith(MockitoExtension.class)
class SecurityInterceptorTest {

    private SecurityInterceptor interceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private MockedStatic<StpUtil> mockedStpUtil;

    @BeforeEach
    void setUp() {
        interceptor = new SecurityInterceptor();
        mockedStpUtil = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        mockedStpUtil.close();
    }

    // ========== AC7: 非 HandlerMethod 直接放行 ==========

    @Test
    void given_nonHandlerMethod_when_preHandle_then_returnTrue() throws Exception {
        // Given: handler 不是 HandlerMethod
        Object handler = new Object();

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        // 验证没有调用任何 StpUtil 方法
        mockedStpUtil.verifyNoInteractions();
    }

    // ========== AC6: 无任何鉴权注解时放行 ==========

    @Test
    void given_noAnnotation_when_preHandle_then_returnTrue() throws Exception {
        // Given: HandlerMethod 且无任何鉴权注解
        Method method = TestController.class.getMethod("noAnnotationMethod");
        HandlerMethod handler = new HandlerMethod(new TestController(), method);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        mockedStpUtil.verifyNoInteractions();
    }

    // ========== AC1: @RequireAuth 检查 ==========

    @Test
    void given_methodRequireAuth_when_preHandle_then_callCheckLogin() throws Exception {
        // Given: 方法有 @RequireAuth 注解
        Method method = TestController.class.getMethod("requireAuthMethod");
        HandlerMethod handler = new HandlerMethod(new TestController(), method);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        mockedStpUtil.verify(StpUtil::checkLogin);
    }

    @Test
    void given_classRequireAuth_when_preHandle_then_callCheckLogin() throws Exception {
        // Given: 类有 @RequireAuth 注解，方法无注解
        Method method = TestClassRequireAuth.class.getMethod("methodWithoutAnnotation");
        HandlerMethod handler = new HandlerMethod(new TestClassRequireAuth(), method);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        mockedStpUtil.verify(StpUtil::checkLogin);
    }

    // ========== AC4: 方法注解优先于类注解（同一类型） ==========

    @Test
    void given_classAndMethodRequireAuth_when_methodHasFalse_then_doNotCheckLogin() throws Exception {
        // Given: 类有 @RequireAuth，方法有 @RequireAuth(false)，方法注解覆盖类注解
        Method method = TestClassRequireAuth.class.getMethod("methodWithRequireAuthFalse");
        HandlerMethod handler = new HandlerMethod(new TestClassRequireAuth(), method);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        mockedStpUtil.verify(StpUtil::checkLogin, never()); // value=false，不检查登录
    }

    @Test
    void given_classRequireAuthAndMethodRequireRole_when_preHandle_then_callBoth() throws Exception {
        // Given: 类有 @RequireAuth，方法有 @RequireRole（不同类型注解，AND 逻辑）
        Method method = TestClassRequireAuth.class.getMethod("methodWithRequireRole");
        HandlerMethod handler = new HandlerMethod(new TestClassRequireAuth(), method);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        // 验证两者都被调用（AND 逻辑：需要登录 AND 需要 admin 角色）
        mockedStpUtil.verify(StpUtil::checkLogin);
        mockedStpUtil.verify(() -> StpUtil.checkRoleOr("admin"));
    }

    // ========== AC2: @RequireRole 检查 ==========

    @Test
    void given_requireRoleSingle_when_preHandle_then_callCheckRoleOr() throws Exception {
        // Given: 方法有 @RequireRole({"admin"})
        Method method = TestController.class.getMethod("requireRoleMethod");
        HandlerMethod handler = new HandlerMethod(new TestController(), method);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        mockedStpUtil.verify(() -> StpUtil.checkRoleOr("admin"));
    }

    // ========== AC5: @RequireRole 多值 OR 逻辑 ==========

    @Test
    void given_requireRoleMultiple_when_preHandle_then_callCheckRoleOrWithAll() throws Exception {
        // Given: 方法有 @RequireRole({"admin", "super"})
        Method method = TestController.class.getMethod("requireRoleMultipleMethod");
        HandlerMethod handler = new HandlerMethod(new TestController(), method);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        mockedStpUtil.verify(() -> StpUtil.checkRoleOr("admin", "super"));
    }

    // ========== AC3: @RequirePermission 检查 ==========

    @Test
    void given_requirePermissionSingle_when_preHandle_then_callCheckPermissionOr() throws Exception {
        // Given: 方法有 @RequirePermission({"user:create"})
        Method method = TestController.class.getMethod("requirePermissionMethod");
        HandlerMethod handler = new HandlerMethod(new TestController(), method);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        mockedStpUtil.verify(() -> StpUtil.checkPermissionOr("user:create"));
    }

    // ========== AC5: @RequirePermission 多值 OR 逻辑 ==========

    @Test
    void given_requirePermissionMultiple_when_preHandle_then_callCheckPermissionOrWithAll() throws Exception {
        // Given: 方法有 @RequirePermission({"user:create", "user:update"})
        Method method = TestController.class.getMethod("requirePermissionMultipleMethod");
        HandlerMethod handler = new HandlerMethod(new TestController(), method);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        mockedStpUtil.verify(() -> StpUtil.checkPermissionOr("user:create", "user:update"));
    }

    // ========== 测试 Controller ==========

    /**
     * 无任何注解的测试 Controller
     */
    static class TestController {

        public void noAnnotationMethod() {
        }

        @RequireAuth
        public void requireAuthMethod() {
        }

        @RequireRole({"admin"})
        public void requireRoleMethod() {
        }

        @RequireRole({"admin", "super"})
        public void requireRoleMultipleMethod() {
        }

        @RequirePermission({"user:create"})
        public void requirePermissionMethod() {
        }

        @RequirePermission({"user:create", "user:update"})
        public void requirePermissionMultipleMethod() {
        }
    }

    /**
     * 类级别有 @RequireAuth 的测试 Controller
     */
    @RequireAuth
    static class TestClassRequireAuth {

        public void methodWithoutAnnotation() {
        }

        @RequireRole({"admin"})
        public void methodWithRequireRole() {
        }

        @RequireAuth(false)
        public void methodWithRequireAuthFalse() {
        }
    }
}
