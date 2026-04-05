# ArchUnit 枚举规则调整实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 调整 ArchUnit 规则，排除 CodeMessage 枚举的 BaseEnum 要求，并防止两个接口体系混淆

**架构:** 通过两条独立的 ArchUnit 规则实现：一条要求普通领域枚举实现 BaseEnum（排除 CodeMessage），另一条禁止 CodeMessage 枚举实现 BaseEnum

**技术栈:** ArchUnit (TngTech ArchUnit 1.x), JUnit 5

---

## Task 1: 修改现有规则排除 CodeMessage 枚举

**Files:**
- Modify: `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java:28-37`

- [ ] **Step 1: 读取现有规则文件**

```bash
cat cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java
```

预期：看到当前的 `domainEnumsShouldImplementBaseEnum` 规则定义

- [ ] **Step 2: 修改规则，添加 CodeMessage 排除条件**

将现有的 `domainEnumsShouldImplementBaseEnum` 规则修改为：

```java
/**
 * 领域层枚举必须实现 BaseEnum（CodeMessage 枚举除外）
 *
 * <p>确保枚举与 Integer 的自动转换。</p>
 * <p>BaseEnum 接口提供 code/name 映射，是框架枚举处理的基础。</p>
 * <p>CodeMessage 枚举用于异常处理，不需要实现 BaseEnum。</p>
 */
@ArchTest
static final ArchRule domainEnumsShouldImplementBaseEnum =
    classes()
        .that()
        .areEnums()
        .and()
        .resideInAPackage("..domain..")
        .and()
        .doNotImplement("com.cartisan.core.exception.CodeMessage")
        .should()
        .implement("com.cartisan.core.domain.BaseEnum")
        .because("Domain enums must implement BaseEnum for automatic Integer conversion (except CodeMessage enums)")
        .allowEmptyShould(true);
```

关键变化：
- 添加 `.and().doNotImplement("com.cartisan.core.exception.CodeMessage")`
- 更新 JavaDoc 说明 CodeMessage 枚举除外
- 更新 `because()` 子句说明例外情况

- [ ] **Step 3: 验证修改**

```bash
grep -A 15 "domainEnumsShouldImplementBaseEnum =" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java
```

预期：看到修改后的规则包含 `doNotImplement("com.cartisan.core.exception.CodeMessage")`

- [ ] **Step 4: 提交修改**

```bash
git add cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java
git commit -m "refactor: exclude CodeMessage enums from BaseEnum requirement

- Add doNotImplement() clause to domainEnumsShouldImplementBaseEnum rule
- CodeMessage enums for exception handling should not be required to implement BaseEnum
- Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 添加新规则防止 CodeMessage 枚举实现 BaseEnum

**Files:**
- Modify: `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java` (添加在文件末尾，类定义内)

- [ ] **Step 1: 定位插入位置**

```bash
grep -n "allowEmptyShould(true);" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java | tail -1
```

预期：找到最后一个规则的结束位置（约第 55 行）

- [ ] **Step 2: 添加新规则**

在 `mapstructMappersShouldExtendDomainMapper` 规则之后、类结束之前添加：

```java
/**
 * CodeMessage 枚举不应实现 BaseEnum
 *
 * <p>避免两个独立的接口体系产生混淆。</p>
 * <p>CodeMessage 用于异常处理（code/message/httpStatus），
 * BaseEnum 用于业务值枚举（code/name）。</p>
 */
@ArchTest
static final ArchRule codeMessageEnumsShouldNotImplementBaseEnum =
    classes()
        .that()
        .areEnums()
        .and()
        .resideInAPackage("..domain..")
        .and()
        .implement("com.cartisan.core.exception.CodeMessage")
        .should()
        .notImplement("com.cartisan.core.domain.BaseEnum")
        .because("CodeMessage enums should not implement BaseEnum to avoid confusion")
        .allowEmptyShould(true);
```

插入位置：在第 55 行（`mapstructMappersShouldExtendDomainMapper` 规则的 `allowEmptyShould(true);` 之后）

- [ ] **Step 3: 验证添加**

```bash
grep -A 15 "codeMessageEnumsShouldNotImplementBaseEnum =" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java
```

预期：看到新添加的完整规则定义

- [ ] **Step 4: 提交添加**

```bash
git add cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java
git commit -m "feat: add rule to prevent CodeMessage enums from implementing BaseEnum

- Add codeMessageEnumsShouldNotImplementBaseEnum rule
- Prevent confusion between two independent interface systems
- Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 在 CartisanArchRules 中注册新规则

**Files:**
- Modify: `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanArchRules.java`

- [ ] **Step 1: 读取规则聚合文件**

```bash
cat cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanArchRules.java
```

预期：看到现有的规则注册结构

- [ ] **Step 2: 在 domainEnumsShouldImplementBaseEnum 规则之后添加新规则注册**

找到 `domainEnumsShouldImplementBaseEnum` 规则注册（约第 115-120 行），在其后添加：

```java
/**
 * CodeMessage 枚举不应实现 BaseEnum
 */
@ArchTest
static final ArchRule codeMessageEnumsShouldNotImplementBaseEnum =
    CartisanCodingStandardsRules.codeMessageEnumsShouldNotImplementBaseEnum;
```

插入位置：在第 120 行（`domainEnumsShouldImplementBaseEnum` 规则注册之后）

- [ ] **Step 3: 验证添加**

```bash
grep -B 2 -A 3 "codeMessageEnumsShouldNotImplementBaseEnum" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanArchRules.java
```

预期：看到新规则注册及其 JavaDoc

- [ ] **Step 4: 提交添加**

```bash
git add cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanArchRules.java
git commit -m "feat: register codeMessageEnumsShouldNotImplementBaseEnum rule in CartisanArchRules

- Enable new rule in aggregate rules class
- Business projects inheriting CartisanArchRules automatically get the new rule
- Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 运行测试验证规则正确性

**Files:**
- Test: `cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanCodingStandardsRulesTest.java`

- [ ] **Step 1: 编译项目**

```bash
mvn clean compile -pl cartisan-test
```

预期：编译成功，无错误

- [ ] **Step 2: 运行 ArchUnit 测试**

```bash
mvn test -pl cartisan-test -Dtest=CartisanCodingStandardsRulesTest
```

预期：测试通过，包括新规则

- [ ] **Step 3: 运行完整的架构测试**

```bash
mvn test -pl cartisan-test
```

预期：所有 ArchUnit 规则测试通过

- [ ] **Step 4: 检查 BaseCodeMessage 不触发规则失败**

```bash
mvn test -pl cartisan-core -Dtest=BaseCodeMessageTest
```

预期：`BaseCodeMessage` 相关测试通过（虽然它不在 domain 包，但验证规则不会误伤正常枚举）

- [ ] **Step 5: 提交验证（如有测试文件更新）**

```bash
# 如果测试文件有更新
git add cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanCodingStandardsRulesTest.java
git commit -m "test: verify enum rule adjustments work correctly

- Confirm CodeMessage enums are excluded from BaseEnum requirement
- Confirm new rule prevents CodeMessage enums from implementing BaseEnum
- Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 更新相关文档

**Files:**
- Update: `docs/guide/ArchUnit规则与编码规范映射.md` (如存在)

- [ ] **Step 1: 检查是否存在映射文档**

```bash
ls -la docs/guide/ArchUnit规则与编码规范映射.md
```

- [ ] **Step 2: 如果文档存在，更新枚举规则说明**

在文档中找到枚举规则相关部分，更新为：

```markdown
### 领域层枚举规则

#### 规则 1: 领域枚举必须实现 BaseEnum（CodeMessage 枚举除外）

**规则**: `domainEnumsShouldImplementBaseEnum`
**目的**: 确保需要自动转换的业务值枚举实现 BaseEnum
**范围**: `domain` 包下的枚举（排除实现 CodeMessage 接口的枚举）

#### 规则 2: CodeMessage 枚举不应实现 BaseEnum

**规则**: `codeMessageEnumsShouldNotImplementBaseEnum`
**目的**: 防止 CodeMessage 枚举与 BaseEnum 接口体系混淆
**范围**: `domain` 包下实现 CodeMessage 接口的枚举
```

- [ ] **Step 3: 提交文档更新**

```bash
git add docs/guide/ArchUnit规则与编码规范映射.md
git commit -m "docs: update ArchUnit rule mapping for enum rules

- Document the two-rule approach for enum validation
- Clarify CodeMessage enum exclusion from BaseEnum requirement
- Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## 完成验证

- [ ] **所有任务完成后的最终验证**

```bash
# 检查所有修改已提交
git status

# 运行全量测试（可选，需要 Docker）
mvn test

# 或只运行单元测试
mvn test -pl cartisan-test,cartisan-core
```

预期：
- Git 工作区干净（无未提交修改）
- 所有测试通过
- 新规则生效且不破坏现有功能

---

## 参考资料

- **设计文档**: `docs/superpowers/specs/2026-04-05-enum-rule-adjustment-design.md`
- **BaseEnum 接口**: `cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java`
- **CodeMessage 接口**: `cartisan-core/src/main/java/com/cartisan/core/exception/CodeMessage.java`
- **BaseCodeMessage 示例**: `cartisan-core/src/main/java/com/cartisan/core/exception/BaseCodeMessage.java`
- **ArchUnit 官方文档**: https://www.archunit.org/userguide/html/000_Index.html
