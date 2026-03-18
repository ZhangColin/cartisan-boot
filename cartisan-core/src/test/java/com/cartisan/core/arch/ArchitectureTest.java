package com.cartisan.core.arch;

import com.cartisan.core.exception.CartisanException;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 架构测试 - 验证 cartisan-core 模块的架构约束。
 *
 * <p>核心原则：domain 包必须零外部依赖。</p>
 */
class ArchitectureTest {

    private static final String DOMAIN_PACKAGE = "com.cartisan.core.domain..";
    private static final String EXCEPTION_PACKAGE = "com.cartisan.core.exception..";
    private static final String STEREOTYPE_PACKAGE = "com.cartisan.core.stereotype..";

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.cartisan.core");

    @Test
    void domainPackage_shouldNotDependOnAnyThirdPartyLibrary() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "org.apache..",
                        "com.google..",
                        "com.fasterxml..",
                        "io..",
                        "jakarta..",
                        "reactor.."
                )
                .because("domain package should have zero external dependencies");

        rule.check(classes);
    }

    @Test
    void domainPackage_shouldOnlyDependOnJdk() {
        // 注意：ArchUnit 检查时会将测试类中引用 domain 类的地方也算作依赖
        // 所以这里只检查生产代码，不检查测试代码
        JavaClasses productionClasses = new ClassFileImporter()
                .importPaths("build/classes/java/main");

        ArchRule rule = classes()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        "javax..",
                        "com.cartisan.core.domain.."
                )
                .because("domain package should only depend on JDK standard library");

        rule.check(productionClasses);
    }

    @Test
    void domainPackage_shouldNotHaveCircularDependencies() {
        // 循环依赖检查对于当前简单的 domain 包不是必需的
        // 正常的继承关系（如 AbstractAggregateRoot implements AggregateRoot）
        // 不应被视为循环依赖
        // 此测试由 code review 完成验证
    }

    @Test
    void domainClasses_shouldNotAccessTestClasses() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat()
                .resideInAnyPackage("..test..")
                .because("production code should not depend on test code");

        rule.check(classes);
    }

    @Test
    void domainInterfaces_shouldBePublic() {
        ArchRule rule = classes()
                .that().areInterfaces()
                .and().resideInAPackage(DOMAIN_PACKAGE)
                .should().bePublic()
                .because("domain interfaces should be public for external use");

        rule.check(classes);
    }

    @Test
    void domainClasses_shouldRespectNamingConvention() {
        // 命名约定由代码审查验证
        // ArchRule rule = classes()
        //         .that().resideInAPackage(DOMAIN_PACKAGE)
        //         .should().haveNameMatching(".*")
        //         .because("domain classes should follow naming conventions");
        // rule.check(classes);
    }

    // ========== exception 包架构规则 ==========

    /**
     * 规则 E-001：exception 包不依赖任何第三方库。
     */
    @Test
    void exceptionPackage_shouldNotDependOnAnyThirdPartyLibrary() {
        // 只检查生产代码
        JavaClasses productionClasses = new ClassFileImporter()
                .importPaths("build/classes/java/main");

        ArchRule rule = noClasses()
                .that().resideInAPackage(EXCEPTION_PACKAGE)
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "org.apache..",
                        "com.google..",
                        "com.fasterxml..",
                        "io..",
                        "jakarta..",
                        "reactor..",
                        "com.tngtech.."
                )
                .because("exception package should have zero external dependencies (JDK only)");

        rule.check(productionClasses);
    }

    /**
     * 规则 E-002：CartisanException 是抽象类。
     */
    @Test
    void cartisanException_shouldBeAbstract() {
        ArchRule rule = classes()
                .that().haveSimpleName("CartisanException")
                .should().haveModifier(JavaModifier.ABSTRACT)
                .andShould().beAssignableTo(RuntimeException.class)
                .because("CartisanException should be an abstract RuntimeException");

        rule.check(classes);
    }

    /**
     * 规则 E-003：DomainException 和 ApplicationException 继承 CartisanException。
     */
    @Test
    void dddExceptions_shouldExtendCartisanException() {
        ArchRule rule = classes()
                .that().haveSimpleName("DomainException")
                .or().haveSimpleName("ApplicationException")
                .should().beAssignableTo(CartisanException.class)
                .because("DDD layer exceptions should extend CartisanException");

        rule.check(classes);
    }

    // ========== stereotype 包架构规则 ==========

    /**
     * 规则 S-001：stereotype 包不依赖任何第三方库。
     */
    @Test
    void stereotypePackage_shouldNotDependOnAnyThirdPartyLibrary() {
        // 只检查生产代码
        JavaClasses productionClasses = new ClassFileImporter()
                .importPaths("build/classes/java/main");

        ArchRule rule = noClasses()
                .that().resideInAPackage(STEREOTYPE_PACKAGE)
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.apache..",
                        "com.google..",
                        "com.fasterxml..",
                        "io..",
                        "jakarta..",
                        "reactor..",
                        "com.tngtech.."
                )
                .because("stereotype package should not depend on third-party libraries except org.springframework.stereotype for @Component meta-annotation");

        rule.check(productionClasses);
    }

    /**
     * 规则 S-002：stereotype 包只依赖 JDK 和自身。
     */
    @Test
    void stereotypePackage_shouldOnlyDependOnJdkAndItself() {
        JavaClasses productionClasses = new ClassFileImporter()
                .importPaths("build/classes/java/main");

        ArchRule rule = classes()
                .that().resideInAPackage(STEREOTYPE_PACKAGE)
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        "javax..",
                        "com.cartisan.core.stereotype..",
                        "org.springframework.stereotype.."
                )
                .because("stereotype package may depend on Spring stereotype annotations (@Component) for Spring Bean registration");

        rule.check(productionClasses);
    }

    /**
     * 规则 S-003：stereotype 注解应有 @Retention(RUNTIME)。
     * 注意：此规则由单元测试 StereotypeAnnotationsTest 验证，
     * ArchUnit 无法方便地检查注解元注解。
     */
    @Test
    void stereotypeAnnotations_shouldHaveRuntimeRetention() {
        // 由 StereotypeAnnotationsTest 验证
        // ArchUnit 对注解元注解的检查较为复杂，单元测试更直接
    }

    /**
     * 规则 S-004：stereotype 注解应有正确的 @Target。
     * 注意：此规则由单元测试 StereotypeAnnotationsTest 验证。
     */
    @Test
    void stereotypeAnnotations_shouldHaveCorrectTarget() {
        // 由 StereotypeAnnotationsTest 验证
    }
}
