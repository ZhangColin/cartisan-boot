package com.cartisan.data.jpa.integration;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.data.jpa.converter.BaseEnumConverter;

/**
 * 测试用管理员状态枚举。
 */
public enum TestAdminUserStatus implements BaseEnum<TestAdminUserStatus> {
    ACTIVE(1, "激活"),
    DISABLED(0, "禁用");

    private final Integer code;
    private final String name;

    TestAdminUserStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
    }
}
