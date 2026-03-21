package com.cartisan.security.permission;

import com.cartisan.security.annotation.RequirePermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPermissionScanner 测试")
class DefaultPermissionScannerTest {

    private DefaultPermissionScanner scanner;

    @Mock
    private RequestMappingHandlerMapping handlerMapping;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        // Mock the handlerMethods to return our test mappings
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = Map.of(
            RequestMappingInfo.paths("/test1").build(),
            new HandlerMethod(new MethodUnderTest(), MethodUnderTest.class.getDeclaredMethod("methodWithPermission")),
            RequestMappingInfo.paths("/test2").build(),
            new HandlerMethod(new MethodUnderTest(), MethodUnderTest.class.getDeclaredMethod("methodWithNameAndScope")),
            RequestMappingInfo.paths("/test3").build(),
            new HandlerMethod(new MethodUnderTest(), MethodUnderTest.class.getDeclaredMethod("methodWithoutAnnotation"))
        );

        when(handlerMapping.getHandlerMethods()).thenReturn(handlerMethods);

        scanner = new DefaultPermissionScanner(handlerMapping);
    }

    @Test
    @DisplayName("首次调用 scanAll when 扫描则返回所有权限")
    void given_firstCall_when_scanAll_then_returnAllPermissions() {
        List<Permission> permissions = scanner.scanAll();

        assertThat(permissions).hasSize(2);
        assertThat(permissions).anyMatch(p ->
            p.code().equals("admin:user:read") &&
            p.name().equals("平台管理 / 用户管理 / 查看") &&
            p.scope().equals("admin")
        );
        assertThat(permissions).anyMatch(p ->
            p.code().equals("admin:user:write") &&
            p.name().equals("admin:user:write") &&
            p.scope() == null
        );
    }

    @Test
    @DisplayName("给定 scope when_scanByScope 则返回匹配的权限")
    void given_scope_when_scanByScope_then_returnMatchingPermissions() {
        List<Permission> adminPermissions = scanner.scanByScope("admin");

        assertThat(adminPermissions).hasSize(1);
        assertThat(adminPermissions.get(0).code()).isEqualTo("admin:user:read");
        assertThat(adminPermissions.get(0).scope()).isEqualTo("admin");
    }

    @Test
    @DisplayName("给定 null scope when_scanByScope 则返回无 scope 的权限")
    void given_nullScope_when_scanByScope_then_returnPermissionsWithoutScope() {
        List<Permission> permissions = scanner.scanByScope(null);

        assertThat(permissions).hasSize(1);
        assertThat(permissions.get(0).code()).isEqualTo("admin:user:write");
        assertThat(permissions.get(0).scope()).isNull();
    }

    @Test
    @DisplayName("多次调用 scanAll when 使用缓存")
    void given_multipleCalls_when_scanAll_then_useCache() {
        List<Permission> first = scanner.scanAll();
        List<Permission> second = scanner.scanAll();

        assertThat(first).isSameAs(second);
    }

    // ========== 测试用 Controller ==========

    static class MethodUnderTest {
        @RequirePermission(value = "admin:user:read", name = "平台管理 / 用户管理 / 查看", scope = "admin")
        public void methodWithPermission() {}

        @RequirePermission("admin:user:write")
        public void methodWithNameAndScope() {}

        public void methodWithoutAnnotation() {}
    }
}
