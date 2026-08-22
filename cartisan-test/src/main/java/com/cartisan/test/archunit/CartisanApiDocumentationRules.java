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
 * </ul>
 *
 * <p>规则按注解名匹配 springdoc 的 {@code @Tag} / {@code @Operation}
 * （与 MapStruct 规则同规），cartisan-test 不引入 springdoc 编译依赖，
 * 不强制消费服务额外依赖；接口缺描述即测试红。</p>
 */
public class CartisanApiDocumentationRules {

    private static final String TAG_ANNOTATION = "io.swagger.v3.oas.annotations.tags.Tag";
    private static final String OPERATION_ANNOTATION = "io.swagger.v3.oas.annotations.Operation";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String SUMMARY_ATTRIBUTE = "summary";

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
            method -> method.getOwner().isAnnotatedWith(RestController.class)
                && REQUEST_MAPPING_ANNOTATIONS.stream().anyMatch(method::isAnnotatedWith));
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
