package com.cartisan.web.doc;

import com.cartisan.core.exception.CodeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ErrorCodesValidator} 的启动校验测试。
 *
 * <p>全部 handler 的 @ErrorCodes 声明可解析则静默通过；存在不可解析 code 时
 * 一次性报出全部违例（端点 + code 清单），应用启动失败。</p>
 */
class ErrorCodesValidatorTest {

    enum ValidatorMessage implements CodeMessage {
        VLD_001(400, "VLD_001", "可解析错误码");

        private final int httpStatus;
        private final String code;
        private final String message;

        ValidatorMessage(int httpStatus, String code, String message) {
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

    static class Endpoints {

        @ErrorCodes({"VLD_001"})
        @SuppressWarnings("unused")
        void resolvable() {
        }

        @ErrorCodes({"VLD_001", "VLD_404", "VLD_500"})
        @SuppressWarnings("unused")
        void partiallyUnresolvable() {
        }

        @SuppressWarnings("unused")
        void notAnnotated() {
        }
    }

    @Test
    void should_pass_silently_when_all_declared_codes_resolvable() {
        ErrorCodesValidator validator = validatorWith(
                CodeMessageRegistry.of(ValidatorMessage.values()),
                method("resolvable"), method("notAnnotated"));

        assertThatCode(validator::afterSingletonsInstantiated)
                .doesNotThrowAnyException();
    }

    @Test
    void should_fail_listing_endpoint_and_each_unresolved_code() {
        // 注册表只含 VLD_001：resolvable 通过，partiallyUnresolvable 的 VLD_404/VLD_500 报出
        ErrorCodesValidator validator = validatorWith(
                CodeMessageRegistry.of(ValidatorMessage.VLD_001),
                method("resolvable"), method("partiallyUnresolvable"), method("notAnnotated"));

        assertThatThrownBy(validator::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Endpoints#partiallyUnresolvable")
                .hasMessageContaining("[VLD_404, VLD_500]")
                .hasMessageNotContaining("Endpoints#resolvable");
    }

    @Test
    void should_ignore_handlers_without_annotation() {
        ErrorCodesValidator validator = validatorWith(
                CodeMessageRegistry.of(ValidatorMessage.values()),
                method("notAnnotated"));

        assertThatCode(validator::afterSingletonsInstantiated)
                .doesNotThrowAnyException();
    }

    // ---------- 支撑 ----------

    private static Method method(String name) {
        return Arrays.stream(Endpoints.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static ErrorCodesValidator validatorWith(CodeMessageRegistry registry, Method... methods) {
        return new ErrorCodesValidator(handlerMappingOf(methods), registry);
    }

    private static RequestMappingHandlerMapping handlerMappingOf(Method... methods) {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = new HashMap<>();
        for (Method method : methods) {
            try {
                handlerMethods.put(mock(RequestMappingInfo.class),
                        new HandlerMethod(new Endpoints(), method));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        when(mapping.getHandlerMethods()).thenReturn(handlerMethods);
        return mapping;
    }
}
