package com.cartisan.test.archunit.fixtures.violation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：请求映射方法缺少 @Operation
 * 违反规则：handlerMethodsShouldHaveOperationSummary
 */
@Tag(name = "商品")
@RestController
public class BadControllerWithoutOperation {

    @GetMapping
    public String find() {
        return "OK";
    }
}
