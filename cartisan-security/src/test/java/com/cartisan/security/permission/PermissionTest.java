package com.cartisan.security.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Permission 测试")
class PermissionTest {

    @Test
    @DisplayName("给定完整参数 when_of 则创建 Permission")
    void given_fullArgs_when_of_then_createPermission() {
        Permission permission = Permission.of("admin:user:read", "平台管理/用户/查看", "admin");

        assertThat(permission.code()).isEqualTo("admin:user:read");
        assertThat(permission.name()).isEqualTo("平台管理/用户/查看");
        assertThat(permission.scope()).isEqualTo("admin");
    }

    @Test
    @DisplayName("给定空 name when_of 则使用 code 作为 name")
    void given_emptyName_when_of_then_useCodeAsName() {
        Permission permission = Permission.of("admin:user:read", "", "admin");

        assertThat(permission.code()).isEqualTo("admin:user:read");
        assertThat(permission.name()).isEqualTo("admin:user:read");
        assertThat(permission.scope()).isEqualTo("admin");
    }

    @Test
    @DisplayName("给定空 scope when_of 则转为 null")
    void given_emptyScope_when_of_then_convertToNull() {
        Permission permission = Permission.of("admin:user:read", "查看", "");

        assertThat(permission.code()).isEqualTo("admin:user:read");
        assertThat(permission.name()).isEqualTo("查看");
        assertThat(permission.scope()).isNull();
    }

    @Test
    @DisplayName("给定相同 code when_equals 则相等")
    void given_sameCode_when_equals_then_true() {
        Permission p1 = Permission.of("admin:user:read", "name1", "scope1");
        Permission p2 = Permission.of("admin:user:read", "name2", "scope2");

        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }
}
