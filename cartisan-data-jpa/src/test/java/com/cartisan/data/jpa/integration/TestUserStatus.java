package com.cartisan.data.jpa.integration;

import com.cartisan.core.domain.BaseEnum;

/**
 * 测试用户状态枚举。
 */
public enum TestUserStatus implements BaseEnum<TestUserStatus> {
    ACTIVE(1, "激活"),
    INACTIVE(0, "未激活");

    private final Integer code;
    private final String name;

    TestUserStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}