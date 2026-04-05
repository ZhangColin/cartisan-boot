# ArchUnit MapStruct Mapper 规则实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 cartisan-test 模块添加第 15 条 ArchUnit 规则，要求所有 MapStruct Mapper 必须继承 DomainMapper 接口

**Architecture:** 在 CartisanCodingStandardsRules 中添加规则，通过检查 @org.mapstruct.Mapper 注解的接口是否实现 DomainMapper 接口来验证，使用 allowEmptyShould(true) 确保项目未使用 MapStruct 时规则仍然有效

**Tech Stack:** Java 21, ArchUnit 1.3, MapStruct, JUnit 5, AssertJ

---

## 文件结构概览

**修改文件**：
- `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java` - 添加新规则
- `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanArchRules.java` - 注册新规则
- `cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanCodingStandardsRulesTest.java` - 添加测试
- `docs/guide/cartisan-boot-使用手册.md` - 更新 cartisan-test 模块说明
- `docs/guide/ArchUnit规则与编码规范映射.md` - 更新规则映射

**新增文件**：
- `cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/compliant/application/GoodMapper.java` - 合规示例
- `cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/violation/shared/BadMapper.java` - 违规示例

---

## Task 1: 创建合规 Fixture（GoodMapper）

**Files:**
- Create: `cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/compliant/application/GoodMapper.java`

- [ ] **Step 1: 创建 GoodMapper.java**

创建接口文件，标注 @Mapper(componentModel = "spring") 注解，继承 DomainMapper<String, String> 接口。添加 JavaDoc 说明这是合规的 Mapper 示例。

文件内容参考：
```java
package com.cartisan.test.archunit.fixtures.compliant.application;

import com.cartisan.web.mapper.DomainMapper;
import org.mapstruct.Mapper;

/**
 * 合规：MapStruct Mapper 继承 DomainMapper
 * 符合规则：mapstructMappersShouldExtendDomainMapper
 */
@Mapper(componentModel = "spring")
public interface GoodMapper extends DomainMapper<String, String> {
    // convert 方法由 MapStruct 自动生成
}
```

- [ ] **Step 2: 验证编译**

Run: `cd cartisan-test && mvn compile`
Expected: 编译成功，无错误

- [ ] **Step 3: 提交**

Run: `git add cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/compliant/application/GoodMapper.java && git commit -m "test: add compliant Mapper fixture (GoodMapper)"`

---

## Task 2: 创建违规 Fixture（BadMapper）

**Files:**
- Create: `cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/violation/shared/BadMapper.java`

- [ ] **Step 1: 创建 BadMapper.java**

创建接口文件，标注 @Mapper(componentModel = "spring") 注解，但**不继承** DomainMapper 接口。添加 JavaDoc 说明这是违规的 Mapper 示例。

文件内容：
```java
package com.cartisan.test.archunit.fixtures.violation.shared;

import org.mapstruct.Mapper;

/**
 * 违规：MapStruct Mapper 不继承 DomainMapper
 * 违反规则：mapstructMappersShouldExtendDomainMapper
 */
@Mapper(componentModel = "spring")
public interface BadMapper {
    String convert(String input);
}
```

- [ ] **Step 2: 验证编译**

Run: `cd cartisan-test && mvn compile`
Expected: 编译成功，无错误

- [ ] **Step 3: 提交**

Run: `git add cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/violation/shared/BadMapper.java && git commit -m "test: add violating Mapper fixture (BadMapper)"`

---

## Task 3: 添加 ArchUnit 规则

**Files:**
- Modify: `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java`

- [ ] **Step 1: 在 CartisanCodingStandardsRules 类中添加新规则**

在类的末尾（在 domainEnumsShouldImplementBaseEnum 规则之后）添加新的静态字段。

规则定义：
- 字段名：`mapstructMappersShouldExtendDomainMapper`
- 注解：`@ArchTest`
- 规则逻辑：
  1. 选择所有接口（`areInterfaces()`）
  2. 且标注了 `@org.mapstruct.Mapper` 注解（`areAnnotatedWith("org.mapstruct.Mapper")`）
  3. 应该实现 `com.cartisan.web.mapper.DomainMapper` 接口（`implement("com.cartisan.web.mapper.DomainMapper")`）
  4. 原因说明（`because("MapStruct Mappers must extend DomainMapper...")`）
  5. 允许空检查（`allowEmptyShould(true)`）

参考 spec 文档中的完整代码示例。

- [ ] **Step 2: 更新类注释**

更新类的 JavaDoc 注释，将规则数量从 "1 条" 更新为 "2 条"。

原注释：
```java
/**
 * 编码规范规则 — 验证框架要求的编码规范
 *
 * <p>包含以下规则：</p>
 * <ul>
 *   <li>领域层枚举必须实现 BaseEnum 接口</li>
 * </ul>
```

更新为：
```java
/**
 * 编码规范规则 — 验证框架要求的编码规范
 *
 * <p>包含以下规则：</p>
 * <ul>
 *   <li>领域层枚举必须实现 BaseEnum 接口</li>
 *   <li>MapStruct Mapper 必须继承 DomainMapper 接口</li>
 * </ul>
```

- [ ] **Step 3: 验证编译**

Run: `cd cartisan-test && mvn compile`
Expected: 编译成功，无错误

- [ ] **Step 4: 提交**

Run: `git add cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java && git commit -m "test: add ArchUnit rule - MapStruct Mappers must extend DomainMapper"`

---

## Task 4: 注册规则到 CartisanArchRules

**Files:**
- Modify: `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanArchRules.java`

- [ ] **Step 1: 在 CartisanArchRules 类中注册新规则**

在类的末尾（在 domainEnumsShouldImplementBaseEnum 规则之后）添加新的静态字段，将 CartisanCodingStandardsRules 中的规则暴露出来。

添加内容：
```java
/**
 * MapStruct Mapper 必须继承 DomainMapper
 */
@ArchTest
static final ArchRule mapstructMappersShouldExtendDomainMapper =
    CartisanCodingStandardsRules.mapstructMappersShouldExtendDomainMapper;
```

位置参考：在 domainEnumsShouldImplementBaseEnum 字段之后。

- [ ] **Step 2: 更新类注释**

更新类的 JavaDoc 注释中的规则总数描述。

将：
```java
 * @see com.cartisan.test.archunit.CartisanCodingStandardsRules
```

更新为（如果规则列表需要显式说明，参考现有格式）：
- 检查类注释中是否有规则数量说明，如有则更新

- [ ] **Step 3: 验证编译**

Run: `cd cartisan-test && mvn compile`
Expected: 编译成功，无错误

- [ ] **Step 4: 提交**

Run: `git add cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanArchRules.java && git commit -m "test: register mapstructMappersShouldExtendDomainMapper rule in CartisanArchRules"`

---

## Task 5: 添加测试方法

**Files:**
- Modify: `cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanCodingStandardsRulesTest.java`

- [ ] **Step 1: 检查现有静态字段**

确认测试类中是否已存在 `compliantClasses` 和 `violatingClasses` 静态字段。

如果存在，跳过此步骤；如果不存在，添加：
```java
static final JavaClasses compliantClasses = new ClassFileImporter()
    .importPackages("com.cartisan.test.archunit.fixtures.compliant");

static final JavaClasses violatingClasses = new ClassFileImporter()
    .importPackages("com.cartisan.test.archunit.fixtures.violation");
```

- [ ] **Step 2: 添加正向测试方法**

添加测试方法验证合规代码通过规则。

方法定义：
```java
@Test
@DisplayName("mapstructMappersShouldExtendDomainMapper - 合规代码应该通过")
void mapstructMappersShouldExtendDomainMapper_passes_forCompliantCode() {
    assertThatCode(() ->
        CartisanCodingStandardsRules.mapstructMappersShouldExtendDomainMapper.check(compliantClasses)
    ).doesNotThrowAnyException();
}
```

- [ ] **Step 3: 添加负向测试方法**

添加测试方法验证违规代码被检测。

方法定义：
```java
@Test
@DisplayName("mapstructMappersShouldExtendDomainMapper - 违规代码应该失败")
void mapstructMappersShouldExtendDomainMapper_fails_forViolatingCode() {
    assertThatThrownBy(() ->
        CartisanCodingStandardsRules.mapstructMappersShouldExtendDomainMapper.check(violatingClasses)
    )
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("implement")
        .hasMessageContaining("DomainMapper");
}
```

- [ ] **Step 4: 运行测试验证**

Run: `cd cartisan-test && mvn test -Dtest=CartisanCodingStandardsRulesTest`
Expected: 所有测试通过，包括新增的两个测试方法

- [ ] **Step 5: 提交**

Run: `git add cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanCodingStandardsRulesTest.java && git commit -m "test: add tests for mapstructMappersShouldExtendDomainMapper rule"`

---

## Task 6: 更新使用手册

**Files:**
- Modify: `docs/guide/cartisan-boot-使用手册.md`

- [ ] **Step 1: 更新 cartisan-test 模块章节**

找到 `### 1.2 cartisan-test 模块` 章节，更新 ArchUnit 规则版本号和规则数量。

将：
```markdown
| **ArchUnit 规则（v1.1）** | DDD 分层、命名规范、禁止规则、编码规范规则的自动验证 |
```

更新为：
```markdown
| **ArchUnit 规则（v1.2）** | 15 条规则：分层、命名、禁止、编码规范规则的自动验证 |
```

- [ ] **Step 2: 添加架构测试使用方法章节**

在 cartisan-test 模块章节的末尾（或合适的子章节位置）添加新的小节。

添加位置参考：在现有 cartisan-test 内容之后，或作为独立的 `### 9.2 架构测试使用方法` 章节

添加内容包含：
1. 继承全部规则的示例代码
2. 15 条规则列表（分层 5 条、命名 5 条、禁止 3 条、编码规范 2 条）
3. 自定义规则示例
4. 选择性继承示例

参考 spec 文档 "三、文档更新" 章节的完整内容。

- [ ] **Step 3: 验证文档格式**

Run: `cd docs && grep -A 5 "ArchUnit 规则" guide/cartisan-boot-使用手册.md`
Expected: 看到更新后的 v1.2 版本号和 15 条规则说明

- [ ] **Step 4: 提交**

Run: `git add docs/guide/cartisan-boot-使用手册.md && git commit -m "docs: update usage manual - add ArchUnit testing guide (v1.2)"`

---

## Task 7: 更新映射文档

**Files:**
- Modify: `docs/guide/ArchUnit规则与编码规范映射.md`

- [ ] **Step 1: 更新映射概览表格**

找到 "映射概览" 章节，将规则总数从 14 更新为 15，编码规范规则从 1 更新为 2。

更新表格：
```markdown
| 规则类别 | 规则数量 | ... |
|---------|---------|-----|
| **编码规范规则** | 2 | ... |
| **总计** | 15 | ... |
```

- [ ] **Step 2: 添加第 15 条规则映射**

在"编码规范规则"章节，添加第 15 条规则的映射说明。

添加内容：
```markdown
#### 15. mapstructMappersShouldExtendDomainMapper

**ArchUnit 规则**：`CartisanCodingStandardsRules.mapstructMappersShouldExtendDomainMapper`

**约束内容**：标注了 `@org.mapstruct.Mapper` 注解的接口必须继承 `com.cartisan.web.mapper.DomainMapper<T, E>` 接口

**编码规范对应**：
- **章节**：四、应用层 → 4.3 Mapper 规范
- **章节**：十、架构守护 → 10.2 检查清单
- **原文**：引用文档中 Mapper 继承 DomainMapper 的要求和示例

**验证状态**：✅ 完全对应

**补充说明**：
- MapStruct 编译时验证泛型类型，ArchUnit 只检查接口继承关系
- 使用字符串路径避免硬依赖 MapStruct
- allowEmptyShould(true) 确保项目未使用 MapStruct 时规则不失败
```

参考 spec 文档中的完整映射内容。

- [ ] **Step 3: 更新结论章节**

更新 "总体评估" 章节，将规则总数从 14 更新为 15，规则版本从 v1.1 更新为 v1.2。

- [ ] **Step 4: 验证文档一致性**

Run: `cd docs && grep -n "15 条规则" guide/ArchUnit规则与编码规范映射.md`
Expected: 至少找到 2 处更新（概览表格和结论章节）

- [ ] **Step 5: 提交**

Run: `git add docs/guide/ArchUnit规则与编码规范映射.md && git commit -m "docs: update ArchUnit mapping document - add rule #15 (v1.2)"`

---

## Task 8: 验证和总结

- [ ] **Step 1: 运行所有测试**

Run: `cd cartisan-test && mvn test`
Expected: 所有测试通过，包括新增的 mapstructMappersShouldExtendDomainMapper 测试

- [ ] **Step 2: 验证文档完整性**

Run: `git diff HEAD~5 docs/guide/`
Expected: 看到使用手册和映射文档的更新

- [ ] **Step 3: 检查代码变更**

Run: `git diff HEAD~5 cartisan-test/src/main/java/com/cartisan/test/archunit/ cartisan-test/src/test/java/com/cartisan/test/archunit/`
Expected:
- CartisanCodingStandardsRules.java 添加了新规则
- CartisanArchRules.java 注册了新规则
- CartisanCodingStandardsRulesTest.java 添加了测试方法
- 两个新 fixture 文件已创建

- [ ] **Step 4: 创建总结文档**

创建完成总结，说明：
- 新增规则数量：1 条（第 15 条）
- 规则版本：v1.1 → v1.2
- 测试覆盖：正向和负向测试均通过
- 文档更新：使用手册和映射文档已同步更新

- [ ] **Step 5: 推送变更**

Run: `git push origin develop`
Expected: 所有提交成功推送到远程仓库

---

## 验收标准

### 功能验收
- ✅ 新规则在 cartisan-test 模块中生效
- ✅ 正向测试通过（GoodMapper 符合规则）
- ✅ 负向测试通过（BadMapper 被正确检测）
- ✅ 所有现有测试继续通过

### 文档验收
- ✅ 使用手册新增"架构测试使用方法"章节
- ✅ 映射文档更新规则总数和映射关系
- ✅ 版本号统一更新为 v1.2

### 性能验收
- ✅ ArchUnit 测试执行时间 < 30 秒
- ✅ mvn test 全量测试通过

---

**实施计划版本**：v1.0
**创建日期**：2026-04-05
**对应设计文档**：docs/superpowers/specs/2026-04-05-archunit-mapstruct-mapper-rule.md
