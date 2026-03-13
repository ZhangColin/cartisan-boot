# Feature: F01-06 cartisan-core 模块完整性验证 — 测试规格

> 版本：v0.1 | 日期：2026-03-13

---

## 测试策略

### 自动化测试层次

| 测试类型 | 工具 | 验证内容 |
|----------|------|----------|
| 单元测试 | JUnit 5 + AssertJ | 各组件的业务逻辑 |
| 架构测试 | ArchUnit | 包结构白名单、零外部依赖 |
| 文档测试 | Gradle javadoc + -Xdoclint | JavaDoc 格式正确性 |

---

## 测试用例清单

### CartisanCoreModuleTest

#### TC-P001: 包结构白名单验证
- **文件**: `CartisanCoreModuleTest.java`
- **方法**: `packageStructure_shouldOnlyUseAllowedTopLevelPackages()`
- **目的**: 确保所有生产类只存在于四个允许的一级包中
- **前置条件**: 生产代码已编译 (`build/classes/java/main` 存在)
- **验证点**:
  - 只允许 `com.cartisan.core.domain..`
  - 只允许 `com.cartisan.core.exception..`
  - 只允许 `com.cartisan.core.stereotype..`
  - 只允许 `com.cartisan.core.util..`
  - 允许上述包的所有子包
- **预期结果**: 规则检查通过，无违规

#### TC-P002: 根包整洁性验证
- **文件**: `CartisanCoreModuleTest.java`
- **方法**: `packageStructure_shouldNotAllowUtilityClassesInRoot()`
- **目的**: 确保根包 `com.cartisan.core` 下没有生产类
- **前置条件**: 生产代码已编译
- **验证点**:
  - 根包下类的数量为 0
  - `package-info.java` 被 ArchUnit 自动过滤
- **预期结果**: 断言通过，根包下无生产类

---

## JavaDoc 验证

### 验证方式
- **工具**: Gradle javadoc 任务
- **配置**: `-Xdoclint:all,-missing -quiet`
- **触发时机**: 每次 `build` 任务执行时

### 验证内容
- HTML 标题层级正确（不直接使用 `<h3>`）
- 表格必须包含 `<caption>`
- HTML5 不支持的属性不被使用（如 `summary`）
- JavaDoc 标签格式正确

### 格式修复记录
以下文件在 F01-06 中修复了 JavaDoc 格式问题：
- `Assertions.java`: 移除方法级注释中的 `<h3>` 标题
- `ValueObject.java`: `<h3>` → `<h2>`，移除 `summary` 属性
- `Entity.java`: `<h3>` → `<h2>`，移除 `summary` 属性
- `AggregateRoot.java`: `<h3>` → `<h2>`
- `DomainEvent.java`: `<h3>` → `<h2>`
- `Identity.java`: `<h3>` → `<h2>`
- `CartisanException.java`: `<h3>` → `<h2>`
- `DomainException.java`: `<h3>` → `<h2>`
- `ApplicationException.java`: `<h3>` → `<h2>`，表格添加 `<caption>`
- `CodeMessage.java`: `<h3>` → `<h2>`
- `domain/package-info.java`: 移除无效的 `@package` 标签
- `exception/package-info.java`: 表格添加 `<caption>`

---

## 测试覆盖率

### 覆盖范围
- **生产代码包**: 100% (domain, exception, stereotype, util)
- **测试代码**: 不在验证范围内

### 未覆盖内容
- 测试代码本身（如 `CartisanCoreModuleTest`, `ArchitectureTest`）
- 测试代码位于 `com.cartisan.core.*` 但在 `src/test/` 目录下

---

## 执行记录

### 首次执行 (2026-03-13)
- **结果**: 全部通过
- **修复**: 8 个 Java 文件的 JavaDoc 格式问题
- **新增**: `CartisanCoreModuleTest.java`
- **配置**: build.gradle.kts 添加 javadoc 校验

### 验证命令
```bash
# 完整验证
./gradlew :cartisan-core:build

# 仅运行包结构测试
./gradlew :cartisan-core:test --tests CartisanCoreModuleTest

# 仅验证 JavaDoc
./gradlew :cartisan-core:javadoc
```

---

## 与 F01-07 的关系

F01-06 验证 cartisan-core 自身的完整性。
F01-07 将创建可复用的 ArchUnit 规则集，供业务项目继承使用。
两者职责分离：
- F01-06: cartisan-core 自证清白
- F01-07: 对外输出规则，守护业务项目
