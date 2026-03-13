package com.cartisan.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CartisanNamingRules 测试
 *
 * <p>使用 fixtures 验证命名规则的有效性。</p>
 */
@DisplayName("CartisanNamingRules 测试")
class CartisanNamingRulesTest {

    static final JavaClasses compliantClasses = new ClassFileImporter()
        .importPackages("com.cartisan.test.archunit.fixtures.compliant");

    static final JavaClasses violatingClasses = new ClassFileImporter()
        .importPackages("com.cartisan.test.archunit.fixtures.violation");

    @Test
    @DisplayName("controllersShouldBeSuffixed - 合规代码应该通过")
    void controllersShouldBeSuffixed_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanNamingRules.controllersShouldBeSuffixed.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("controllersShouldBeSuffixed - 违规代码应该失败")
    void controllersShouldBeSuffixed_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanNamingRules.controllersShouldBeSuffixed.check(violatingClasses)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("Controller");
    }

    @Test
    @DisplayName("appServicesShouldBeSuffixed - 合规代码应该通过")
    void appServicesShouldBeSuffixed_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanNamingRules.appServicesShouldBeSuffixed.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("appServicesShouldBeSuffixed - 违规代码应该失败")
    void appServicesShouldBeSuffixed_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanNamingRules.appServicesShouldBeSuffixed.check(violatingClasses)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("AppService");
    }

    @Test
    @DisplayName("domainServicesShouldBeSuffixed - 合规代码应该通过")
    void domainServicesShouldBeSuffixed_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanNamingRules.domainServicesShouldBeSuffixed.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("domainServicesShouldBeSuffixed - 违规代码应该失败")
    void domainServicesShouldBeSuffixed_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanNamingRules.domainServicesShouldBeSuffixed.check(violatingClasses)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("Service");
    }

    @Test
    @DisplayName("repositoriesShouldBeSuffixed - 合规代码应该通过")
    void repositoriesShouldBeSuffixed_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanNamingRules.repositoriesShouldBeSuffixed.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("repositoriesShouldBeSuffixed - 违规代码应该失败")
    void repositoriesShouldBeSuffixed_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanNamingRules.repositoriesShouldBeSuffixed.check(violatingClasses)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("Repository");
    }
}
