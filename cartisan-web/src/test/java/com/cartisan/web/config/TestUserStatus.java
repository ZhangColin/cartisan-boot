package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;

/**
 * BaseEnum 测试用枚举。
 */
public enum TestUserStatus implements BaseEnum<TestUserStatus> {
    ACTIVE(1, "启用"),
    DISABLED(0, "禁用"),
    PENDING(2, "待审核");

    private final Integer code;
    private final String name;

    TestUserStatus(Integer code, String name) {
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