package com.cartisan.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * CartisanCodingStandardsRules 的测试类
 */
@DisplayName("CartisanCodingStandardsRules 测试")
class CartisanCodingStandardsRulesTest {

    static final JavaClasses cartisanClasses = new ClassFileImporter()
        .importPackages("com.cartisan");

    @Test
    @DisplayName("domainEnumsShouldImplementBaseEnum - cartisan 框架应该通过")
    void domainEnumsShouldImplementBaseEnum_passes_forCartisanFramework() {
        assertThatCode(() ->
            CartisanCodingStandardsRules.domainEnumsShouldImplementBaseEnum.check(cartisanClasses)
        ).doesNotThrowAnyException();
    }
}
