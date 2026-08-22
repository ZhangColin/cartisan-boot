package com.cartisan.web.doc;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.CodeMessage;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ErrorCodeOperationCustomizer} 的渲染规则测试。
 *
 * <p>@ErrorCodes 声明的错误码以「错误码：」块追加进 operation description，
 * 逐码一行 {@code - {httpStatus} {code} — {message}}，声明序、按 code 去重。</p>
 */
class ErrorCodeOperationCustomizerTest {

    /** 测试用业务错误码枚举（CodeMessage 形，对齐消费方 XxxMessage 注册表）。 */
    enum TestOrderMessage implements CodeMessage {
        ORDER_NOT_FOUND(404, "ORD_001", "订单不存在"),
        ORDER_STATUS_CONFLICT(409, "ORD_002", "订单状态冲突"),
        ORDER_LOCKED(409, "ORD_003", "订单已锁定");

        private final int httpStatus;
        private final String code;
        private final String message;

        TestOrderMessage(int httpStatus, String code, String message) {
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

    /** 承载 @ErrorCodes 声明的 fixture 方法宿主。 */
    static class Endpoints {

        @ErrorCodes({"ORD_001", "ORD_002"})
        @SuppressWarnings("unused")
        void annotated() {
        }

        @ErrorCodes({"ORD_001", "ORD_001", "ORD_002", "ORD_003"})
        @SuppressWarnings("unused")
        void duplicatedDeclaration() {
        }

        @ErrorCodes({})
        @SuppressWarnings("unused")
        void emptyDeclaration() {
        }

        @SuppressWarnings("unused")
        void notAnnotated() {
        }
    }

    private final ErrorCodeOperationCustomizer customizer = new ErrorCodeOperationCustomizer(
            CodeMessageRegistry.of(TestOrderMessage.values()));

    @Test
    void should_return_operation_unchanged_when_method_not_annotated() throws Exception {
        Operation operation = new Operation().description("手写描述");

        customizer.customize(operation, handlerMethod("notAnnotated"));

        assertThat(operation.getDescription()).isEqualTo("手写描述");
    }

    @Test
    void should_render_error_code_block_when_description_absent() throws Exception {
        Operation operation = new Operation();

        customizer.customize(operation, handlerMethod("annotated"));

        assertThat(operation.getDescription()).isEqualTo("""
                错误码：
                - 404 ORD_001 — 订单不存在
                - 409 ORD_002 — 订单状态冲突""");
    }

    @Test
    void should_append_block_after_existing_description() throws Exception {
        Operation operation = new Operation().description("创建时间倒序，不合法取值 400。");

        customizer.customize(operation, handlerMethod("annotated"));

        assertThat(operation.getDescription()).isEqualTo("""
                创建时间倒序，不合法取值 400。

                错误码：
                - 404 ORD_001 — 订单不存在
                - 409 ORD_002 — 订单状态冲突""");
    }

    @Test
    void should_keep_declaration_order_and_dedup_by_code() throws Exception {
        Operation operation = new Operation();

        customizer.customize(operation, handlerMethod("duplicatedDeclaration"));

        assertThat(operation.getDescription()).isEqualTo("""
                错误码：
                - 404 ORD_001 — 订单不存在
                - 409 ORD_002 — 订单状态冲突
                - 409 ORD_003 — 订单已锁定""");
    }

    @Test
    void should_not_render_anything_when_no_codes_declared() throws Exception {
        Operation operation = new Operation().description("手写描述");

        customizer.customize(operation, handlerMethod("emptyDeclaration"));

        assertThat(operation.getDescription()).isEqualTo("手写描述");
    }

    @Test
    void should_not_duplicate_block_when_customize_runs_twice() throws Exception {
        Operation operation = new Operation().description("手写描述");

        customizer.customize(operation, handlerMethod("annotated"));
        customizer.customize(operation, handlerMethod("annotated"));

        assertThat(operation.getDescription()).isEqualTo("""
                手写描述

                错误码：
                - 404 ORD_001 — 订单不存在
                - 409 ORD_002 — 订单状态冲突""");
    }

    @Test
    void should_fail_fast_when_code_not_in_registry() throws Exception {
        Operation operation = new Operation();
        ErrorCodeOperationCustomizer registryWithoutOrderCodes = new ErrorCodeOperationCustomizer(
                CodeMessageRegistry.of(BaseCodeMessage.RESOURCE_NOT_FOUND));

        assertThatThrownBy(() ->
                registryWithoutOrderCodes.customize(operation, handlerMethod("annotated")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ORD_001");
    }

    private HandlerMethod handlerMethod(String name) throws NoSuchMethodException {
        Method method = Endpoints.class.getDeclaredMethod(name);
        return new HandlerMethod(new Endpoints(), method);
    }
}
