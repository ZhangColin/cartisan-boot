package com.cartisan.test.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 编码规范规则 — 验证框架要求的编码规范
 *
 * <p>包含以下规则：</p>
 * <ul>
 *   <li>领域层枚举必须实现 BaseEnum 接口</li>
 * </ul>
 *
 * <p>这些规则专注于可验证的架构约束，避免过于复杂的 DDD 最佳实践检查。</p>
 */
public class CartisanCodingStandardsRules {

    /**
     * 领域层枚举必须实现 BaseEnum
     *
     * <p>确保枚举与 Integer 的自动转换。</p>
     * <p>BaseEnum 接口提供 code/name 映射，是框架枚举处理的基础。</p>
     */
    @ArchTest
    static final ArchRule domainEnumsShouldImplementBaseEnum =
        classes()
            .that()
            .areEnums()
            .and()
            .resideInAPackage("..domain..")
            .should()
            .implement("com.cartisan.core.domain.BaseEnum")
            .because("Domain enums must implement BaseEnum for automatic Integer conversion")
            .allowEmptyShould(true);
}
