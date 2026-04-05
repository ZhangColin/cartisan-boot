package com.cartisan.test.archunit.fixtures.violation.controller;

import com.cartisan.test.archunit.fixtures.compliant.domain.aggregate.GoodAggregate;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：Controller 直接依赖聚合根
 * 违反规则：controllersShouldOnlyDependOnApplication
 */
@RestController
public class BadControllerCallsDomain {

    private GoodAggregate aggregate;  // ❌ Controller 只应该依赖 application 层

    public BadControllerCallsDomain(GoodAggregate aggregate) {
        this.aggregate = aggregate;
    }
}
