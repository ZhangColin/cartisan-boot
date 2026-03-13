package com.cartisan.test.archunit.fixtures.violation.infrastructure;

import org.springframework.stereotype.Repository;

/**
 * 违规：@Repository 不以 Repository 结尾
 * 违反规则：repositoriesShouldBeSuffixed
 */
@Repository
public class BadDao {  // ❌ 应该以 Repository 结尾

}
