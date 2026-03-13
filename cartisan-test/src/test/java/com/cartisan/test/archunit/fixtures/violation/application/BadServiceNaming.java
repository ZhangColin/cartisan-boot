package com.cartisan.test.archunit.fixtures.violation.application;

import org.springframework.stereotype.Service;

/**
 * 违规：application 包的 @Service 不以 AppService 结尾
 * 违反规则：appServicesShouldBeSuffixed
 */
@Service
public class BadServiceNaming {  // ❌ 应该以 AppService 结尾

}
