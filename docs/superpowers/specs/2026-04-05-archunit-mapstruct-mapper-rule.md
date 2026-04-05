# ArchUnit MapStruct Mapper 规则设计文档

> **版本**：v1.0
> **日期**：2026-04-05
> **状态**：待实现
> **Epic**：增强 cartisan-boot 框架架构测试能力

---

## 一、背景

### 1.1 问题说明

当前 cartisan-test 模块提供了 14 条 ArchUnit 架构测试规则，用于约束业务平台的代码架构。这些规则与《限界上下文代码编写规范》文档保持一致。

经审查，编码规范"十、架构守护 → 10.2 检查清单"中有一项约束可以用 ArchUnit 验证，但目前缺失：

**缺失规则**：Mapper 继承 `DomainMapper<T, E>` 接口

### 1.2 业务价值

添加此规则可以：
1. **确保一致性**：所有 MapStruct Mapper 统一继承 `DomainMapper` 基类
2. **自动获得功能**：继承后自动获得批量转换方法（`convertList`、`convertSet`）
3. **强制规范执行**：通过自动化测试约束，而非仅靠文档说明

### 1.3 技术约束

- MapStruct 的 `@Mapper` 注解路径：`org.mapstruct.Mapper`
- `DomainMapper` 接口路径：`com.cartisan.web.mapper.DomainMapper<T, E>`
- 框架自身已有符合规范的 Mapper：`cartisan-web/src/test/java/com/cartisan/web/mapper/fixtures/TestMapper`

---

## 二、设计方案

### 2.1 规则设计

**规则名称**：`mapstructMappersShouldExtendDomainMapper`

**规则类别**：编码规范规则（Coding Standards Rules）

**约束内容**：
- 所有标注了 `@org.mapstruct.Mapper` 注解的接口
- 必须继承 `com.cartisan.web.mapper.DomainMapper<T, E>` 接口

**规则目的**：
- 确保 MapStruct Mapper 统一继承 `DomainMapper` 基类
- 自动获得批量转换方法（`convertList`、`convertSet`）
- 保持 Mapper 的一致性

**实现位置**：
`cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java`

**规则代码**：
```java
/**
 * MapStruct Mapper 必须继承 DomainMapper
 *
 * <p>确保 Mapper 统一继承 DomainMapper 基类，自动获得批量转换方法。</p>
 * <p>DomainMapper 提供 convert/convertList/convertSet 方法，是框架 Mapper 处理的基础。</p>
 */
@ArchTest
static final ArchRule mapstructMappersShouldExtendDomainMapper =
    classes()
        .that()
        .areInterfaces()
        .and()
        .areAnnotatedWith("org.mapstruct.Mapper")
        .should()
        .implement("com.cartisan.web.mapper.DomainMapper")
        .because("MapStruct Mappers must extend DomainMapper for consistency and utility methods")
        .allowEmptyShould(true);
```

### 2.2 测试设计

#### 2.2.1 正向测试

**测试目的**：验证框架自己的 Mapper 通过规则

**测试数据**：
- ✅ `cartisan-web/src/test/java/com/cartisan/web/mapper/fixtures/TestMapper`
  - 标注了 `@Mapper` 注解
  - 继承了 `DomainMapper<SimpleEntity, SimpleDto>`

**测试方法**：
```java
@Test
@DisplayName("mapstructMappersShouldExtendDomainMapper - cartisan 框架应该通过")
void mapstructMappersShouldExtendDomainMapper_passes_forCartisanFramework() {
    assertThatCode(() ->
        CartisanCodingStandardsRules.mapstructMappersShouldExtendDomainMapper.check(cartisanClasses)
    ).doesNotThrowAnyException();
}
```

**测试位置**：
`cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanCodingStandardsRulesTest.java`

#### 2.2.2 负向测试

**测试目的**：验证违反规则的 Mapper 被正确检测

**测试数据文件**：
`cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/violation/application/BadMapper.java`

```java
package com.cartisan.test.archunit.fixtures.violation.application;

import org.mapstruct.Mapper;

/**
 * 违规：MapStruct Mapper 不继承 DomainMapper
 * 违反规则：mapstructMappersShouldExtendDomainMapper
 */
@Mapper(componentModel = "spring")
public interface BadMapper {
    // ❌ 应该继承 DomainMapper<T, E>
    String convert(String input);
}
```

**测试方法**：
```java
@Test
@DisplayName("mapstructMappersShouldExtendDomainMapper - 应检测到未继承 DomainMapper 的 Mapper")
void mapstructMappersShouldExtendDomainMapper_detects_Mapper_not_extending_DomainMapper() {
    JavaClasses testClasses = new ClassFileImporter()
        .importPackages("com.cartisan.test.archunit.fixtures.violation");

    assertThatThrownBy(() ->
        CartisanCodingStandardsRules.mapstructMappersShouldExtendDomainMapper.check(testClasses)
    )
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("DomainMapper");
}
```

**测试位置**：
`cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanCodingStandardsRulesTest.java`

### 2.3 规则注册

**修改位置**：
`cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanArchRules.java`

**添加代码**：
```java
/**
 * MapStruct Mapper 必须继承 DomainMapper
 */
@ArchTest
static final ArchRule mapstructMappersShouldExtendDomainMapper =
    CartisanCodingStandardsRules.mapstructMappersShouldExtendDomainMapper;
```

**更新注释**：将规则总数从 14 条更新为 15 条

---

## 三、文档更新

### 3.1 使用手册更新

**更新位置**：`docs/guide/cartisan-boot-使用手册.md`

**更新章节**：cartisan-test 模块章节

**新增内容**：

```markdown
### 9.2 架构测试使用方法

#### 继承全部规则

业务平台通过继承 `CartisanArchRules` 即可获得完整的架构守护：

```java
@AnalyzeClasses(packages = "com.yourcompany")
public class ArchitectureTest extends CartisanArchRules {
    // 完了。所有规则自动生效。
}
```

#### 规则列表

当前框架提供 **15 条架构规则**：

**分层规则**（5 条）：
- 领域层不能依赖基础设施层
- 领域层不能依赖 Spring
- Controller 只能依赖应用层
- 应用服务不能直接操作数据库
- Controller 不应依赖聚合根

**命名规则**（5 条）：
- Controller 命名规范
- 应用服务命名规范
- 领域服务命名规范
- Repository 命名规范
- 外部 API Controller 版本号

**禁止规则**（3 条）：
- 禁止字段注入
- 禁止使用 java.util.Date
- 禁止金额字段使用浮点数

**编码规范规则**（2 条）：
- 领域层枚举必须实现 BaseEnum
- MapStruct Mapper 必须继承 DomainMapper

#### 自定义规则

如需添加平台特有的规则，可以在测试类中追加 `@ArchTest` 字段：

```java
@AnalyzeClasses(packages = "com.yourcompany")
public class ArchitectureTest extends CartisanArchRules {

    @ArchTest
    static final ArchRule myCustomRule = classes()
        .that()
        .resideInAPackage("..mypackage..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage("..mypackage..", "java..");
}
```

#### 选择性继承

如只需部分规则，可只继承特定的规则类：

```java
@AnalyzeClasses(packages = "com.yourcompany")
public class LayeringTest extends CartisanLayeringRules {
    // 只继承分层规则
}
```

或者不继承，直接在测试类中声明需要的规则字段。
```

### 3.2 映射文档更新

**更新位置**：`docs/guide/ArchUnit规则与编码规范映射.md`

**更新内容**：
1. 将规则总数从 14 条更新为 15 条
2. 在"编码规范规则"章节添加第 15 条规则的映射说明
3. 更新验证状态为"✅ 完全覆盖"

---

## 四、实施计划

### 4.1 文件变更清单

**修改文件**（4 个）：
1. `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanCodingStandardsRules.java`
   - 添加 `mapstructMappersShouldExtendDomainMapper` 规则

2. `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanArchRules.java`
   - 注册新规则
   - 更新类注释（14 条 → 15 条）

3. `cartisan-test/src/test/java/com/cartisan/test/archunit/CartisanCodingStandardsRulesTest.java`
   - 添加正向测试方法
   - 添加负向测试方法

4. `docs/guide/cartisan-boot-使用手册.md`
   - 添加"9.2 架构测试使用方法"章节

**新增文件**（1 个）：
1. `cartisan-test/src/test/java/com/cartisan/test/archunit/fixtures/violation/application/BadMapper.java`
   - 违规测试数据

### 4.2 实施步骤

1. **添加规则**：在 `CartisanCodingStandardsRules.java` 中添加新规则
2. **注册规则**：在 `CartisanArchRules.java` 中注册新规则
3. **创建测试数据**：创建 `BadMapper.java` 违规示例
4. **添加测试**：在 `CartisanCodingStandardsRulesTest.java` 中添加正向和负向测试
5. **验证测试**：运行测试确保通过
6. **更新文档**：更新使用手册和映射文档
7. **提交变更**：创建 git commit

### 4.3 验收标准

**功能验收**：
- ✅ 新规则在 cartisan-test 模块中生效
- ✅ 正向测试通过（框架的 TestMapper 符合规则）
- ✅ 负向测试通过（BadMapper 被正确检测）
- ✅ 所有现有测试继续通过

**文档验收**：
- ✅ 使用手册新增"架构测试使用方法"章节
- ✅ 映射文档更新规则总数和映射关系

**性能验收**：
- ✅ ArchUnit 测试执行时间 < 30 秒

---

## 五、风险评估

### 5.1 技术风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| 业务平台现有 Mapper 不符合规则 | 测试失败 | 低 | 规则添加 `allowEmptyShould(true)`，允许空检查 |
| MapStruct 版本兼容性问题 | 规则失效 | 极低 | 使用字符串路径 `"org.mapstruct.Mapper"` 而非类引用 |

### 5.2 兼容性

**框架兼容性**：
- ✅ cartisan-web 模块的 `TestMapper` 已符合规则
- ✅ 规则只检查接口，不影响 MapStruct 生成的实现类

**业务平台兼容性**：
- ⚠️ 现有业务平台的 Mapper 可能不符合规则
- 建议：在文档中提供迁移指南

---

## 六、后续工作

### 6.1 可选增强

以下规则在编码规范中有说明，但经讨论决定**不添加**（仅通过文档和代码审查保障）：

1. ❌ 聚合根必须实现 AggregateRoot<T> - 已有技术约束（BaseRepository）
2. ❌ 实体必须实现 DomainEntity<T, ID> - 无需规则检查
3. ❌ package-info.java 必须存在 - 用户认为不重要
4. ❌ 适配器必须使用 @Adapter 注解 - 用户认为不重要

### 6.2 文档完善

建议后续工作：
1. 在编码规范中添加 Mapper 最佳实践说明
2. 为业务平台提供 Mapper 迁移指南
3. 创建常见问题解答（FAQ）

---

## 七、参考文档

1. 《限界上下文代码编写规范》v2.1 - 十、架构守护 → 10.2 检查清单
2. 《ArchUnit规则与编码规范映射》v1.0
3. MapStruct 官方文档：https://mapstruct.org/
4. ArchUnit 官方文档：https://www.archunit.org/

---

**文档结束** | **版本**：v1.0 | **日期**：2026-04-05
