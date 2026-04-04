package com.cartisan.test.archunit.fixtures.violation.controller;

import com.cartisan.test.archunit.fixtures.violation.domain.BadAggregate;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：Controller 直接依赖聚合根
 * 违反规则：controllersShouldNotDependOnAggregates
 */
@RestController
public class BadControllerDependsOnAggregate {

    private final BadAggregate aggregate;  // ❌ Controller 不应该直接依赖聚合根

    public BadControllerDependsOnAggregate(BadAggregate aggregate) {
        this.aggregate = aggregate;
    }
}
