package com.cartisan.security.permission;

import com.cartisan.security.integration.IntegrationTestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PermissionScanner 集成测试，验证在完整 Web 上下文中的功能。
 */
@SpringBootTest(classes = IntegrationTestApplication.class)
@WebAppConfiguration
class PermissionScannerIntegrationTest {

    @Autowired(required = false)
    private PermissionScanner permissionScanner;

    @Test
    void given_fullWebContext_when_contextLoads_then_permissionScannerBeanExists() {
        assertThat(permissionScanner).isNotNull();
        assertThat(permissionScanner).isInstanceOf(DefaultPermissionScanner.class);
    }

    @Test
    @DisplayName("scanAll when 扫描则返回所有权限")
    void given_controllersWithPermissions_when_scanAll_then_returnAll() {
        List<Permission> permissions = permissionScanner.scanAll();

        assertThat(permissions).isNotEmpty();
        assertThat(permissions).anyMatch(p ->
            p.code().equals("test:admin:user:read") &&
            p.name().equals("测试 / 管理员 / 用户查看") &&
            "test".equals(p.scope())
        );
        assertThat(permissions).anyMatch(p ->
            p.code().equals("test:simple:action") &&
            p.name().equals("test:simple:action") &&
            p.scope() == null
        );
    }

    @Test
    @DisplayName("scanByScope when 给定 scope 则返回匹配权限")
    void given_controllersWithPermissions_when_scanByScope_then_returnMatching() {
        List<Permission> testPermissions = permissionScanner.scanByScope("test");

        assertThat(testPermissions).hasSize(1);
        assertThat(testPermissions.get(0).code()).isEqualTo("test:admin:user:read");
        assertThat(testPermissions.get(0).name()).isEqualTo("测试 / 管理员 / 用户查看");
        assertThat(testPermissions.get(0).scope()).isEqualTo("test");
    }

    @Test
    @DisplayName("scanByScope when 给定 null 则返回无 scope 权限")
    void given_controllersWithPermissions_when_scanByScopeNull_then_returnWithoutScope() {
        List<Permission> permissions = permissionScanner.scanByScope(null);

        assertThat(permissions).isNotEmpty();
        assertThat(permissions).anyMatch(p -> p.code().equals("test:simple:action"));
        assertThat(permissions).noneMatch(p -> "test".equals(p.scope()));
    }
}
