package com.cartisan.test.archunit.fixtures.violation.domain;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 违规：领域类使用了 Spring 注解
 * 违反规则：domainShouldNotDependOnSpring
 */
public class BadDomainWithSpringDependency {

    @Autowired  // ❌ 领域层不应该有 Spring 注解
    private String someDependency;
}
