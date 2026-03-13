package com.cartisan.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CartisanLayeringRules 测试
 *
 * <p>使用 fixtures 验证分层规则的有效性。</p>
 */
@DisplayName("CartisanLayeringRules 测试")
class CartisanLayeringRulesTest {

    static final JavaClasses compliantClasses = new ClassFileImporter()
        .importPackages("com.cartisan.test.archunit.fixtures.compliant");

    static final JavaClasses violatingClasses = new ClassFileImporter()
        .importPackages("com.cartisan.test.archunit.fixtures.violation");

    @Test
    @DisplayName("domainShouldNotDependOnInfrastructure - 合规代码应该通过")
    void domainShouldNotDependOnInfrastructure_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanLayeringRules.domainShouldNotDependOnInfrastructure.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("domainShouldNotDependOnInfrastructure - 违规代码应该失败")
    void domainShouldNotDependOnInfrastructure_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanLayeringRules.domainShouldNotDependOnInfrastructure.check(violatingClasses)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("domain")
         .hasMessageContaining("infrastructure");
    }

    @Test
    @DisplayName("domainShouldNotDependOnSpring - 合规代码应该通过")
    void domainShouldNotDependOnSpring_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanLayeringRules.domainShouldNotDependOnSpring.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("domainShouldNotDependOnSpring - 违规代码应该失败")
    void domainShouldNotDependOnSpring_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanLayeringRules.domainShouldNotDependOnSpring.check(violatingClasses)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("domain")
         .hasMessageContaining("org.springframework");
    }

    @Test
    @DisplayName("controllersShouldOnlyDependOnApplication - 合规代码应该通过")
    void controllersShouldOnlyDependOnApplication_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanLayeringRules.controllersShouldOnlyDependOnApplication.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("controllersShouldOnlyDependOnApplication - 违规代码应该失败")
    void controllersShouldOnlyDependOnApplication_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanLayeringRules.controllersShouldOnlyDependOnApplication.check(violatingClasses)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("controller")
         .hasMessageContaining("domain");
    }

    @Test
    @DisplayName("applicationShouldNotAccessDatabaseDirectly - 合规代码应该通过")
    void applicationShouldNotAccessDatabaseDirectly_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanLayeringRules.applicationShouldNotAccessDatabaseDirectly.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("applicationShouldNotAccessDatabaseDirectly - 违规代码应该失败")
    void applicationShouldNotAccessDatabaseDirectly_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanLayeringRules.applicationShouldNotAccessDatabaseDirectly.check(violatingClasses)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("application")
         .hasMessageContaining("EntityManager");
    }
}
