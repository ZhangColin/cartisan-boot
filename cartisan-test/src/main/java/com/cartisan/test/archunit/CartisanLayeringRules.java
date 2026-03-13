package com.cartisan.test.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * DDD 分层规则 — 验证六边形架构的分层依赖方向
 *
 * <p>包含以下规则：</p>
 * <ul>
 *   <li>领域层不依赖基础设施层</li>
 *   <li>领域层不依赖 Spring</li>
 *   <li>Controller 只依赖应用服务</li>
 *   <li>应用服务不直接操作数据库</li>
 * </ul>
 */
public class CartisanLayeringRules {

    /**
     * 领域层不能依赖基础设施层
     *
     * <p>这是 DDD 六边形架构的核心约束。</p>
     * <p>领域模型必须是纯粹的业务逻辑，不包含持久化细节。</p>
     */
    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("Domain layer should not depend on infrastructure layer");

    /**
     * 领域层不能依赖 Spring
     *
     * <p>领域模型必须保持框架无关。</p>
     * <p>Spring 注解只能用在应用层、基础设施层。</p>
     */
    @ArchTest
    static final ArchRule domainShouldNotDependOnSpring =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .because("Domain layer should be framework-agnostic");

    /**
     * Controller 只能依赖应用服务
     *
     * <p>Controller 不能直接调用领域层或基础设施层。</p>
     * <p>所有业务逻辑通过应用服务协调。</p>
     */
    @ArchTest
    static final ArchRule controllersShouldOnlyDependOnApplication =
        noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..domain..")
            .orShould()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("Controllers should only depend on application services");

    /**
     * 应用服务不能直接操作数据库
     *
     * <p>应用服务通过 Repository（端口）访问数据。</p>
     * <p>不能直接使用 JPA EntityManager 或 JDBC。</p>
     */
    @ArchTest
    static final ArchRule applicationShouldNotAccessDatabaseDirectly =
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("jakarta.persistence.EntityManager")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("jakarta.persistence.EntityManagerFactory")
            .orShould()
            .dependOnClassesThat()
            .resideInAPackage("java.sql..")
            .because("Application services should access data through Repository ports, not directly");
}
