package com.cartisan.test.archunit.fixtures.violation.domain;

import com.cartisan.core.domain.AggregateRoot;

/**
 * 违规：聚合根示例
 * 用于测试 Controller 不应依赖聚合根的规则
 */
public class BadAggregate implements AggregateRoot<BadAggregate, String> {

    private final String name;

    public BadAggregate(String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return null;
    }

    public String getName() {
        return name;
    }
}
