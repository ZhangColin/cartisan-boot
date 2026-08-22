package com.cartisan.test.archunit.fixtures.violation.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：@RestController 缺少 @Tag
 * 违反规则：controllersShouldHaveTag
 */
@RestController
public class BadControllerWithoutTag {

    @Operation(summary = "查询商品")
    @GetMapping
    public String find() {
        return "OK";
    }
}
