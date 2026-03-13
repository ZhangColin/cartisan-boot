# Feature: F01-07 — 实施计划

> 版本：v0.1 | 日期：2026-03-13
> 状态：Phase 3 完成

---

## 目标复述

为 cartisan-test 模块创建可复用的 ArchUnit 架构规则集。包含 4 个规则类（分层、命名、禁止、聚合）和 3 个测试类。业务项目继承 `CartisanArchRules` 即可获得完整的 DDD 分层架构守护。规则使用 `ArchRules.in()` 组合，支持灵活选择。使用 fixtures 方式测试，每条规则有合规/违规成对验证。

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| Create | `cartisan-test/src/main/java/com/cartisan/test/archunit/package-info.java` | archunit 包说明 |
| Create | `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanProhibitionRules.java` | 禁止规则（3 条） |
| Create | `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanNamingRules.java` | 命名规则（4 条） |
| Create | `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java` | 分层规则（4 条） |
| Create | `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanArchRules.java` | 聚合规则入口 |
| Create | `cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/compliant/**/*.java` | 合规 fixtures（6 个类） |
| Create | `cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/violation/**/*.java` | 违规 fixtures（11 个类） |
| Create | `cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanProhibitionRulesTest.java` | 禁止规则测试 |
| Create | `cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanNamingRulesTest.java` | 命名规则测试 |
| Create | `cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanLayeringRulesTest.java` | 分层规则测试 |

---

## 核心流程（伪代码）

```
1. 创建 archunit 包结构
2. 按依赖顺序创建规则类：
   ProhibitionRules（无外部依赖）
   → NamingRules（依赖 cartisan-core 注解）
   → LayeringRules（依赖 JPA/Spring 包名）
   → CartisanArchRules（组合以上三类）
3. 创建合规 fixtures（所有规则都应该通过）
4. 创建违规 fixtures（每条规则至少一个违规示例）
5. 创建测试类（每条规则两个测试：pass + fail）
6. 全量验证
```

---

## 原子任务清单

### Step 1: 创建 archunit 包基础结构

**文件：**
- `cartisan-test/src/main/java/com/cartisan/test/archunit/package-info.java`

**内容：**
- 包级 JavaDoc，说明 archunit 模块的用途
- 列出 4 个规则类的职责
- 提供业务项目使用示例代码

**验证：**
```bash
./gradlew :cartisan-test:compileJava
```
Expected: SUCCESS

---

### Step 2: 创建 CartisanProhibitionRules

**文件：**
- `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanProhibitionRules.java`

**内容：**
- `noFieldInjection`：禁止 `@Autowired` 字段
- `noJavaUtilDate`：禁止 `java.util.Date`
- `noFloatingPointForMoney`：禁止金额字段用 `Double/Float`

**关键代码：**
```java
@ArchTest
static final ArchRule noFieldInjection =
    noFields().should().beAnnotatedWith(Autowired.class)
        .because("Use constructor injection");

@ArchTest
static final ArchRule noJavaUtilDate =
    noClasses().should().dependOnClassesThat()
        .haveFullyQualifiedName("java.util.Date")
        .because("Use java.time API");

@ArchTest
static final ArchRule noFloatingPointForMoney =
    noFields().that().haveNameMatching(".*(?i)(price|amount|fee|cost|balance).*")
        .should().haveRawType(Double.class).orShould().haveRawType(Float.class)
        .because("Use BigDecimal for monetary fields");
```

**验证：**
```bash
./gradlew :cartisan-test:compileJava
```
Expected: SUCCESS

---

### Step 3: 创建 CartisanNamingRules

**文件：**
- `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanNamingRules.java`

**内容：**
- `controllersShouldBeSuffixed`：`@RestController` → `*Controller`
- `appServicesShouldBeSuffixed`：`@Service` + `..application..` → `*AppService`
- `domainServicesShouldBeSuffixed`：`@DomainService` → `*Service`
- `repositoriesShouldBeSuffixed`：`@Repository` → `*Repository`

**关键代码：**
```java
@ArchTest
static final ArchRule appServicesShouldBeSuffixed =
    classes().that().areAnnotatedWith(Service.class)
        .and().resideInAPackage("..application..")
        .should().haveSimpleNameEndingWith("AppService")
        .because("Distinguish from domain services");
```

**验证：**
```bash
./gradlew :cartisan-test:compileJava
```
Expected: SUCCESS

---

### Step 4: 创建 CartisanLayeringRules

**文件：**
- `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java`

**内容：**
- `domainShouldNotDependOnInfrastructure`
- `domainShouldNotDependOnSpring`
- `controllersShouldOnlyDependOnApplication`
- `applicationShouldNotAccessDatabaseDirectly`

**关键代码：**
```java
@ArchTest
static final ArchRule domainShouldNotDependOnSpring =
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("org.springframework..")
        .because("Domain layer should be framework-agnostic");
```

**验证：**
```bash
./gradlew :cartisan-test:compileJava
```
Expected: SUCCESS

---

### Step 5: 创建 CartisanArchRules 聚合类

**文件：**
- `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanArchRules.java`

**内容：**
- 使用 `ArchRules.in()` 组合三类规则
- 提供 JavaDoc 说明三种使用姿势

**关键代码：**
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

**验证：**
```bash
./gradlew :cartisan-test:compileJava
```
Expected: SUCCESS

---

### Step 6: 创建测试包结构和 fixtures

**文件：**
- 创建目录结构
- `cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/compliant/**/*.java`
- `cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/violation/**/*.java`

**内容：**

| Compliant Fixtures | 违反规则 |
|-------------------|---------|
| `GoodEntity` | 无违规 |
| `GoodDomainService` | 无违规 |
| `GoodAppService` | 无违规 |
| `GoodRepository` | 无违规 |
| `GoodController` | 无违规 |
| `GoodMoney` | 无违规 |

| Violation Fixtures | 违反规则 |
|-------------------|---------|
| `BadDomainWithSpring` | domainShouldNotDependOnSpring |
| `BadDomainDependsOnInfra` | domainShouldNotDependOnInfrastructure |
| `BadDomainService` | domainServicesShouldBeSuffixed |
| `BadServiceNaming` | appServicesShouldBeSuffixed |
| `BadAppServiceWithJPA` | applicationShouldNotAccessDatabaseDirectly |
| `BadRepository` | repositoriesShouldBeSuffixed |
| `BadControllerNaming` | controllersShouldBeSuffixed |
| `BadFieldInjection` | noFieldInjection |
| `BadControllerCallsDomain` | controllersShouldOnlyDependOnApplication |
| `BadDateUsage` | noJavaUtilDate |
| `BadMoneyWithDouble` | noFloatingPointForMoney |

**验证：**
```bash
./gradlew :cartisan-test:compileTestJava
```
Expected: SUCCESS

---

### Step 7: 创建 CartisanProhibitionRulesTest

**文件：**
- `cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanProhibitionRulesTest.java`

**内容：**
- 使用 `ClassFileImporter` 加载 fixtures
- 每条规则两个测试：合规通过、违规失败
- 使用 AssertJ 断言

**关键代码：**
```java
static final JavaClasses compliantClasses = new ClassFileImporter()
    .importPackages("com.cartisan.test.archunit.fixtures.compliant");
static final JavaClasses violatingClasses = new ClassFileImporter()
    .importPackages("com.cartisan.test.archunit.fixtures.violation");

@Test
void noFieldInjection_passes() {
    assertThatCode(() -> rule.check(compliantClasses))
        .doesNotThrowAnyException();
}

@Test
void noFieldInjection_fails() {
    assertThatThrownBy(() -> rule.check(violatingClasses))
        .isInstanceOf(AssertionError.class);
}
```

**验证：**
```bash
./gradlew :cartisan-test:test --tests CartisanProhibitionRulesTest
```
Expected: 6/6 PASS

---

### Step 8: 创建 CartisanNamingRulesTest

**文件：**
- `cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanNamingRulesTest.java`

**内容：**
- 4 条规则 × 2 个测试 = 8 个测试方法
- 结构同 ProhibitionRulesTest

**验证：**
```bash
./gradlew :cartisan-test:test --tests CartisanNamingRulesTest
```
Expected: 8/8 PASS

---

### Step 9: 创建 CartisanLayeringRulesTest

**文件：**
- `cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanLayeringRulesTest.java`

**内容：**
- 4 条规则 × 2 个测试 = 8 个测试方法
- 结构同 ProhibitionRulesTest

**验证：**
```bash
./gradlew :cartisan-test:test --tests CartisanLayeringRulesTest
```
Expected: 8/8 PASS

---

### Step 10: 全量验证

**内容：**
- 运行所有测试
- 验证模块构建

**命令：**
```bash
./gradlew :cartisan-test:test
./gradlew :cartisan-test:build
```
Expected: 全部 SUCCESS

---

## 验收清单

- [ ] `CartisanLayeringRules` 包含 4 条分层规则
- [ ] `CartisanNamingRules` 包含 4 条命名规则
- [ ] `CartisanProhibitionRules` 包含 3 条禁止规则
- [ ] `CartisanArchRules` 使用 `ArchRules.in()` 组合所有规则
- [ ] 每个规则类有对应的测试类
- [ ] fixtures 包含合规和违规示例
- [ ] 每条规则有两个测试（通过 + 失败）
- [ ] 所有测试通过
- [ ] 模块可以成功构建

---

## 下一步

进入 **Phase 4: Execute**，按本计划的原子任务清单逐步执行：
1. 逐个创建规则类（Step 2-5）
2. 创建 fixtures（Step 6）
3. 创建测试类（Step 7-9）
4. 全量验证（Step 10）
