package com.cartisan.core.stereotype;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stereotype 注解元数据验证测试。
 *
 * <p>本测试验证注解的元注解配置正确性，防止误改导致 ArchUnit 规则失效。</p>
 *
 * <h2>测试策略</h2>
 * <ul>
 *   <li>守护元注解契约：@Retention(RUNTIME)、@Target</li>
 *   <li>守护枚举完整性：防止新增值时没有对应规则</li>
 * </ul>
 */
@DisplayName("Stereotype 注解元数据验证")
class StereotypeAnnotationsTest {

    // ========== @Retention(RUNTIME) 验证 ==========

    @ParameterizedTest(name = "{0} 应有 @Retention(RUNTIME)")
    @MethodSource("allStereotypeAnnotations")
    @DisplayName("所有架构注解应保留在运行时")
    void stereotypeAnnotations_shouldBeRetainedAtRuntime(Class<?> annotation) {
        Retention retention = annotation.getAnnotation(Retention.class);
        assertThat(retention)
                .as("@Retention 注解存在")
                .isNotNull();

        assertThat(retention.value())
                .as("@Retention 应为 RUNTIME，供 ArchUnit 反射读取")
                .isEqualTo(RetentionPolicy.RUNTIME);
    }

    static Stream<Arguments> allStereotypeAnnotations() {
        return Stream.of(
                Arguments.of(Named.of("@BoundedContext", BoundedContext.class)),
                Arguments.of(Named.of("@Aggregate", Aggregate.class)),
                Arguments.of(Named.of("@DomainService", DomainService.class)),
                Arguments.of(Named.of("@Port", Port.class)),
                Arguments.of(Named.of("@Adapter", Adapter.class))
        );
    }

    // ========== @Target 验证 ==========

    @Test
    @DisplayName("@BoundedContext 应只标注在包上")
    void boundedContext_shouldTargetPackageOnly() {
        Target target = BoundedContext.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();

        ElementType[] elementTypes = target.value();
        assertThat(elementTypes)
                .as("@BoundedContext 应只允许 PACKAGE 目标")
                .containsExactly(ElementType.PACKAGE);
    }

    @Test
    @DisplayName("@Aggregate 应标注在类型上")
    void aggregate_shouldTargetType() {
        Target target = Aggregate.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();

        ElementType[] elementTypes = target.value();
        assertThat(elementTypes)
                .as("@Aggregate 应允许 TYPE 目标")
                .containsExactly(ElementType.TYPE);
    }

    @Test
    @DisplayName("@DomainService 应标注在类型上")
    void domainService_shouldTargetType() {
        Target target = DomainService.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();

        ElementType[] elementTypes = target.value();
        assertThat(elementTypes)
                .as("@DomainService 应允许 TYPE 目标")
                .containsExactly(ElementType.TYPE);
    }

    @Test
    @DisplayName("@Port 应标注在类型上")
    void port_shouldTargetType() {
        Target target = Port.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();

        ElementType[] elementTypes = target.value();
        assertThat(elementTypes)
                .as("@Port 应允许 TYPE 目标")
                .containsExactly(ElementType.TYPE);
    }

    @Test
    @DisplayName("@Adapter 应标注在类型上")
    void adapter_shouldTargetType() {
        Target target = Adapter.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();

        ElementType[] elementTypes = target.value();
        assertThat(elementTypes)
                .as("@Adapter 应允许 TYPE 目标")
                .containsExactly(ElementType.TYPE);
    }

    // ========== 枚举完整性验证 ==========

    @Test
    @DisplayName("SubDomain 应有且仅有三个值")
    void subDomain_shouldHaveExactlyThreeValues() {
        SubDomain[] values = SubDomain.values();
        Set<SubDomain> valueSet = new HashSet<>();
        for (SubDomain value : values) {
            valueSet.add(value);
        }

        assertThat(valueSet)
                .as("SubDomain 应包含 CORE, SUPPORTING, GENERIC 三个值")
                .containsExactlyInAnyOrder(
                        SubDomain.CORE,
                        SubDomain.SUPPORTING,
                        SubDomain.GENERIC
                );

        assertThat(values)
                .as("SubDomain 应只有三个值，防止新增值时没有对应规则")
                .hasSize(3);
    }

    @Test
    @DisplayName("PortType 应有且仅有三个值")
    void portType_shouldHaveExactlyThreeValues() {
        PortType[] values = PortType.values();
        Set<PortType> valueSet = new HashSet<>();
        for (PortType value : values) {
            valueSet.add(value);
        }

        assertThat(valueSet)
                .as("PortType 应包含 REPOSITORY, CLIENT, PUBLISHER 三个值")
                .containsExactlyInAnyOrder(
                        PortType.REPOSITORY,
                        PortType.CLIENT,
                        PortType.PUBLISHER
                );

        assertThat(values)
                .as("PortType 应只有三个值，防止新增值时没有对应规则")
                .hasSize(3);
    }
}
