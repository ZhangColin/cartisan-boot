package com.cartisan.test.archunit.fixtures.violation.controller;

import com.cartisan.test.archunit.fixtures.compliant.domain.GoodEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：Controller 直接依赖领域层
 * 违反规则：controllersShouldOnlyDependOnApplication
 */
@RestController
public class BadControllerCallsDomain {

    private GoodEntity entity;  // ❌ Controller 只应该依赖 application 层

    public BadControllerCallsDomain(GoodEntity entity) {
        this.entity = entity;
    }
}
