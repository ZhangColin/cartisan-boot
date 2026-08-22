package com.cartisan.web.doc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明端点可能返回的业务错误码（稳定公开标识，如 {@code PRJ_007}），
 * {@link ErrorCodeOperationCustomizer} 据此把错误码语义渲染进 swagger 端点描述
 * ——错误码清单不再手写文案，与错误码注册表（CodeMessage 枚举）单点同源。
 *
 * <p>code 必须存在于 {@link CodeMessageRegistry}（启动时扫描 CodeMessage 枚举建立
 * code→常量索引；通用 HTTP 错误码 {@code com.cartisan.core.exception.BaseCodeMessage}
 * 恒可解析）。声明了 registry 解析不到的 code，应用启动即失败
 * （{@link ErrorCodesValidator} 报出端点与 code，防 typo）：</p>
 * <pre>{@code
 * @PostMapping("/{id}/stage/approve")
 * @Operation(summary = "门通过（推进；验收门通过即收口）")
 * @ErrorCodes({"PRJ_007", "PRJ_009", "PRJ_010"})
 * public ApiResponse<ProjectDetailResponse> approve(@PathVariable String id) { ... }
 * }</pre>
 *
 * <p>渲染为 description 末尾的「错误码：」块，逐码一行
 * {@code - {httpStatus} {code} — {message}}，声明序、按 code 去重；已有手写
 * description 保留在前。禁止把错误码手写进文案——会随错误码注册表演进漂移。</p>
 *
 * <p>仅在 classpath 存在 springdoc 时生效（无 springdoc 的服务注解 inert）。
 * 机机接口（{@code @RequireSignature}）强制声明本注解的规则见 cartisan-test 的
 * {@code CartisanApiDocumentationRules}。</p>
 *
 * @since 0.2.0
 * @see com.cartisan.core.exception.CodeMessage
 * @see ErrorCodeOperationCustomizer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ErrorCodes {

    /**
     * 端点可能返回的错误码（CodeMessage#code()），声明序即渲染序。
     *
     * @return 错误码数组（空数组等于不渲染）
     */
    String[] value();
}
