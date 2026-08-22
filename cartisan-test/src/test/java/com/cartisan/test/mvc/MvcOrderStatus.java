package com.cartisan.test.mvc;

import com.cartisan.core.domain.BaseEnum;

/**
 * {@code @CartisanMvcTest} 验证用 BaseEnum 枚举。
 */
public enum MvcOrderStatus implements BaseEnum<MvcOrderStatus> {
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    CLOSED(9, "已关闭");

    private final Integer code;
    private final String name;

    MvcOrderStatus(Integer code, String name) {
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
