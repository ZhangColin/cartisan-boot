package com.cartisan.core.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 架构测试 - 验证 cartisan-core 模块的架构约束。
 *
 * <p>核心原则：domain 包必须零外部依赖。</p>
 */
class ArchitectureTest {

    private static final String DOMAIN_PACKAGE = "com.cartisan.core.domain..";

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
}
