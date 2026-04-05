package com.cartisan.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CartisanCodingStandardsRules 的测试类
 */
@DisplayName("CartisanCodingStandardsRules 测试")
class CartisanCodingStandardsRulesTest {

    static final JavaClasses cartisanClasses = new ClassFileImporter()
        .importPackages("com.cartisan");

    static final JavaClasses compliantClasses = new ClassFileImporter()
        .importPackages("com.cartisan.test.archunit.fixtures.compliant");

    static final JavaClasses violatingClasses = new ClassFileImporter()
        .importPackages("com.cartisan.test.archunit.fixtures.violation");

    @Test
    @DisplayName("domainEnumsShouldImplementBaseEnum - cartisan 框架应该通过")
    void domainEnumsShouldImplementBaseEnum_passes_forCartisanFramework() {
        assertThatCode(() ->
            CartisanCodingStandardsRules.domainEnumsShouldImplementBaseEnum.check(cartisanClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("mapstructMappersShouldExtendDomainMapper - 合规代码应该通过")
    void mapstructMappersShouldExtendDomainMapper_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanCodingStandardsRules.mapstructMappersShouldExtendDomainMapper.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("mapstructMappersShouldExtendDomainMapper - 违规代码应该失败")
    void mapstructMappersShouldExtendDomainMapper_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanCodingStandardsRules.mapstructMappersShouldExtendDomainMapper.check(violatingClasses)
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("assignable")
            .hasMessageContaining("DomainMapper");
    }
}
