package com.cartisan.test.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 聚合全部 ArchUnit 规则
 *
 * <p>业务项目继承此类即可获得完整的架构守护：</p>
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.yourcompany")
 * public class ArchitectureTest extends CartisanArchRules {
 *     // 完了。所有规则自动生效。
 * }
 * }</pre>
 *
 * <p><strong>灵活使用方式：</strong></p>
 * <ul>
 *   <li>继承全部：{@code extends CartisanArchRules}</li>
 *   <li>选择部分：不继承，直接在测试类中声明需要的规则字段</li>
 *   <li>追加自定义：继承后添加自己的 {@code @ArchTest} 字段</li>
 * </ul>
 *
 * @see com.cartisan.test.archunit.CartisanLayeringRules
 * @see com.cartisan.test.archunit.CartisanNamingRules
 * @see com.cartisan.test.archunit.CartisanProhibitionRules
 * @see com.cartisan.test.archunit.CartisanCodingStandardsRules
 */
public class CartisanArchRules {

    /**
     * 领域层不能依赖基础设施层
     */
    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
        CartisanLayeringRules.domainShouldNotDependOnInfrastructure;

    /**
     * 领域层不能依赖 Spring
     */
    @ArchTest
    static final ArchRule domainShouldNotDependOnSpring =
        CartisanLayeringRules.domainShouldNotDependOnSpring;

    /**
     * Controller 只能依赖应用服务
     */
    @ArchTest
    static final ArchRule controllersShouldOnlyDependOnApplication =
        CartisanLayeringRules.controllersShouldOnlyDependOnApplication;

    /**
     * 应用服务不能直接操作数据库
     */
    @ArchTest
    static final ArchRule applicationShouldNotAccessDatabaseDirectly =
        CartisanLayeringRules.applicationShouldNotAccessDatabaseDirectly;

    /**
     * Controller 命名规范
     */
    @ArchTest
    static final ArchRule controllersShouldBeSuffixed =
        CartisanNamingRules.controllersShouldBeSuffixed;

    /**
     * 应用服务命名规范
     */
    @ArchTest
    static final ArchRule appServicesShouldBeSuffixed =
        CartisanNamingRules.appServicesShouldBeSuffixed;

    /**
     * 领域服务命名规范
     */
    @ArchTest
    static final ArchRule domainServicesShouldBeSuffixed =
        CartisanNamingRules.domainServicesShouldBeSuffixed;

    /**
     * Repository 命名规范
     */
    @ArchTest
    static final ArchRule repositoriesShouldBeSuffixed =
        CartisanNamingRules.repositoriesShouldBeSuffixed;

    /**
     * 禁止字段注入
     */
    @ArchTest
    static final ArchRule noFieldInjection =
        CartisanProhibitionRules.noFieldInjection;

    /**
     * 禁止使用 java.util.Date
     */
    @ArchTest
    static final ArchRule noJavaUtilDate =
        CartisanProhibitionRules.noJavaUtilDate;

    /**
     * 禁止金额字段使用浮点数
     */
    @ArchTest
    static final ArchRule noFloatingPointForMoney =
        CartisanProhibitionRules.noFloatingPointForMoney;

    /**
     * 领域层枚举必须实现 BaseEnum
     */
    @ArchTest
    static final ArchRule domainEnumsShouldImplementBaseEnum =
        CartisanCodingStandardsRules.domainEnumsShouldImplementBaseEnum;
}
