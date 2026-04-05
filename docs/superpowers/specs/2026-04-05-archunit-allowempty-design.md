# ArchUnit 规则统一添加 allowEmptyShould 设计

**日期**: 2026-04-05
**状态**: 设计中
**负责人**: Claude

## 背景

### 当前问题

在 ArchUnit 规则中，当规则使用 `noClasses()` 或 `noFields()` 作为起始点时，如果没有匹配到任何类或字段，规则会抛出异常。这导致：

**问题场景：**
- 业务项目没有使用某类功能（如没有 Controller、没有金额字段）
- ArchUnit 规则检查时匹配不到任何类
- 规则误报错，强制项目必须存在相关代码

**设计原则：**
> "规则应该作用于写了的，都用不到的，那强加规则没意思。"

规则应该是防护性的，只在存在相关代码时进行检查，而不是强制项目必须使用某类功能。

### 当前状态

**已有 `.allowEmptyShould(true)` 的规则：**
- ✅ `CartisanCodingStandardsRules` - 3 条规则

**缺少 `.allowEmptyShould(true)` 的规则：**
- ❌ `CartisanLayeringRules` - 5 条规则
- ❌ `CartisanNamingRules` - 5 条规则
- ❌ `CartisanProhibitionRules` - 3 条规则

**总计：13 条规则需要添加 `.allowEmptyShould(true)`**

## 设计方案

### 核心修改

为所有缺少 `.allowEmptyShould(true)` 的 ArchUnit 规则统一添加此配置。

### 修改模式

在每个规则定义的末尾添加 `.allowEmptyShould(true)`：

**修改前：**
```java
static final ArchRule noFloatingPointForMoney =
    noFields()
        .that()
        .haveNameMatching(".*(?i)(price|amount|fee|cost|balance|money|payment|refund|commission).*")
        .should()
        .haveRawType(Double.class)
        .orShould()
        .haveRawType(Float.class)
        .because("Use BigDecimal for monetary fields to avoid precision loss");
```

**修改后：**
```java
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

### 修改范围

#### 1. CartisanProhibitionRules（3 条规则）

**文件：** `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanProhibitionRules.java`

- `noFieldInjection`（第 28 行）
- `noJavaUtilDate`（第 41 行）
- `noFloatingPointForMoney`（第 58 行）

#### 2. CartisanLayeringRules（5 条规则）

**文件：** `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java`

- `domainShouldNotDependOnInfrastructure`（第 28 行）
- `domainShouldNotDependOnSpring`（第 47 行）
- `controllersShouldOnlyDependOnApplication`（第 68 行）
- `applicationShouldNotAccessDatabaseDirectly`（第 90 行）
- `controllersShouldNotDependOnAggregates`（第 112 行）

#### 3. CartisanNamingRules（5 条规则）

**文件：** `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanNamingRules.java`

- `controllersShouldBeSuffixed`（第 30 行）
- `appServicesShouldBeSuffixed`（第 48 行）
- `domainServicesShouldBeSuffixed`（第 62 行）
- `repositoriesShouldBeSuffixed`（第 74 行）
- `externalApiControllersMustContainVersion`（第 89 行）

#### 4. CartisanCodingStandardsRules

**无需修改** - 3 条规则已全部有 `.allowEmptyShould(true)`

## 测试策略

### 验证目标

1. 确保添加 `.allowEmptyShould(true)` 后规则逻辑不变
2. 验证现有违规代码仍能被正确检测
3. 确认在没有匹配代码时规则不会报错

### 测试命令

```bash
# 运行 cartisan-test 模块的所有测试
mvn test -pl cartisan-test

# 预期结果：67 个测试全部通过
```

### 测试覆盖

现有测试用例已覆盖：
- ✅ `CartisanLayeringRulesTest` - 10 个测试
- ✅ `CartisanNamingRulesTest` - 10 个测试
- ✅ `CartisanProhibitionRulesTest` - 6 个测试
- ✅ `CartisanCodingStandardsRulesTest` - 3 个测试

## 优先级

**优先级：高**

**理由：**
- 影响业务项目的开发体验
- 不应该强制项目使用不存在的功能
- 统一所有规则的行为，减少混淆

## 向后兼容性

**影响：无破坏性变更**

- 对于有违规代码的项目：规则仍然会检测到违规
- 对于没有相关代码的项目：规则不再误报错
- 对于 cartisan-boot 框架本身：测试全部通过，不受影响

## 实施计划

详见实施计划文档：`docs/superpowers/plans/YYYY-MM-DD-archunit-allowempty.md`

## 参考资料

- ArchUnit 官方文档：https://www.archunit.org/userguide/html/000_Index.html
- `CartisanProhibitionRules.java` - 禁止规则定义
- `CartisanLayeringRules.java` - 分层规则定义
- `CartisanNamingRules.java` - 命名规则定义
- `CartisanCodingStandardsRules.java` - 编码规范规则定义（已有 allowEmptyShould）
