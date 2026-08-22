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

    @Test
    @DisplayName("requireSignatureEndpointsShouldBeDocumented - 合规机机接口应该通过")
    void requireSignatureEndpointsShouldBeDocumented_passes_forCompliantCode() {
        assertThatCode(() ->
            CartisanApiDocumentationRules.requireSignatureEndpointsShouldBeDocumented.check(compliantClasses)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireSignatureEndpointsShouldBeDocumented - 机机接口缺 @Operation(summary) 应该失败")
    void requireSignatureEndpointsShouldBeDocumented_fails_whenSummaryMissing() {
        assertThatThrownBy(() ->
            CartisanApiDocumentationRules.requireSignatureEndpointsShouldBeDocumented.check(violatingClasses)
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("BadSignedApiWithoutSummary")
            .hasMessageContaining("@Operation(summary)");
    }

    @Test
    @DisplayName("requireSignatureEndpointsShouldBeDocumented - 机机接口缺 @ErrorCodes 应该失败")
    void requireSignatureEndpointsShouldBeDocumented_fails_whenErrorCodesMissing() {
        assertThatThrownBy(() ->
            CartisanApiDocumentationRules.requireSignatureEndpointsShouldBeDocumented.check(violatingClasses)
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("BadSignedApiWithoutErrorCodes")
            .hasMessageContaining("@ErrorCodes");
    }

    @Test
    @DisplayName("requireSignatureEndpointsShouldBeDocumented - @ErrorCodes 空数组应该失败")
    void requireSignatureEndpointsShouldBeDocumented_fails_whenErrorCodesEmpty() {
        assertThatThrownBy(() ->
            CartisanApiDocumentationRules.requireSignatureEndpointsShouldBeDocumented.check(violatingClasses)
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("BadSignedApiWithEmptyErrorCodes")
            .hasMessageContaining("@ErrorCodes");
    }

    @Test
    @DisplayName("requireSignatureEndpointsShouldBeDocumented - 非机机接口不受影响")
    void requireSignatureEndpointsShouldBeDocumented_ignores_unsignedEndpoints() {
        // violation 包中普通 Controller 的 handler 缺 @Operation，但不带 @RequireSignature，
        // 本规则不选中（缺 @Operation 的报错来自通用规则，与机机强制无关）
        assertThatCode(() ->
            CartisanApiDocumentationRules.requireSignatureEndpointsShouldBeDocumented.check(
                new ClassFileImporter().importClasses(
                    com.cartisan.test.archunit.fixtures.violation.controller.BadControllerWithoutOperation.class))
        ).doesNotThrowAnyException();
    }
}
