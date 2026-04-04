package com.cartisan.test.archunit;

import com.cartisan.core.stereotype.DomainService;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 命名规范规则 — 验证 DDD 各层组件的命名约定
 *
 * <p>包含以下规则：</p>
 * <ul>
 *   <li>Controller 类必须以 Controller 结尾</li>
 *   <li>应用服务（application 包）必须以 AppService 结尾</li>
 *   <li>领域服务必须以 Service 结尾</li>
 *   <li>Repository 必须以 Repository 结尾</li>
 * </ul>
 */
public class CartisanNamingRules {

    /**
     * @RestController 类必须以 Controller 结尾
     */
    @ArchTest
    static final ArchRule controllersShouldBeSuffixed =
        classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .haveSimpleNameEndingWith("Controller")
            .because("REST controllers should be suffixed with 'Controller'");

    /**
     * application 包中的 @Service 类必须以 AppService 结尾
     *
     * <p>此规则区分应用服务和领域服务：</p>
     * <ul>
     *   <li>应用服务：*AppService，位于 application 包，使用 @Service</li>
     *   <li>领域服务：*Service，位于 domain 包，使用 @DomainService</li>
     * </ul>
     */
    @ArchTest
    static final ArchRule appServicesShouldBeSuffixed =
        classes()
            .that()
            .areAnnotatedWith(Service.class)
            .and()
            .resideInAPackage("..application..")
            .should()
            .haveSimpleNameEndingWith("AppService")
            .because("Application services should be suffixed with 'AppService' to distinguish from domain services");

    /**
     * @DomainService 类必须以 Service 结尾
     */
    @ArchTest
    static final ArchRule domainServicesShouldBeSuffixed =
        classes()
            .that()
            .areAnnotatedWith(DomainService.class)
            .should()
            .haveSimpleNameEndingWith("Service")
            .because("Domain services should be suffixed with 'Service'");

    /**
     * @Repository 必须以 Repository 结尾
     */
    @ArchTest
    static final ArchRule repositoriesShouldBeSuffixed =
        classes()
            .that()
            .areAnnotatedWith(Repository.class)
            .should()
            .haveSimpleNameEndingWith("Repository")
            .because("Repositories should be suffixed with 'Repository'");

    /**
     * 外部 API Controller 必须包含版本号
     *
     * <p>避免多版本共存时 Spring Bean 名称冲突。</p>
     * <p>类名必须包含 V{数字} 格式，如 {@code UserApiV1Controller}。</p>
     */
    @ArchTest
    static final ArchRule externalApiControllersMustContainVersion =
        classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .and()
            .resideInAPackage("..endpoints.api..")
            .should()
            .haveNameMatching(".*V\\d+.*")
            .because("External API controllers must include version number to avoid bean name conflicts");
}
