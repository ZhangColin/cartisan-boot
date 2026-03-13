package com.cartisan.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CartisanProhibitionRules 测试
 *
 * <p>使用 fixtures 验证规则的有效性：</p>
 * <ul>
 *   <li>合规代码通过</li>
 *   <li>违规代码失败</li>
 * </ul>
 */
@DisplayName("CartisanProhibitionRules 测试")
class CartisanProhibitionRulesTest {

    // 合规代码类集
    static final JavaClasses compliantClasses = new ClassFileImporter()
        .importPackages("com.cartisan.test.archunit.fixtures.compliant");

    // 违规代码类集
    static final JavaClasses violatingClasses = new ClassFileImporter()
        .importPackages("com.cartisan.test.archunit.fixtures.violation");

    @Test
    @DisplayName("noFieldInjection - 合规代码应该通过")
    void noFieldInjection_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanProhibitionRules.noFieldInjection.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("noFieldInjection - 违规代码应该失败")
    void noFieldInjection_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanProhibitionRules.noFieldInjection.check(violatingClasses)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("@Autowired")
         .hasMessageContaining("field");
    }

    @Test
    @DisplayName("noJavaUtilDate - 合规代码应该通过")
    void noJavaUtilDate_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanProhibitionRules.noJavaUtilDate.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("noJavaUtilDate - 违规代码应该失败")
    void noJavaUtilDate_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanProhibitionRules.noJavaUtilDate.check(violatingClasses)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("java.util.Date")
         .hasMessageContaining("java.time");
    }

    @Test
    @DisplayName("noFloatingPointForMoney - 合规代码应该通过")
    void noFloatingPointForMoney_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanProhibitionRules.noFloatingPointForMoney.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("noFloatingPointForMoney - 违规代码应该失败")
    void noFloatingPointForMoney_fails_forViolatingCode() {
        assertThatThrownBy(() ->
            CartisanProhibitionRules.noFloatingPointForMoney.check(violatingClasses)
        ).isInstanceOf(AssertionError.class)
         .hasMessageContaining("BigDecimal")
         .hasMessageContaining("price");
    }
}
