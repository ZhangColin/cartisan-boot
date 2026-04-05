package com.cartisan.test.archunit.fixtures.compliant.domain.aggregate;

/**
 * 合规的聚合根示例
 * - 无 Spring 依赖
 * - 位于 domain.aggregate 包
 */
public class GoodAggregate {

    private final String name;

    public GoodAggregate(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
