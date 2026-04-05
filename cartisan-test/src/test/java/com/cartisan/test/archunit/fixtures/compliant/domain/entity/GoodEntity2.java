package com.cartisan.test.archunit.fixtures.compliant.domain.entity;

/**
 * 合规的实体示例
 * - 无 Spring 依赖
 * - 位于 domain.entity 包
 */
public class GoodEntity2 {

    private final String value;

    public GoodEntity2(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
