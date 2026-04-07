package com.cartisan.core;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * cartisan-core 模块完整性测试。
 *
 * <p>验证范围：</p>
 * <ul>
 *   <li>包结构白名单守护（domain/exception/stereotype/util）</li>
 *   <li>根包整洁性</li>
 * </ul>
 *
 * <p>注意：零外部依赖验证由 {@code com.cartisan.core.arch.ArchitectureTest} 负责，
 * JavaDoc 完整性由 Maven javadoc 插件负责。</p>
 *
 * @since 0.1.0
 */
class CartisanCoreModuleTest {

    private static final String ROOT_PACKAGE = "com.cartisan.core";

    /**
     * 只导入生产代码，不导入测试代码。
     *
     * <p>使用 Maven 标准构建路径 {@code target/classes}。
     * 这与现有 {@code ArchitectureTest.java} 的模式保持一致。
     * 测试前需要先运行 {@code mvn compile}。</p>
     */
    private final JavaClasses productionClasses = new ClassFileImporter()
            .importPaths("target/classes");

    /**
     * 规则 P-001：所有生产类必须在允许的一级包中。
     *
     * <p>允许的一级包：</p>
     * <ul>
     *   <li>com.cartisan.core.domain</li>
     *   <li>com.cartisan.core.exception</li>
     *   <li>com.cartisan.core.stereotype</li>
     *   <li>com.cartisan.core.util</li>
     * </ul>
     *
     * <p>允许这些包的任意子包（如 domain.event）。</p>
     *
     * <p>注意：排除 package-info 类，因为它们是包级别文档，不是真正的类。</p>
     */
    @Test
    void packageStructure_shouldOnlyUseAllowedTopLevelPackages() {
        ArchRule rule = classes()
                .that().resideInAPackage(ROOT_PACKAGE + "..")
                .and().doNotHaveSimpleName("package-info")
                .should().resideInAnyPackage(
                        "com.cartisan.core.domain..",
                        "com.cartisan.core.exception..",
                        "com.cartisan.core.stereotype..",
                        "com.cartisan.core.util.."
                )
                .because("只允许四个一级包及其所有子包");

        rule.check(productionClasses);
    }

    /**
     * 规则 P-002：根包下不应有生产类。
     *
     * <p>com.cartisan.core 根包下只允许 package-info.java，
     * 防止有人随手添加工具类到根包。</p>
     *
     * <p>注意：ArchUnit 会导入 package-info.java，需要显式排除。</p>
     */
    @Test
    void packageStructure_shouldNotAllowUtilityClassesInRoot() {
        // 检查根包下没有任何生产类（排除 package-info）
        long rootPackageClassCount = productionClasses.stream()
                .filter(c -> c.getPackage().getName().equals(ROOT_PACKAGE))
                .filter(c -> !c.getSimpleName().equals("package-info"))
                .count();

        assertEquals(0, rootPackageClassCount,
                "根包下不应有类，只允许 package-info.java");
    }
}
