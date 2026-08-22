package com.cartisan.web.doc;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.CodeMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CodeMessageRegistry} 的扫描与解析测试。
 */
class CodeMessageRegistryTest {

    /** 唯一 code 的测试枚举（放在 com.cartisan.web.doc 下，命中默认扫描包）。 */
    enum ScanTargetMessage implements CodeMessage {
        SCAN_A(400, "SCAN_A_001", "扫描枚举 A");

        private final int httpStatus;
        private final String code;
        private final String message;

        ScanTargetMessage(int httpStatus, String code, String message) {
            this.httpStatus = httpStatus;
            this.code = code;
            this.message = message;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public String message() {
            return message;
        }

        @Override
        public int httpStatus() {
            return httpStatus;
        }
    }

    /** 与 ScanTargetMessage 同 code 的冲突枚举见 io.cartisan.web.doc.ConflictingMessage（默认扫描包外）。 */

    @Test
    void should_find_code_message_enums_by_scanning_package() {
        CodeMessageRegistry registry = CodeMessageRegistry.scan("com.cartisan.web.doc");

        assertThat(registry.find("SCAN_A_001")).containsSame(ScanTargetMessage.SCAN_A);
    }

    @Test
    void should_fall_back_to_default_packages_when_none_configured() {
        CodeMessageRegistry registry = CodeMessageRegistry.scan();

        assertThat(registry.find("SCAN_A_001")).containsSame(ScanTargetMessage.SCAN_A);
    }

    @Test
    void should_always_preload_base_code_message_even_with_foreign_scan_package() {
        CodeMessageRegistry registry = CodeMessageRegistry.scan("com.example.nosuch");

        assertThat(registry.find("RESOURCE_NOT_FOUND")).contains(BaseCodeMessage.RESOURCE_NOT_FOUND);
    }

    @Test
    void should_resolve_code_with_semantics_via_require() {
        CodeMessageRegistry registry = CodeMessageRegistry.of(ScanTargetMessage.values());

        CodeMessage message = registry.require("SCAN_A_001");

        assertThat(message.message()).isEqualTo("扫描枚举 A");
        assertThat(message.httpStatus()).isEqualTo(400);
    }

    @Test
    void should_fail_with_code_name_when_code_unregistered() {
        CodeMessageRegistry registry = CodeMessageRegistry.of(ScanTargetMessage.values());

        assertThatThrownBy(() -> registry.require("SCAN_A_999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SCAN_A_999")
                .hasMessageContaining("cartisan.web.error-codes.scan-packages");
    }

    @Test
    void should_hint_actual_scan_packages_when_customized() {
        CodeMessageRegistry registry = CodeMessageRegistry.scan("com.aieducenter.aiplatform");

        assertThatThrownBy(() -> registry.require("SCAN_A_999"))
                .hasMessageContaining("扫描包 com.aieducenter.aiplatform");
    }

    @Test
    void should_fail_when_same_code_registered_twice_from_different_constants() {
        assertThatThrownBy(() -> CodeMessageRegistry.of(
                ScanTargetMessage.SCAN_A, io.cartisan.web.doc.ConflictingMessage.SCAN_A))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SCAN_A_001")
                .hasMessageContaining("ScanTargetMessage.SCAN_A")
                .hasMessageContaining("ConflictingMessage.SCAN_A");
    }

    @Test
    void should_be_idempotent_when_same_constant_registered_twice() {
        CodeMessageRegistry registry = CodeMessageRegistry.of(
                ScanTargetMessage.SCAN_A, ScanTargetMessage.SCAN_A);

        assertThat(registry.find("SCAN_A_001")).containsSame(ScanTargetMessage.SCAN_A);
    }
}
