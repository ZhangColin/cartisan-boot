package com.cartisan.test.archunit.fixtures.violation.controller;

import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：@RestController 不以 Controller 结尾
 * 违反规则：controllersShouldBeSuffixed
 */
@RestController
public class BadControllerNaming {  // ❌ 应该以 Controller 结尾

}
