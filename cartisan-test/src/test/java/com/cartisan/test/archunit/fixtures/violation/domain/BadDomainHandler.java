package com.cartisan.test.archunit.fixtures.violation.domain;

import com.cartisan.core.stereotype.DomainService;

/**
 * 违规：@DomainService 类不以 Service 结尾
 * 违反规则：domainServicesShouldBeSuffixed
 */
@DomainService
public class BadDomainHandler {  // ❌ 应该以 Service 结尾

}
