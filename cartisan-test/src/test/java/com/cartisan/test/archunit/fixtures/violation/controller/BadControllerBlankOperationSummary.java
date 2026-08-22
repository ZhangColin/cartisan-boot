package com.cartisan.test.archunit.fixtures.violation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：@Operation(summary) 为空白
 * 违反规则：handlerMethodsShouldHaveOperationSummary
 */
@Tag(name = "商品")
@RestController
public class BadControllerBlankOperationSummary {

    @Operation(summary = "   ")
    @GetMapping
    public String find() {
        return "OK";
    }
}
