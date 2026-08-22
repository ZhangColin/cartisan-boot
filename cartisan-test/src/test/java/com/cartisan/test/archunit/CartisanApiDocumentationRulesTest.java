package com.cartisan.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CartisanApiDocumentationRules 测试
 *
 * <p>使用 fixtures 验证接口描述规则的有效性。</p>
 */
@DisplayName("CartisanApiDocumentationRules 测试")
class CartisanApiDocumentationRulesTest {

    static final JavaClasses compliantClasses = new ClassFileImporter()
        .importPackages("com.cartisan.test.archunit.fixtures.compliant");

    static final JavaClasses violatingClasses = new ClassFileImporter()
        .importPackages("com.cartisan.test.archunit.fixtures.violation");

    @Test
    @DisplayName("controllersShouldHaveTag - 合规代码应该通过")
    void controllersShouldHaveTag_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanApiDocumentationRules.controllersShouldHaveTag.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("controllersShouldHaveTag - 缺少 @Tag 应该失败")
    void controllersShouldHaveTag_fails_whenTagMissing() {
        assertThatThrownBy(() ->
            CartisanApiDocumentationRules.controllersShouldHaveTag.check(violatingClasses)
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("BadControllerWithoutTag")
            .hasMessageContaining("@Tag");
    }

    @Test
    @DisplayName("controllersShouldHaveTag - name 空白应该失败")
    void controllersShouldHaveTag_fails_whenTagNameBlank() {
        assertThatThrownBy(() ->
            CartisanApiDocumentationRules.controllersShouldHaveTag.check(violatingClasses)
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("BadControllerBlankTagName")
            .hasMessageContaining("@Tag");
    }

    @Test
    @DisplayName("handlerMethodsShouldHaveOperationSummary - 合规代码应该通过")
    void handlerMethodsShouldHaveOperationSummary_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanApiDocumentationRules.handlerMethodsShouldHaveOperationSummary.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handlerMethodsShouldHaveOperationSummary - 缺少 @Operation 应该失败")
    void handlerMethodsShouldHaveOperationSummary_fails_whenOperationMissing() {
        assertThatThrownBy(() ->
            CartisanApiDocumentationRules.handlerMethodsShouldHaveOperationSummary.check(violatingClasses)
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("BadControllerWithoutOperation")
            .hasMessageContaining("@Operation");
    }

    @Test
    @DisplayName("handlerMethodsShouldHaveOperationSummary - summary 空白应该失败")
    void handlerMethodsShouldHaveOperationSummary_fails_whenSummaryBlank() {
        assertThatThrownBy(() ->
            CartisanApiDocumentationRules.handlerMethodsShouldHaveOperationSummary.check(violatingClasses)
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("BadControllerBlankOperationSummary")
            .hasMessageContaining("@Operation");
    }
}
