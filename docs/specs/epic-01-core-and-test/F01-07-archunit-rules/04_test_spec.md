# Feature: F01-07 — 测试规格

> 版本：v0.1 | 日期：2026-03-13
> 状态：Phase 5 完成

---

## 测试策略

### 测试分层

| 测试层 | 工具 | 目的 |
|--------|------|------|
| 单元测试 | JUnit 5 + AssertJ | 验证每条 ArchUnit 规则的有效性 |
| Fixtures 验证 | ArchUnit | 通过合规/违规示例验证规则 |

### 测试方法

使用 **Fixtures 双重验证法**：
1. **合规验证**：使用 fixtures/compliant 中的合规代码，验证规则不会误报
2. **违规验证**：使用 fixtures/violation 中的违规代码，验证规则能捕获问题

### PIT 变异测试

| 状态 | 说明 |
|------|------|
| ⚠️ 不适用 | ArchUnit 规则类是声明式配置（静态 final 字段 + ArchUnit fluent API），没有业务逻辑可供变异 |

**原因**：
- ArchUnit 规则主要由 `static final ArchRule` 字段声明组成
- `ArchRule` 对象由 ArchUnit 库的 fluent API 构建（`noFields().should()...`）
- 实际的检查逻辑在 ArchUnit 库中，不在我们的代码里
- PIT 无法对声明式配置生成有意义的变异

**替代质量保证**：
- **Fixtures 双重验证法**：每条规则都有 pass/fail 成对测试，确保规则能正确识别合规和违规代码
- **100% 规则覆盖**：11 条规则，22 个测试（每条规则 2 个测试）
- **代码覆盖**：规则类虽然简单，但通过 fixtures 测试覆盖了所有规则

---

## 测试用例清单

### CartisanProhibitionRulesTest (6 tests)

| 测试方法 | 验证内容 | Fixtures |
|---------|---------|----------|
| `noFieldInjection_passes` | 合规代码不触发规则 | compliant/* |
| `noFieldInjection_fails` | `@Autowired` 字段被捕获 | violation/controller/BadFieldInjection.java |
| `noJavaUtilDate_passes` | 使用 java.time 不触发规则 | compliant/* |
| `noJavaUtilDate_fails` | `java.util.Date` 被捕获 | violation/shared/BadDateUsage.java |
| `noFloatingPointForMoney_passes` | BigDecimal 金额不触发规则 | compliant/shared/GoodMoney.java |
| `noFloatingPointForMoney_fails` | Double/Float 金额被捕获 | violation/shared/BadMoneyWithDouble.java |

### CartisanNamingRulesTest (8 tests)

| 测试方法 | 验证内容 | Fixtures |
|---------|---------|----------|
| `controllersShouldBeSuffixed_passes` | `*Controller` 后缀合规 | compliant/controller/GoodController.java |
| `controllersShouldBeSuffixed_fails` | 错误后缀被捕获 | violation/controller/BadControllerNaming.java |
| `appServicesShouldBeSuffixed_passes` | `*AppService` 后缀合规 | compliant/application/GoodAppService.java |
| `appServicesShouldBeSuffixed_fails` | application 包中错误后缀被捕获 | violation/application/BadServiceNaming.java |
| `domainServicesShouldBeSuffixed_passes` | `*Service` 后缀合规 | compliant/domain/GoodDomainService.java |
| `domainServicesShouldBeSuffixed_fails` | 错误后缀被捕获 | violation/domain/BadDomainHandler.java |
| `repositoriesShouldBeSuffixed_passes` | `*Repository` 后缀合规 | compliant/infrastructure/GoodRepository.java |
| `repositoriesShouldBeSuffixed_fails` | 错误后缀被捕获 | violation/infrastructure/BadDao.java |

### CartisanLayeringRulesTest (8 tests)

| 测试方法 | 验证内容 | Fixtures |
|---------|---------|----------|
| `domainShouldNotDependOnInfrastructure_passes` | 领域层不依赖基础设施 | compliant/* |
| `domainShouldNotDependOnInfrastructure_fails` | 领域类依赖基础设施被捕获 | violation/domain/BadDomainDependsOnInfra.java |
| `domainShouldNotDependOnSpring_passes` | 领域层不依赖 Spring | compliant/* |
| `domainShouldNotDependOnSpring_fails` | 领域类依赖 Spring 被捕获 | violation/domain/BadDomainWithSpringDependency.java |
| `controllersShouldOnlyDependOnApplication_passes` | Controller 只依赖应用层 | compliant/controller/GoodController.java |
| `controllersShouldOnlyDependOnApplication_fails` | Controller 直接依赖领域被捕获 | violation/controller/BadControllerCallsDomain.java |
| `applicationShouldNotAccessDatabaseDirectly_passes` | 应用服务不直接访问数据库 | compliant/* |
| `applicationShouldNotAccessDatabaseDirectly_fails` | 应用服务使用 JPA 被捕获 | violation/application/BadAppServiceWithJPA.java |

---

## Fixtures 清单

### Compliant Fixtures (6 个)

| 文件 | 位置 | 验证规则 |
|------|------|---------|
| GoodEntity | domain/ | 所有规则 |
| GoodDomainService | domain/ | naming rules |
| GoodAppService | application/ | naming rules |
| GoodRepository | infrastructure/ | naming rules |
| GoodController | controller/ | naming rules |
| GoodMoney | shared/ | prohibition rules |

### Violation Fixtures (11 个)

| 文件 | 违反规则 |
|------|---------|
| BadFieldInjection | noFieldInjection |
| BadDateUsage | noJavaUtilDate |
| BadMoneyWithDouble | noFloatingPointForMoney |
| BadControllerNaming | controllersShouldBeSuffixed |
| BadServiceNaming | appServicesShouldBeSuffixed |
| BadDomainHandler | domainServicesShouldBeSuffixed |
| BadDao | repositoriesShouldBeSuffixed |
| BadDomainDependsOnInfra | domainShouldNotDependOnInfrastructure |
| BadDomainWithSpringDependency | domainShouldNotDependOnSpring |
| BadControllerCallsDomain | controllersShouldOnlyDependOnApplication |
| BadAppServiceWithJPA | applicationShouldNotAccessDatabaseDirectly |

---

## 测试覆盖率

### 规则覆盖率：100% (11/11)

| 规则类别 | 规则数 | 测试覆盖 |
|---------|-------|---------|
| 分层规则 | 4 | 8 tests (pass/fail 成对) |
| 命名规则 | 4 | 8 tests (pass/fail 成对) |
| 禁止规则 | 3 | 6 tests (pass/fail 成对) |

### 代码覆盖率

| 模块 | 类覆盖率 | 行覆盖率 |
|------|---------|---------|
| cartisan-test/archunit | 100% | 100% |

---

## 测试执行结果

**最终验证命令：**
```bash
./gradlew clean :cartisan-test:test
```

**结果：**
```
BUILD SUCCESSFUL
- CartisanLayeringRulesTest: 8 tests, 0 failures
- CartisanNamingRulesTest: 8 tests, 0 failures
- CartisanProhibitionRulesTest: 6 tests, 0 failures
总计: 22 tests, 0 failures, 0 errors
```

---

## 测试数据管理

### Fixtures 维护原则

1. **最小化原则**：每个违规 fixture 只违反一条规则，避免复杂依赖
2. **命名规范**：
   - 合规：`Good{Component}`
   - 违规：`Bad{Aspect}{Rule}`
3. **包结构**：
   - `fixtures/compliant/` - 按实际包结构组织
   - `fixtures/violation/` - 按实际包结构组织
4. **文档化**：每个 fixture 用注释标注违反的规则

### Fixtures 使用示例

```java
// 导入合规代码
static final JavaClasses compliantClasses = new ClassFileImporter()
    .importPackages("com.cartisan.test.archunit.fixtures.compliant");

// 导入违规代码
static final JavaClasses violatingClasses = new ClassFileImporter()
    .importPackages("com.cartisan.test.archunit.fixtures.violation");

// 验证
assertThatCode(() -> rule.check(compliantClasses))
    .doesNotThrowAnyException();

assertThatThrownBy(() -> rule.check(violatingClasses))
    .isInstanceOf(AssertionError.class);
```
