package com.cartisan.security.annotation;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.security.context.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.doThrow;

/**
 * CurrentUserMethodArgumentResolver 单元测试。
 * <p>
 * 测试命名规范：given_{条件}_when_{操作}_then_{预期结果}
 */
@ExtendWith(MockitoExtension.class)
class CurrentUserMethodArgumentResolverTest {

    private CurrentUserMethodArgumentResolver resolver;
    private MockedStatic<SecurityContext> mockedSecurityContext;
    private MockedStatic<StpUtil> mockedStpUtil;
    private ModelAndViewContainer mavContainer;
    private NativeWebRequest webRequest;
    private WebDataBinderFactory binderFactory;

    @BeforeEach
    void setUp() {
        resolver = new CurrentUserMethodArgumentResolver();
        mockedSecurityContext = mockStatic(SecurityContext.class);
        mockedStpUtil = mockStatic(StpUtil.class);
        mavContainer = null; // 未使用
        webRequest = null; // 未使用
        binderFactory = null; // 未使用
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContext.close();
        mockedStpUtil.close();
    }

    // ========== supportsParameter() 测试 ==========

    @Test
    void given_parameterWithCurrentUserAnnotationAndLongType_when_supportsParameter_then_returnTrue()
            throws NoSuchMethodException {
        // Given: 参数有 @CurrentUser 注解且类型为 Long
        Method method = TestController.class.getMethod("methodWithLong", Long.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        // When
        boolean result = resolver.supportsParameter(parameter);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void given_parameterWithCurrentUserAnnotationAndOptionalLongType_when_supportsParameter_then_returnTrue()
            throws NoSuchMethodException {
        // Given: 参数有 @CurrentUser 注解且类型为 Optional<Long>
        Method method = TestController.class.getMethod("methodWithOptional", Optional.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        // When
        boolean result = resolver.supportsParameter(parameter);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void given_parameterWithoutCurrentUserAnnotation_when_supportsParameter_then_returnFalse()
            throws NoSuchMethodException {
        // Given: 参数没有 @CurrentUser 注解
        Method method = TestController.class.getMethod("methodWithoutAnnotation", Long.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        // When
        boolean result = resolver.supportsParameter(parameter);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void given_parameterWithUnsupportedType_when_supportsParameter_then_returnFalse()
            throws NoSuchMethodException {
        // Given: 参数有 @CurrentUser 注解但类型为 String（不支持）
        Method method = TestController.class.getMethod("methodWithString", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        // When
        boolean result = resolver.supportsParameter(parameter);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void given_optionalLongWithoutAnnotation_when_supportsParameter_then_returnFalse()
            throws NoSuchMethodException {
        // Given: 参数类型为 Optional<Long> 但没有 @CurrentUser 注解
        Method method = TestController.class.getMethod("methodWithOptionalNoAnnotation", Optional.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        // When
        boolean result = resolver.supportsParameter(parameter);

        // Then
        assertThat(result).isFalse();
    }

    // ========== resolveArgument() 测试 ==========

    @Test
    void given_authenticatedUser_when_resolveLong_then_returnUserId()
            throws NoSuchMethodException {
        // Given: 用户已登录
        mockedSecurityContext.when(SecurityContext::getCurrentUserId).thenReturn(123L);
        Method method = TestController.class.getMethod("methodWithLong", Long.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        // When
        Object result = resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        // Then
        assertThat(result).isEqualTo(123L);
    }

    @Test
    void given_unauthenticatedUser_when_resolveLong_then_throwNotLoginException()
            throws NoSuchMethodException {
        // Given: 用户未登录
        mockedSecurityContext.when(SecurityContext::getCurrentUserId).thenReturn(null);
        // StpUtil.checkLogin() 会抛出 NotLoginException
        mockedStpUtil.when(StpUtil::checkLogin).thenThrow(new NotLoginException(null, null, null));
        Method method = TestController.class.getMethod("methodWithLong", Long.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        // When & Then
        assertThatThrownBy(() ->
                resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory)
        ).isInstanceOf(NotLoginException.class);
    }

    @Test
    void given_authenticatedUser_when_resolveOptional_then_returnPresent()
            throws NoSuchMethodException {
        // Given: 用户已登录
        mockedSecurityContext.when(SecurityContext::getCurrentUserId).thenReturn(123L);
        Method method = TestController.class.getMethod("methodWithOptional", Optional.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        // When
        Object result = resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        // Then
        @SuppressWarnings("unchecked")
        Optional<Long> optional = (Optional<Long>) result;
        assertThat(optional).isPresent().hasValue(123L);
    }

    @Test
    void given_unauthenticatedUser_when_resolveOptional_then_returnEmpty()
            throws NoSuchMethodException {
        // Given: 用户未登录
        mockedSecurityContext.when(SecurityContext::getCurrentUserId).thenReturn(null);
        Method method = TestController.class.getMethod("methodWithOptional", Optional.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        // When
        Object result = resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        // Then
        @SuppressWarnings("unchecked")
        Optional<Long> optional = (Optional<Long>) result;
        assertThat(optional).isEmpty();
    }

    // ========== 测试辅助 Controller ==========
    static class TestController {
        public void methodWithLong(@CurrentUser Long userId) { }
        public void methodWithOptional(@CurrentUser Optional<Long> userId) { }
        public void methodWithoutAnnotation(Long userId) { }
        public void methodWithString(@CurrentUser String username) { }
        public void methodWithOptionalNoAnnotation(Optional<Long> userId) { }
    }
}
