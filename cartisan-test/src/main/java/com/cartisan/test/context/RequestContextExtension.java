package com.cartisan.test.context;

import com.cartisan.core.context.RequestContext;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * JUnit 5 Extension，读取 @WithRequestContext 注解并自动 ScopedValue.bind。
 *
 * <p>使用 InvocationInterceptor 将测试方法包裹在 ScopedValue.where().run() 中。</p>
 */
public class RequestContextExtension implements InvocationInterceptor, BeforeEachCallback {

    private static final ThreadLocal<RequestContext> CONTEXT_HOLDER = new ThreadLocal<>();

    @Override
    public void beforeEach(ExtensionContext context) {
        WithRequestContext annotation = findAnnotation(context);
        if (annotation == null) return;

        Long userId = annotation.userId() == 0L ? null : annotation.userId();
        Long tenantId = annotation.tenantId() == 0L ? null : annotation.tenantId();
        String userName = annotation.userName().isEmpty() ? null : annotation.userName();
        String tenantName = annotation.tenantName().isEmpty() ? null : annotation.tenantName();

        RequestContext ctx = new RequestContext(
                annotation.requestId(),
                annotation.clientIp(),
                null, null,
                userId, userName,
                tenantId, tenantName);

        CONTEXT_HOLDER.set(ctx);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {
        RequestContext ctx = CONTEXT_HOLDER.get();
        if (ctx != null) {
            RequestContext.run(ctx, () -> {
                try {
                    invocation.proceed();
                } catch (Throwable e) {
                    if (e instanceof RuntimeException re) throw re;
                    if (e instanceof Error err) throw err;
                    throw new RuntimeException(e);
                }
            });
        } else {
            invocation.proceed();
        }
    }

    /**
     * Get the RequestContext set by this extension (for programmatic use).
     */
    static RequestContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    private WithRequestContext findAnnotation(ExtensionContext context) {
        Optional<Method> method = context.getTestMethod();
        if (method.isPresent()) {
            WithRequestContext methodAnnotation = method.get().getAnnotation(WithRequestContext.class);
            if (methodAnnotation != null) return methodAnnotation;
        }

        Optional<Class<?>> testClass = context.getTestClass();
        if (testClass.isPresent()) {
            WithRequestContext classAnnotation = testClass.get().getAnnotation(WithRequestContext.class);
            if (classAnnotation != null) return classAnnotation;
        }

        return null;
    }
}
