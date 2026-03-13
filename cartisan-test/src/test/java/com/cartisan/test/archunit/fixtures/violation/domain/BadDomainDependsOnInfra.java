package com.cartisan.test.archunit.fixtures.violation.domain;

import com.cartisan.test.archunit.fixtures.violation.infrastructure.BadDao;

/**
 * 违规：领域类直接依赖基础设施层
 * 违反规则：domainShouldNotDependOnInfrastructure
 */
public class BadDomainDependsOnInfra {

    private BadDao dao;  // ❌ 领域层不应该依赖 infrastructure

    public BadDomainDependsOnInfra(BadDao dao) {
        this.dao = dao;
    }
}
