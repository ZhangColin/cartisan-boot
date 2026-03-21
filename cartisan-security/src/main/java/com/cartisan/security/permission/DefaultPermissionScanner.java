package com.cartisan.security.permission;

import com.cartisan.security.annotation.RequirePermission;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认权限扫描器实现。
 * <p>
 * 利用 Spring 的 {@link org.springframework.web.servlet.mvc.method.RequestMappingHandlerMapping} 扫描所有 HandlerMethod，
 * 提取 {@link RequirePermission} 注解并构造 {@link Permission} 对象。
 * </p>
 */
public class DefaultPermissionScanner implements PermissionScanner {

    private final RequestMappingHandlerMapping handlerMapping;
    private volatile List<Permission> allPermissionsCache;
    private final Map<String, List<Permission>> scopedPermissionsCache = new ConcurrentHashMap<>();

    public DefaultPermissionScanner(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public List<Permission> scanByScope(String scope) {
        if (scope == null) {
            return scanAll().stream()
                .filter(p -> p.scope() == null)
                .toList();
        }
        return scanAll().stream()
            .filter(p -> scope.equals(p.scope()))
            .toList();
    }

    @Override
    public List<Permission> scanAll() {
        if (allPermissionsCache != null) {
            return allPermissionsCache;
        }

        synchronized (this) {
            if (allPermissionsCache != null) {
                return allPermissionsCache;
            }

            Set<Permission> permissions = new java.util.HashSet<>();
            handlerMapping.getHandlerMethods().forEach((info, handlerMethod) -> {
                RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
                if (annotation != null) {
                    Permission permission = Permission.of(
                        annotation.value(),
                        annotation.name(),
                        annotation.scope()
                    );
                    permissions.add(permission);
                }
            });

            allPermissionsCache = List.copyOf(permissions);
            return allPermissionsCache;
        }
    }
}
