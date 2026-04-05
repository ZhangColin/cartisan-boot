# ArchUnit 规则统一添加 allowEmptyShould 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 为所有缺少 `.allowEmptyShould(true)` 的 ArchUnit 规则添加此配置，确保规则只在存在相关代码时生效

**架构:** 统一修改 3 个规则类中的 13 条规则，在每条规则末尾添加 `.allowEmptyShould(true)`，保持规则行为一致性

**技术栈:** ArchUnit (TngTech ArchUnit 1.x), JUnit 5

---

## Task 1: 修改 CartisanProhibitionRules（3 条规则）

**文件：** `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanProhibitionRules.java`

- [ ] **Step 1: 读取规则文件**

```bash
cat cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanProhibitionRules.java
```

预期：看到当前的 3 条规则定义

- [ ] **Step 2: 修改 noFieldInjection 规则（第 28-32 行）**

在 `.because()` 后添加 `.allowEmptyShould(true);`：

```java
@ArchTest
static final ArchRule noFieldInjection =
    noFields()
        .should()
        .beAnnotatedWith(Autowired.class)
        .because("Use constructor injection instead of field injection")
        .allowEmptyShould(true);
```

- [ ] **Step 3: 修改 noJavaUtilDate 规则（第 40-46 行）**

在 `.because()` 后添加 `.allowEmptyShould(true);`：

```java
@ArchTest
static final ArchRule noJavaUtilDate =
    noClasses()
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("java.util.Date")
        .because("Use java.time API instead of java.util.Date")
        .allowEmptyShould(true);
```

- [ ] **Step 4: 修改 noFloatingPointForMoney 规则（第 58-66 行）**

在 `.because()` 后添加 `.allowEmptyShould(true);`：

```java
@ArchTest
static final ArchRule noFloatingPointForMoney =
    noFields()
        .that()
        .haveNameMatching(".*(?i)(price|amount|fee|cost|balance|money|payment|refund|commission).*")
        .should()
        .haveRawType(Double.class)
        .orShould()
        .haveRawType(Float.class)
        .because("Use BigDecimal for monetary fields to avoid precision loss")
        .allowEmptyShould(true);
```

- [ ] **Step 5: 验证修改**

```bash
grep -c "allowEmptyShould(true)" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanProhibitionRules.java
```

预期：输出 3（确认 3 条规则都已添加）

- [ ] **Step 6: 提交修改**

```bash
git add cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanProhibitionRules.java
git commit -m "refactor: add allowEmptyShould to CartisanProhibitionRules

- Add .allowEmptyShould(true) to all 3 prohibition rules
- Rules now only apply when relevant code exists
- Prevents false positives in projects without certain features
- Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 修改 CartisanLayeringRules（5 条规则）

**文件：** `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java`

- [ ] **Step 1: 读取规则文件**

```bash
cat cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java
```

预期：看到当前的 5 条规则定义

- [ ] **Step 2: 修改 domainShouldNotDependOnInfrastructure 规则（第 27-35 行）**

```java
@ArchTest
static final ArchRule domainShouldNotDependOnInfrastructure =
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..infrastructure..")
        .because("Domain layer should not depend on infrastructure layer")
        .allowEmptyShould(true);
```

- [ ] **Step 3: 修改 domainShouldNotDependOnSpring 规则（第 46-56 行）**

```java
@ArchTest
static final ArchRule domainShouldNotDependOnSpring =
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .and()
        .areNotInterfaces()
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework..")
        .because("Domain layer should be framework-agnostic")
        .allowEmptyShould(true);
```

- [ ] **Step 4: 修改 controllersShouldOnlyDependOnApplication 规则（第 67-81 行）**

```java
@ArchTest
static final ArchRule controllersShouldOnlyDependOnApplication =
    noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..domain..aggregate..")
        .orShould()
        .dependOnClassesThat()
        .resideInAPackage("..domain..entity..")
        .orShould()
        .dependOnClassesThat()
        .resideInAPackage("..infrastructure..")
        .because("Controllers should only depend on application services")
        .allowEmptyShould(true);
```

- [ ] **Step 5: 修改 applicationShouldNotAccessDatabaseDirectly 规则（第 89-103 行）**

```java
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
        .because("Application services should access data through Repository ports, not directly")
        .allowEmptyShould(true);
```

- [ ] **Step 6: 修改 controllersShouldNotDependOnAggregates 规则（第 111-125 行）**

```java
@ArchTest
static final ArchRule controllersShouldNotDependOnAggregates =
    noClasses()
        .that()
        .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
        .should()
        .dependOnClassesThat()
        .areAssignableTo("com.cartisan.core.domain.AggregateRoot")
        .orShould()
        .dependOnClassesThat()
        .resideInAPackage("..domain.aggregate..")
        .orShould()
        .dependOnClassesThat()
        .resideInAPackage("..domain.entity..")
        .because("Controllers should access domain logic through AppServices, not directly")
        .allowEmptyShould(true);
```

- [ ] **Step 7: 验证修改**

```bash
grep -c "allowEmptyShould(true)" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java
```

预期：输出 5（确认 5 条规则都已添加）

- [ ] **Step 8: 提交修改**

```bash
git add cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java
git commit -m "refactor: add allowEmptyShould to CartisanLayeringRules

- Add .allowEmptyShould(true) to all 5 layering rules
- Rules now only apply when relevant layers exist
- Allows projects without certain layers to pass tests
- Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 修改 CartisanNamingRules（5 条规则）

**文件：** `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanNamingRules.java`

- [ ] **Step 1: 读取规则文件**

```bash
cat cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanNamingRules.java
```

预期：看到当前的 5 条规则定义

- [ ] **Step 2: 修改 controllersShouldBeSuffixed 规则（第 29-36 行）**

```java
@ArchTest
static final ArchRule controllersShouldBeSuffixed =
    classes()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .haveSimpleNameEndingWith("Controller")
        .because("REST controllers should be suffixed with 'Controller'")
        .allowEmptyShould(true);
```

- [ ] **Step 3: 修改 appServicesShouldBeSuffixed 规则（第 47-56 行）**

```java
@ArchTest
static final ArchRule appServicesShouldBeSuffixed =
    classes()
        .that()
        .areAnnotatedWith(Service.class)
        .and()
        .resideInAPackage("..application..")
        .should()
        .haveSimpleNameEndingWith("AppService")
        .because("Application services should be suffixed with 'AppService' to distinguish from domain services")
        .allowEmptyShould(true);
```

- [ ] **Step 4: 修改 domainServicesShouldBeSuffixed 规则（第 61-68 行）**

```java
@ArchTest
static final ArchRule domainServicesShouldBeSuffixed =
    classes()
        .that()
        .areAnnotatedWith(DomainService.class)
        .should()
        .haveSimpleNameEndingWith("Service")
        .because("Domain services should be suffixed with 'Service'")
        .allowEmptyShould(true);
```

- [ ] **Step 5: 修改 repositoriesShouldBeSuffixed 规则（第 73-80 行）**

```java
@ArchTest
static final ArchRule repositoriesShouldBeSuffixed =
    classes()
        .that()
        .areAnnotatedWith(Repository.class)
        .should()
        .haveSimpleNameEndingWith("Repository")
        .because("Repositories should be suffixed with 'Repository'")
        .allowEmptyShould(true);
```

- [ ] **Step 6: 修改 externalApiControllersMustContainVersion 规则（第 88-97 行）**

```java
@ArchTest
static final ArchRule externalApiControllersMustContainVersion =
    classes()
        .that()
        .areAnnotatedWith(RestController.class)
        .and()
        .resideInAPackage("..endpoints.api..")
        .should()
        .haveNameMatching(".*V\\d+.*")
        .because("External API controllers must include version number to avoid bean name conflicts")
        .allowEmptyShould(true);
```

- [ ] **Step 7: 验证修改**

```bash
grep -c "allowEmptyShould(true)" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanNamingRules.java
```

预期：输出 5（确认 5 条规则都已添加）

- [ ] **Step 8: 提交修改**

```bash
git add cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanNamingRules.java
git commit -m "refactor: add allowEmptyShould to CartisanNamingRules

- Add .allowEmptyShould(true) to all 5 naming rules
- Rules now only apply when relevant annotated classes exist
- Allows projects without certain components to pass tests
- Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 运行测试验证规则正确性

- [ ] **Step 1: 编译项目**

```bash
mvn clean compile -pl cartisan-test
```

预期：编译成功，无错误

- [ ] **Step 2: 运行所有 ArchUnit 测试**

```bash
mvn test -pl cartisan-test
```

预期：所有 67 个测试通过

- [ ] **Step 3: 验证规则总数**

```bash
# 统计所有 allowEmptyShould 的数量
echo "CartisanProhibitionRules: $(grep -c "allowEmptyShould(true)" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanProhibitionRules.java)"
echo "CartisanLayeringRules: $(grep -c "allowEmptyShould(true)" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java)"
echo "CartisanNamingRules: $(grep -c "allowEmptyShould(true)" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanNamingRules.java)"
echo "CartisanCodingStandardsRules: $(grep -c "allowEmptyShould(true)" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java)"
```

预期输出：
```
CartisanProhibitionRules: 3
CartisanLayeringRules: 5
CartisanNamingRules: 5
CartisanCodingStandardsRules: 3
```

总计：16 条规则都有 `.allowEmptyShould(true)`

- [ ] **Step 4: 提交验证结果**

```bash
# 如果测试通过，创建验证提交
git add cartisan-test/src/test/java/com/cartisan/test/archunit/
git commit -m "test: verify all ArchUnit rules pass with allowEmptyShould

- All 67 tests in cartisan-test pass
- Verified all 16 rules now have .allowEmptyShould(true)
- No breaking changes to existing functionality
- Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 更新文档（可选）

- [ ] **Step 1: 检查文档是否需要更新**

```bash
grep -n "allowEmptyShould" docs/guide/ArchUnit规则与编码规范映射.md | head -5
```

- [ ] **Step 2: 如需要，更新文档说明**

在文档中添加说明，解释所有规则现在都使用 `.allowEmptyShould(true)`：

```markdown
### 规则执行原则

所有 ArchUnit 规则都使用 `.allowEmptyShould(true)` 配置，确保：
- 规则只在存在相关代码时进行检查
- 项目不会被强制使用不存在的功能
- 遵循"规则应该作用于写了的，都用不到的，那强加规则没意思"的设计原则
```

- [ ] **Step 3: 提交文档更新**

```bash
git add docs/guide/ArchUnit规则与编码规范映射.md
git commit -m "docs: document allowEmptyShould principle for all ArchUnit rules

- Explain all rules use .allowEmptyShould(true)
- Document design principle: rules guard written code, not force unused features
- Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## 完成验证

- [ ] **最终验证**

```bash
# 检查所有修改已提交
git status

# 确认所有规则都有 allowEmptyShould
find cartisan-test/src/main/java/com/cartisan/test/archunit/ -name "*Rules.java" -exec grep -l "ArchRule" {} \; | while read f; do
  echo "=== $f ==="
  grep -c "allowEmptyShould(true)" "$f" || echo "0"
done

# 运行完整测试
mvn test -pl cartisan-test
```

预期：
- Git 工作区干净（无未提交修改）
- 所有 16 条规则都有 `.allowEmptyShould(true)`
- 所有 67 个测试通过

---

## 参考资料

- **设计文档**: `docs/superpowers/specs/2026-04-05-archunit-allowempty-design.md`
- **CartisanProhibitionRules.java**: `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanProhibitionRules.java`
- **CartisanLayeringRules.java**: `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java`
- **CartisanNamingRules.java**: `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanNamingRules.java`
- **CartisanCodingStandardsRules.java**: `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java`（已有 allowEmptyShould）
- **ArchUnit 官方文档**: https://www.archunit.org/userguide/html/000_Index.html
