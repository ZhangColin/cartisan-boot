# Feature: F01-07 — 接口契约

> 版本：v0.1 | 日期：2026-03-13
> 状态：Phase 2 完成

---

## 模块结构

```
cartisan-test/src/
├── main/java/com/cartisan/test/archunit/
│   ├── CartisanLayeringRules.java           # DDD 分层规则（4 条）
│   ├── CartisanNamingRules.java             # 命名规范规则（4 条）
│   ├── CartisanProhibitionRules.java        # 禁止规则（3 条）
│   ├── CartisanArchRules.java               # 聚合全部规则
│   └── package-info.java
│
└── test/java/com/cartisan/test/archunit/
    ├── CartisanLayeringRulesTest.java
    ├── CartisanNamingRulesTest.java
    ├── CartisanProhibitionRulesTest.java
    │
    └── fixtures/
        ├── compliant/                        # 合规示例代码
        │   ├── domain/GoodEntity.java
        │   ├── application/GoodAppService.java
        │   ├── infrastructure/GoodRepository.java
        │   └── controller/GoodController.java
        │
        └── violation/                        # 违规示例代码
            ├── domain/BadDomainWithSpring.java
            ├── domain/BadDomainDependsOnInfra.java
            ├── application/BadServiceNaming.java
            ├── application/BadAppServiceWithJPA.java
            ├── controller/BadControllerNaming.java
            ├── controller/BadFieldInjection.java
            └── shared/BadMoneyWithDouble.java
```

---

## 类设计

### CartisanLayeringRules — DDD 分层规则

**职责**：验证六边形架构的分层依赖方向

**规则定义**（伪代码）：

| 规则名称 | 检查内容 | 违规示例 |
|---------|---------|---------|
| `domainShouldNotDependOnInfrastructure` | `..domain..` 包的类不能依赖 `..infrastructure..` 包的类 | 领域类直接调用 Repository |
| `domainShouldNotDependOnSpring` | `..domain..` 包的类不能依赖 `org.springframework..` 包的类 | 领域类使用 `@Autowired` |
| `controllersShouldOnlyDependOnApplication` | `..controller..` 包的类只能依赖 `..application..`，不能直接依赖 `..domain..` | Controller 直接调用 Entity |
| `applicationShouldNotAccessDatabaseDirectly` | `..application..` 包的类不能依赖 JPA/JDBC 类 | 应用服务直接使用 `EntityManager` |

**实现要点**：
- 使用 `noClasses().that().resideInAPackage("..source..").should().dependOnClassesThat().resideInAPackage("..target..")` DSL
- 规则使用 `@ArchTest` 注解 + `static final` 字段
- 每条规则添加 `.because("理由")` 提供清晰的失败消息

---

### CartisanNamingRules — 命名规范规则

**职责**：验证 DDD 各层组件的命名约定

**规则定义**（伪代码）：

| 规则名称 | 条件 | 后缀要求 | 理由 |
|---------|------|---------|------|
| `controllersShouldBeSuffixed` | `@RestController` 注解 | `*Controller` | REST 层统一命名 |
| `appServicesShouldBeSuffixed` | `@Service` 注解 + `..application..` 包 | `*AppService` | 区分应用服务和领域服务 |
| `domainServicesShouldBeSuffixed` | `@DomainService` 注解 | `*Service` | 领域服务不需要 App 前缀 |
| `repositoriesShouldBeSuffixed` | `@Repository` 注解 | `*Repository` | 仓储层统一命名 |

**实现要点**：
- 使用 `classes().that().areAnnotatedWith(XXX.class).should().haveSimpleNameEndingWith("YYY")` DSL
- `AppService` 规则需要组合两个条件：`.and().resideInAPackage("..application..")`
- 注解引用：`RestController.class`、`Service.class`、`Repository.class`、`DomainService.class`

---

### CartisanProhibitionRules — 禁止规则

**职责**：禁止反模式和危险实践

**规则定义**（伪代码）：

| 规则名称 | 检查内容 | 违规示例 |
|---------|---------|---------|
| `noFieldInjection` | 字段不能有 `@Autowired` 注解 | `@Autowired private UserService userService;` |
| `noJavaUtilDate` | 不能依赖 `java.util.Date` 类 | `import java.util.Date;` |
| `noFloatingPointForMoney` | 金额字段（按名字匹配）不能用 `Double`/`Float` | `private Double price;` |

**金额字段匹配模式**（正则）：
```regex
.*(?i)(price|amount|fee|cost|balance|money|payment|refund|commission).*
```

**实现要点**：
- 字段注入使用 `noFields().should().beAnnotatedWith(Autowired.class)` DSL
- Date 禁止使用 `noClasses().should().dependOnClassesThat().haveFullyQualifiedName("java.util.Date")` DSL
- 金额字段使用 `noFields().that().haveNameMatching("regex").should().haveRawType(Double.class).orShould().haveRawType(Float.class)` DSL

---

### CartisanArchRules — 聚合全部规则

**职责**：组合所有规则的入口，业务项目继承即可获得完整守护

**伪代码**：

```java
public class CartisanArchRules {
    @ArchTest
    static final ArchRules layering = ArchRules.in(CartisanLayeringRules.class);

    @ArchTest
    static final ArchRules naming = ArchRules.in(CartisanNamingRules.class);

    @ArchTest
    static final ArchRules prohibition = ArchRules.in(CartisanProhibitionRules.class);
}
```

**设计说明**：
- 使用 `ArchRules.in()` 组合，不是 Java 继承
- 每个分类规则独立成一个 `@ArchTest` 字段
- 业务项目 `extends CartisanArchRules` 即继承全部规则

---

## 测试类设计

### CartisanLayeringRulesTest

**职责**：验证 4 条分层规则的有效性

**测试方法**（伪代码）：

| 测试方法 | 输入 | 预期结果 |
|---------|------|---------|
| `domainShouldNotDependOnInfrastructure_passes` | fixtures.compliant 包 | 不抛异常 |
| `domainShouldNotDependOnInfrastructure_fails` | fixtures.violation 包（BadDomainDependsOnInfra） | 抛 `AssertionError` |
| `domainShouldNotDependOnSpring_passes` | fixtures.compliant 包 | 不抛异常 |
| `domainShouldNotDependOnSpring_fails` | fixtures.violation 包（BadDomainWithSpring） | 抛 `AssertionError` |
| `controllersShouldOnlyDependOnApplication_passes` | fixtures.compliant 包 | 不抛异常 |
| `controllersShouldOnlyDependOnApplication_fails` | fixtures.violation 包（BadControllerCallsDomain） | 抛 `AssertionError` |
| `applicationShouldNotAccessDatabaseDirectly_passes` | fixtures.compliant 包 | 不抛异常 |
| `applicationShouldNotAccessDatabaseDirectly_fails` | fixtures.violation 包（BadAppServiceWithJPA） | 抛 `AssertionError` |

**实现要点**：
- 使用 `new ClassFileImporter().importPackages("com.cartisan.test.archunit.fixtures.XXX")` 加载测试类
- 断言通过使用 `rule.check(classes)`（不抛异常 = 通过）
- 断言失败使用 `assertThatThrownBy(() -> rule.check(classes)).isInstanceOf(AssertionError.class)`

---

### CartisanNamingRulesTest

**职责**：验证 4 条命名规则的有效性

**测试方法**（伪代码）：

| 测试方法 | fixtures | 预期结果 |
|---------|---------|---------|
| `controllersShouldBeSuffixed_passes` | GoodController（`@RestController` + `*Controller` 后缀） | 通过 |
| `controllersShouldBeSuffixed_fails` | BadControllerNaming（`@RestController` + 错误后缀） | 失败 |
| `appServicesShouldBeSuffixed_passes` | GoodAppService（`@Service` + `..application..` + `*AppService`） | 通过 |
| `appServicesShouldBeSuffixed_fails` | BadServiceNaming（`@Service` + `..application..` + 错误后缀） | 失败 |
| `domainServicesShouldBeSuffixed_passes` | GoodDomainService（`@DomainService` + `*Service`） | 通过 |
| `domainServicesShouldBeSuffixed_fails` | BadDomainService（`@DomainService` + 错误后缀） | 失败 |
| `repositoriesShouldBeSuffixed_passes` | GoodRepository（`@Repository` + `*Repository`） | 通过 |
| `repositoriesShouldBeSuffixed_fails` | BadRepository（`@Repository` + 错误后缀） | 失败 |

---

### CartisanProhibitionRulesTest

**职责**：验证 3 条禁止规则的有效性

**测试方法**（伪代码）：

| 测试方法 | fixtures | 预期结果 |
|---------|---------|---------|
| `noFieldInjection_passes` | 构造函数注入的类 | 通过 |
| `noFieldInjection_fails` | BadFieldInjection（`@Autowired` 字段） | 失败 |
| `noJavaUtilDate_passes` | 使用 `java.time` 的类 | 通过 |
| `noJavaUtilDate_fails` | 使用 `java.util.Date` 的类 | 失败 |
| `noFloatingPointForMoney_passes` | 金额字段用 `BigDecimal` | 通过 |
| `noFloatingPointForMoney_fails` | BadMoneyWithDouble（金额字段用 `Double`） | 失败 |

---

## Fixtures 设计

### Compliant Fixtures（合规示例）

| 类名 | 位置 | 注解 | 特征 |
|------|------|------|------|
| `GoodEntity` | `domain/` | `@Entity` | 纯领域逻辑，无 Spring 依赖 |
| `GoodAppService` | `application/` | `@Service` | `*AppService` 后缀，无 JPA |
| `GoodRepository` | `infrastructure/` | `@Repository` | `*Repository` 后缀 |
| `GoodController` | `controller/` | `@RestController` | `*Controller` 后缀，构造函数注入 |
| `GoodDomainService` | `domain/` | `@DomainService` | `*Service` 后缀 |
| `GoodMoney` | `shared/` | — | 金额字段用 `BigDecimal` |

### Violation Fixtures（违规示例）

| 类名 | 违反规则 | 违规内容 |
|------|---------|---------|
| `BadDomainDependsOnInfra` | `domainShouldNotDependOnInfrastructure` | 领域类直接依赖 Repository 类 |
| `BadDomainWithSpring` | `domainShouldNotDependOnSpring` | 领域类使用 `@Autowired` |
| `BadControllerCallsDomain` | `controllersShouldOnlyDependOnApplication` | Controller 直接注入 Entity |
| `BadAppServiceWithJPA` | `applicationShouldNotAccessDatabaseDirectly` | 应用服务使用 `EntityManager` |
| `BadControllerNaming` | `controllersShouldBeSuffixed` | `@RestController` 但后缀不是 `*Controller` |
| `BadServiceNaming` | `appServicesShouldBeSuffixed` | `@Service` 在 application 包但后缀不是 `*AppService` |
| `BadDomainService` | `domainServicesShouldBeSuffixed` | `@DomainService` 但后缀不是 `*Service` |
| `BadRepository` | `repositoriesShouldBeSuffixed` | `@Repository` 但后缀不是 `*Repository` |
| `BadFieldInjection` | `noFieldInjection` | 字段上有 `@Autowired` |
| `BadDateUsage` | `noJavaUtilDate` | 使用 `java.util.Date` |
| `BadMoneyWithDouble` | `noFloatingPointForMoney` | `price` 字段类型是 `Double` |

---

## 包结构

```
com.cartisan.test.archunit
├── CartisanLayeringRules       # 分层规则
├── CartisanNamingRules         # 命名规则
├── CartisanProhibitionRules    # 禁止规则
└── CartisanArchRules           # 聚合规则
```

---

## 依赖

### 外部依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| ArchUnit | 1.3.0+ | 规则引擎 |
| JUnit 5 | 5.x | 测试框架 |
| AssertJ | 3.x | 断言库 |
| Spring Framework | (通过 cartisan-core) | 注解类引用 |

### 内部依赖

| 依赖 | 用途 |
|------|------|
| cartisan-core | 引用 `@DomainService` 等架构注解 |

---

## 业务项目使用方式

### 方式 1：继承全部规则（推荐）

```java
@AnalyzeClasses(packages = "com.aieducenter")
public class ArchitectureTest extends CartisanArchRules {
    // 完了。所有规则自动生效。
}
```

### 方式 2：选择部分规则

```java
@AnalyzeClasses(packages = "com.aieducenter")
public class ArchitectureTest {
    @ArchTest
    static final ArchRules layering = ArchRules.in(CartisanLayeringRules.class);
    @ArchTest
    static final ArchRules prohibition = ArchRules.in(CartisanProhibitionRules.class);
    // 不要 naming 规则
}
```

### 方式 3：追加自定义规则

```java
@AnalyzeClasses(packages = "com.aieducenter")
public class ArchitectureTest extends CartisanArchRules {

    @ArchTest
    static final ArchRule orderServiceMustBeTransactional =
        classes().that().haveSimpleNameContaining("OrderService")
            .should().beAnnotatedWith(Transactional.class);
}
```

---

## 构建配置

### cartisan-test/build.gradle.kts

```kotlin
dependencies {
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    api(libs.assertj.core)
    api(libs.mockito.core)
    api(libs.archunit.junit5)

    implementation(project(":cartisan-core"))
}
```

**说明**：
- 使用 `api` 暴露测试依赖给业务项目
- ArchUnit 使用 `archunit-junit5`（包含 `@AnalyzeClasses` 支持）

---

## 验收检查

### 规则类完整性
- [ ] `CartisanLayeringRules` 包含 4 条规则
- [ ] `CartisanNamingRules` 包含 4 条规则
- [ ] `CartisanProhibitionRules` 包含 3 条规则
- [ ] `CartisanArchRules` 使用 `ArchRules.in()` 组合

### 规则实现正确性
- [ ] 所有规则使用 `@ArchTest` 注解
- [ ] 所有规则是 `static final` 字段
- [ ] 规则失败时有清晰的错误消息
- [ ] 注解直接引用（如 `Autowired.class`）

### 测试完整性
- [ ] 每个规则类有对应测试
- [ ] 每条规则有 pass 和 fail 两个测试
- [ ] fixtures 覆盖所有违规场景
- [ ] 测试使用 `ClassFileImporter` 加载 fixtures

### 文档完整性
- [ ] 每个规则类有 JavaDoc
- [ ] 每条规则有描述性名称
- [ ] package-info.java 说明模块用途
