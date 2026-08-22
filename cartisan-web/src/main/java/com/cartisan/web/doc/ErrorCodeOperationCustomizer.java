package com.cartisan.web.doc;

import com.cartisan.core.exception.CodeMessage;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 把 {@link ErrorCodes} 声明的错误码渲染进 swagger 端点描述的全局
 * {@link OperationCustomizer}。
 *
 * <p>swagger 是前后端（及服务间）契约正本，端点会返回哪些错误码是契约的一部分；
 * 手写文案会随错误码注册表演进漂移，注解声明 code、由 {@link CodeMessageRegistry}
 * 解析语义则单点同源。</p>
 *
 * <p>渲染为 description 末尾的「错误码：」块：</p>
 * <pre>{@code
 * 错误码：
 * - 409 PRJ_007 — 门禁计数不足
 * - 409 PRJ_009 — 当前阶段无门段
 * }</pre>
 *
 * <p>逐码一行 {@code - {httpStatus} {code} — {message}}，声明序、按 code 去重；
 * 已有手写 description 保留在前；无注解或空声明的端点不受影响。声明了未注册
 * code 的端点在启动阶段即被 {@link ErrorCodesValidator} 拦截，正常不会走到本类。</p>
 *
 * <p>由 {@code CartisanWebAutoConfiguration} 在 classpath 存在 springdoc 时注册为
 * Bean，springdoc 自动收集容器内全部 OperationCustomizer，对所有分组生效。</p>
 *
 * @since 0.2.0
 * @see ErrorCodes
 */
public class ErrorCodeOperationCustomizer implements OperationCustomizer {

    private final CodeMessageRegistry registry;

    public ErrorCodeOperationCustomizer(CodeMessageRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ErrorCodes errorCodes = handlerMethod.getMethodAnnotation(ErrorCodes.class);
        if (errorCodes == null || errorCodes.value().length == 0) {
            return operation;
        }
        operation.setDescription(
                withErrorCodeBlock(operation.getDescription(), errorCodes.value(), registry));
        return operation;
    }

    /**
     * 把错误码块并入已有描述：空描述直接用块；已含同块（重复 customize）不重复追加。
     */
    private static String withErrorCodeBlock(String description, String[] codes,
                                             CodeMessageRegistry registry) {
        String block = renderBlock(codes, registry);
        if (description == null || description.isBlank()) {
            return block;
        }
        return description.contains(block) ? description : description + "\n\n" + block;
    }

    /**
     * 渲染错误码块：声明序、按 code 去重（同 code 重复声明只渲染首个）。
     */
    private static String renderBlock(String[] codes, CodeMessageRegistry registry) {
        Set<String> seen = new LinkedHashSet<>();
        StringBuilder block = new StringBuilder("错误码：");
        for (String code : codes) {
            if (seen.add(code)) {
                CodeMessage message = registry.require(code);
                block.append("\n- ").append(message.httpStatus())
                        .append(' ').append(message.code())
                        .append(" — ").append(message.message());
            }
        }
        return block.toString();
    }
}
