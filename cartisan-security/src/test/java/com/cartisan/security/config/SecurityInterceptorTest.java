package com.cartisan.security.config;

import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.security.annotation.RequireRole;
import com.cartisan.security.authorization.AuthorizationBypassResolver;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

/**
 * SecurityInterceptor 单元测试。
 * <p>
 * 测试命名规范：given_{条件}_when_{操作}_then_{预期结果}
 * <p>
 * 构造注入 mock 的 {@link ObjectProvider}（默认返回 null，即无 bypass resolver），
 * 验证未提供 resolver 时行为向后兼容。
 */
@ExtendWith(MockitoExtension.class)
class SecurityInterceptorTest {

    private SecurityInterceptor interceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ObjectProvider<AuthorizationBypassResolver> bypassResolverProvider;

    private MockedStatic<StpUtil> mockedStpUtil;

    @BeforeEach
    void setUp() {
        // bypassResolverProvider 默认 getIfAvailable() 返回 null（无 resolver），验证向后兼容
        interceptor = new SecurityInterceptor(bypassResolverProvider);
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

    // ========== AC3: @RequireRole 多值 OR 逻辑 ==========

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

    // ========== AC4: @RequirePermission 检查 ==========

    @Test
    void given_requirePermission_when_preHandle_then_callCheckPermission() throws Exception {
        // Given: 方法有 @RequirePermission("user:create")
        Method method = TestController.class.getMethod("requirePermissionMethod");
        HandlerMethod handler = new HandlerMethod(new TestController(), method);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        mockedStpUtil.verify(() -> StpUtil.checkPermission("user:create"));
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

        @RequirePermission("user:create")
        public void requirePermissionMethod() {
        }
    }
}
