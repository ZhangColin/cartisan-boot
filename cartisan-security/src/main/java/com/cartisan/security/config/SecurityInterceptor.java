package com.cartisan.security.config;

import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.security.annotation.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * MVC 拦截器，处理声明式权限鉴权。
 * <p>
 * 支持的注解：
 * <ul>
 *   <li>{@link RequireAuth} - 要求用户登录</li>
 *   <li>{@link RequireRole} - 要求用户拥有指定角色之一（OR 逻辑）</li>
 *   <li>{@link RequirePermission} - 要求用户拥有指定权限之一（OR 逻辑）</li>
 * </ul>
 * <p>
 * 注解优先级：方法注解优先于类注解。
 * 鉴权顺序：@RequireAuth → @RequireRole → @RequirePermission（AND 逻辑）
 */
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) {
        // 非 HandlerMethod 直接放行（静态资源等）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        Class<?> beanType = handlerMethod.getBeanType();

        // @RequireAuth 检查
        RequireAuth requireAuth = findAnnotation(method, beanType, RequireAuth.class);
        if (requireAuth != null && requireAuth.value()) {
            StpUtil.checkLogin();
        }

        // @RequireRole 检查
        RequireRole requireRole = findAnnotation(method, beanType, RequireRole.class);
        if (requireRole != null) {
            StpUtil.checkRoleOr(requireRole.value());
        }

        // @RequirePermission 检查
        RequirePermission requirePermission = findAnnotation(method, beanType, RequirePermission.class);
        if (requirePermission != null) {
            StpUtil.checkPermissionOr(requirePermission.value());
        }

        return true;
    }

    /**
     * 查找注解，方法注解优先于类注解。
     *
     * @param method         Controller 方法
     * @param beanType       Controller 类
     * @param annotationType 注解类型
     * @param <A>            注解泛型
     * @return 注解实例，不存在返回 {@code null}
     */
    private <A extends Annotation> A findAnnotation(Method method,
                                                     Class<?> beanType,
                                                     Class<A> annotationType) {
        // 优先查找方法注解
        A methodAnnotation = method.getAnnotation(annotationType);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        // 方法无注解时查找类注解
        return beanType.getAnnotation(annotationType);
    }
}
