package com.cartisan.test.archunit.fixtures.violation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：@Tag(name) 为空白
 * 违反规则：controllersShouldHaveTag
 */
@Tag(name = "   ")
@RestController
public class BadControllerBlankTagName {

    @Operation(summary = "查询商品")
    @GetMapping
    public String find() {
        return "OK";
    }
}
