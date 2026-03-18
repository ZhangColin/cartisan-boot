package com.cartisan.security.annotation;

import com.cartisan.security.context.SecurityContext;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

/**
 * Spring MVC 参数解析器，处理 {@link CurrentUser} 注解的参数。
 * <p>
 * 从 {@link SecurityContext} 获取当前用户 ID，根据参数类型决定行为：
 * <ul>
 *   <li>{@code Long} - 未登录时抛 {@code NotLoginException}</li>
 *   <li>{@code Optional<Long>} - 未登录时返回 {@code Optional.empty()}</li>
 * </ul>
 *
 * @since 0.3.0
 */
@Component
public class CurrentUserMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 检查 @CurrentUser 注解
        if (!parameter.hasParameterAnnotation(CurrentUser.class)) {
            return false;
        }

        // 检查参数类型
        Class<?> paramType = parameter.getParameterType();
        return paramType == Long.class || paramType == Optional.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        // 从 SecurityContext 获取用户 ID
        Long userId = SecurityContext.getCurrentUserId();

        if (parameter.getParameterType() == Long.class) {
            // 必需登录：未登录时抛异常
            if (userId == null) {
                StpUtil.checkLogin(); // 抛出 NotLoginException
            }
            return userId;
        }

        if (parameter.getParameterType() == Optional.class) {
            // 可选登录：未登录时返回 empty
            return Optional.ofNullable(userId);
        }

        // 不应该到达这里
        throw new IllegalStateException("Unsupported parameter type: " + parameter.getParameterType());
    }
}
