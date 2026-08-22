package com.cartisan.test.contract;

import com.cartisan.core.domain.BaseEnum;

/**
 * 契约测试基类验证用 BaseEnum 枚举。
 */
public enum ContractOrderStatus implements BaseEnum<ContractOrderStatus> {
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    CLOSED(9, "已关闭");

    private final Integer code;
    private final String name;

    ContractOrderStatus(Integer code, String name) {
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
