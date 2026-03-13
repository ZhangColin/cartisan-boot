package com.cartisan.test.archunit.fixtures.violation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：使用 @Autowired 字段注入
 * 违反规则：noFieldInjection
 */
@RestController
public class BadFieldInjection {

    @Autowired  // ❌ 应该使用构造函数注入
    private String someDependency;
}
