package com.cartisan.test.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * API 文档规则 — 验证 REST 接口的 OpenAPI 描述完整性
 *
 * <p>「接口上必须有 swagger 描述」不再依赖口头约定：</p>
 * <ul>
 *   <li>@RestController 类必须有非空 {@code @Tag(name)}</li>
 *   <li>@RestController 类内每个请求映射方法必须有非空 {@code @Operation(summary)}</li>
 *   <li>机机接口（@RequireSignature，类或方法级）的 handler 还必须声明
 *       {@code @ErrorCodes}（至少一个错误码）——swagger 是服务间契约正本，
 *       调用方按文档编程，描述缺失即测试红（构建失败）</li>
 * </ul>
 *
 * <p>规则按注解名匹配 springdoc 与 cartisan-openapi 的注解
 * （与 MapStruct 规则同规），cartisan-test 不引入 springdoc / cartisan-openapi
 * 编译依赖，不强制消费服务额外依赖；接口缺描述即测试红。</p>
 */
public class CartisanApiDocumentationRules {

    private static final String TAG_ANNOTATION = "io.swagger.v3.oas.annotations.tags.Tag";
    private static final String OPERATION_ANNOTATION = "io.swagger.v3.oas.annotations.Operation";
    private static final String ERROR_CODES_ANNOTATION = "com.cartisan.web.doc.ErrorCodes";
    private static final String REQUIRE_SIGNATURE_ANNOTATION =
        "com.cartisan.openapi.annotation.RequireSignature";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String SUMMARY_ATTRIBUTE = "summary";
    private static final String VALUE_ATTRIBUTE = "value";

    private static final Set<Class<? extends Annotation>> REQUEST_MAPPING_ANNOTATIONS = Set.of(
        GetMapping.class, PostMapping.class, PutMapping.class,
        DeleteMapping.class, PatchMapping.class, RequestMapping.class);

    /**
     * @RestController 类必须有非空 @Tag(name)
     *
     * <p>Tag 是 swagger 分组的入口，缺失或空白会让接口在文档中归入默认组，
     * 前后端契约不自描述。</p>
     */
    @ArchTest
    static final ArchRule controllersShouldHaveTag =
        classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should(haveTagWithNonBlankName())
            .because("REST controllers must have a @Tag with non-blank name so the OpenAPI contract stays self-describing")
            .allowEmptyShould(true);

    /**
     * 请求映射方法必须有非空 @Operation(summary)
     *
     * <p>覆盖 @GetMapping/@PostMapping/@PutMapping/@DeleteMapping/@PatchMapping
     * 与方法级 @RequestMapping；类级 @RequestMapping 只声明基础路径，不作 handler，
     * 不在本规则范围内。</p>
     */
    @ArchTest
    static final ArchRule handlerMethodsShouldHaveOperationSummary =
        methods()
            .that(areHandlerMethodsOfRestControllers())
            .should(haveNonBlankOperationSummary())
            .because("every REST endpoint must have a non-blank @Operation(summary) so the OpenAPI contract stays self-describing")
            .allowEmptyShould(true);

    /**
     * 机机接口（@RequireSignature）的 handler 必须有完整 swagger 描述
     *
     * <p>服务间签名调用的接口，swagger 是唯一契约正本，调用方（含代码生成器）
     * 按文档编程：非空 {@code @Operation(summary)} 描述行为，
     * {@code @ErrorCodes}（至少一个错误码）声明错误面——错误码语义由
     * cartisan-web 的 ErrorCodeOperationCustomizer 渲染进端点描述。
     * 缺失即测试红，构建失败。</p>
     *
     * <p>@RequireSignature 标注在类或方法上均生效（与验签语义一致）。</p>
     */
    @ArchTest
    static final ArchRule requireSignatureEndpointsShouldBeDocumented =
        methods()
            .that(areHandlerMethodsOfRequireSignatureEndpoints())
            .should(beFullyDocumentedSignedEndpoint())
            .because("machine-to-machine endpoints (@RequireSignature) must declare a non-blank @Operation(summary) "
                + "and at least one @ErrorCodes entry — swagger is the contract of record between services")
            .allowEmptyShould(true);

    private static ArchCondition<JavaClass> haveTagWithNonBlankName() {
        return new ArchCondition<>("be annotated with a @Tag with non-blank name") {
            @Override
            public void check(JavaClass controller, ConditionEvents events) {
                Optional<JavaAnnotation<JavaClass>> tag = controller.tryGetAnnotationOfType(TAG_ANNOTATION);
                if (tag.isEmpty()) {
                    events.add(SimpleConditionEvent.violated(controller,
                        "%s has no @Tag annotation in %s".formatted(
                            controller.getDescription(), controller.getSourceCodeLocation())));
                    return;
                }
                if (hasBlankAttribute(tag.get(), NAME_ATTRIBUTE)) {
                    events.add(SimpleConditionEvent.violated(controller,
                        "%s has a @Tag with blank name in %s".formatted(
                            controller.getDescription(), controller.getSourceCodeLocation())));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> haveNonBlankOperationSummary() {
        return new ArchCondition<>("have a non-blank @Operation(summary)") {
            @Override
            public void check(JavaMethod handler, ConditionEvents events) {
                Optional<? extends JavaAnnotation<?>> operation =
                    handler.tryGetAnnotationOfType(OPERATION_ANNOTATION);
                if (operation.isEmpty()) {
                    events.add(SimpleConditionEvent.violated(handler,
                        "%s has no @Operation annotation in %s".formatted(
                            handler.getDescription(), handler.getSourceCodeLocation())));
                    return;
                }
                if (hasBlankAttribute(operation.get(), SUMMARY_ATTRIBUTE)) {
                    events.add(SimpleConditionEvent.violated(handler,
                        "%s has a blank @Operation(summary) in %s".formatted(
                            handler.getDescription(), handler.getSourceCodeLocation())));
                }
            }
        };
    }

    /**
     * 选中 @RestController 类中标注了任意请求映射注解的方法
     */
    private static DescribedPredicate<JavaMethod> areHandlerMethodsOfRestControllers() {
        return DescribedPredicate.describe("are annotated with a Spring request mapping annotation and declared in a @RestController",
            method -> method.getOwner().isAnnotatedWith(RestController.class) && isHandlerMethod(method));
    }

    /**
     * 选中机机接口的 handler：请求映射方法自身或所属类标注了 @RequireSignature
     */
    private static DescribedPredicate<JavaMethod> areHandlerMethodsOfRequireSignatureEndpoints() {
        return DescribedPredicate.describe("are request mapping methods of endpoints marked @RequireSignature (on method or owner)",
            method -> isHandlerMethod(method)
                && (method.isAnnotatedWith(REQUIRE_SIGNATURE_ANNOTATION)
                    || method.getOwner().isAnnotatedWith(REQUIRE_SIGNATURE_ANNOTATION)));
    }

    /**
     * 标注了任意请求映射注解的方法（handler）
     */
    private static boolean isHandlerMethod(JavaMethod method) {
        return REQUEST_MAPPING_ANNOTATIONS.stream().anyMatch(method::isAnnotatedWith);
    }

    /**
     * 机机接口描述完整性：非空 @Operation(summary) + @ErrorCodes 至少一个错误码，
     * 各缺失项分别报出（一次指出全部问题）
     */
    private static ArchCondition<JavaMethod> beFullyDocumentedSignedEndpoint() {
        return new ArchCondition<>("be fully documented (non-blank @Operation(summary) and non-empty @ErrorCodes)") {
            @Override
            public void check(JavaMethod handler, ConditionEvents events) {
                if (hasBlankOperationSummary(handler)) {
                    events.add(SimpleConditionEvent.violated(handler,
                        "%s lacks a non-blank @Operation(summary) in %s".formatted(
                            handler.getDescription(), handler.getSourceCodeLocation())));
                }
                if (declaresNoErrorCodes(handler)) {
                    events.add(SimpleConditionEvent.violated(handler,
                        "%s lacks a non-empty @ErrorCodes in %s".formatted(
                            handler.getDescription(), handler.getSourceCodeLocation())));
                }
            }
        };
    }

    private static boolean hasBlankOperationSummary(JavaMethod handler) {
        Optional<? extends JavaAnnotation<?>> operation = handler.tryGetAnnotationOfType(OPERATION_ANNOTATION);
        return operation.isEmpty() || hasBlankAttribute(operation.get(), SUMMARY_ATTRIBUTE);
    }

    /**
     * 未标注 @ErrorCodes，或标注了但 value 为空数组
     */
    private static boolean declaresNoErrorCodes(JavaMethod handler) {
        return handler.tryGetAnnotationOfType(ERROR_CODES_ANNOTATION)
            .map(CartisanApiDocumentationRules::hasEmptyValueAttribute)
            .orElse(true);
    }

    /**
     * 注解 value 属性为空（缺失、空数组或空集合——ArchUnit 对数组注解成员
     * 的表示形态随版本有异，两种都兼容）
     */
    private static boolean hasEmptyValueAttribute(JavaAnnotation<?> annotation) {
        Object value = annotation.get(VALUE_ATTRIBUTE).orElse(null);
        if (value instanceof String[] codes) {
            return codes.length == 0;
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return true;
    }

    /**
     * 注解属性缺失（用默认值）或为空白字符串
     */
    private static boolean hasBlankAttribute(JavaAnnotation<?> annotation, String attribute) {
        return annotation.get(attribute)
            .map(String::valueOf)
            .map(String::isBlank)
            .orElse(true);
    }
}
