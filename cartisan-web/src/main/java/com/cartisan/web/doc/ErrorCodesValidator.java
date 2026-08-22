package com.cartisan.web.doc;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动时校验全部 {@link ErrorCodes} 声明的 code 可解析。
 *
 * <p>swagger 契约依赖 code 解析（{@link ErrorCodeOperationCustomizer} 渲染时才查
 * 注册表，属惰性路径），typo 若等到拉取 api-docs 才暴露则太晚——本校验器在单例
 * 装配完成后遍历全部 handler，一次性收集并报出所有不可解析的声明（端点 + code），
 * 应用启动即失败，符合「swagger 为契约正本」的规范强度。</p>
 *
 * @since 0.2.0
 * @see ErrorCodes
 * @see CodeMessageRegistry
 */
public class ErrorCodesValidator implements SmartInitializingSingleton {

    private final RequestMappingHandlerMapping handlerMapping;
    private final CodeMessageRegistry registry;

    public ErrorCodesValidator(RequestMappingHandlerMapping handlerMapping,
                               CodeMessageRegistry registry) {
        this.handlerMapping = handlerMapping;
        this.registry = registry;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<String> violations = new ArrayList<>();
        handlerMapping.getHandlerMethods().values().forEach(handler -> {
            ErrorCodes errorCodes = handler.getMethodAnnotation(ErrorCodes.class);
            if (errorCodes == null) {
                return;
            }
            List<String> unresolved = unresolvedCodes(handler, errorCodes.value());
            if (!unresolved.isEmpty()) {
                violations.add("%s 声明的错误码无法解析：%s".formatted(
                        describe(handler), unresolved));
            }
        });
        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                    "@ErrorCodes 存在未注册的错误码，swagger 契约不完整：\n"
                            + String.join("\n", violations));
        }
    }

    private List<String> unresolvedCodes(HandlerMethod handler, String[] codes) {
        List<String> unresolved = new ArrayList<>();
        for (String code : codes) {
            if (registry.find(code).isEmpty()) {
                unresolved.add(code);
            }
        }
        return unresolved;
    }

    private static String describe(HandlerMethod handler) {
        return "%s#%s".formatted(handler.getBeanType().getSimpleName(), handler.getMethod().getName());
    }
}
