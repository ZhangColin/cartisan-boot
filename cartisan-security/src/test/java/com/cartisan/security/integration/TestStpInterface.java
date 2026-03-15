package com.cartisan.security.integration;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用 StpInterface 实现。
 * <p>
 * 提供简单的角色/权限存储，用于集成测试。
 * </p>
 */
@Component
public class TestStpInterface implements StpInterface {

    /**
     * 获取权限列表。
     * <p>
     * 从 Session 中读取 "permissions" 键。
     * </p>
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) cn.dev33.satoken.stp.StpUtil
                .getSession()
                .get("permissions");
        return permissions != null ? permissions : new ArrayList<>();
    }

    /**
     * 获取角色列表。
     * <p>
     * 从 Session 中读取 "roles" 键。
     * </p>
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) cn.dev33.satoken.stp.StpUtil
                .getSession()
                .get("roles");
        return roles != null ? roles : new ArrayList<>();
    }
}
