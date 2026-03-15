# Feature: F04-03 JooqTenantSupport — 测试规格与归档

> **Epic:** Data-Query (Epic 4)
> **完成日期:** 2026-03-15
> **状态:** ✅ Phase 5 审查完成

---

## 测试策略

### 测试范围

| 测试类型 | 覆盖场景 | 状态 |
|---------|---------|------|
| **单元测试** | 无租户上下文 → 返回 noCondition | ✅ |
| **单元测试** | null 参数 → 抛出 NullPointerException | ✅ |
| **集成测试** | 有租户上下文 → 返回等值条件 | ⏳ 留给 F04-05 |

### 测试实现说明

由于 `TenantContext.runWithTenant()` 是 package-private 方法，F04-03 单元测试无法直接模拟租户上下文。因此：
- **F04-03 单元测试**：验证无租户场景 + null 参数校验
- **F04-05 集成测试**：验证有租户场景（需要 DataSource + DSLContext 环境）

---

## 最终用例清单

### TC1: givenNoTenantContext_whenEqTenantId_thenReturnsNoCondition

**目的：** 验证无租户上下文时返回空条件

**输入：** mock TableField（非 null）

**预期输出：** `DSL.noCondition()`

**实现：**
```java
@Test
void givenNoTenantContext_whenEqTenantId_thenReturnsNoCondition() {
    TableField<?, Long> mockField = createMockTenantIdField();
    Condition result = JooqTenantSupport.eqTenantId(mockField);
    assertThat(result).isEqualTo(DSL.noCondition());
}
```

**状态：** ✅ 通过

---

### TC2: givenNullField_whenEqTenantId_thenThrowsNullPointerException

**目的：** 验证 null 参数抛出异常

**输入：** `null`

**预期输出：** `NullPointerException`，消息包含 "tenantIdField"

**实现：**
```java
@Test
void givenNullField_whenEqTenantId_thenThrowsNullPointerException() {
    TableField<?, Long> nullField = null;
    assertThatThrownBy(() -> JooqTenantSupport.eqTenantId(nullField))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("tenantIdField");
}
```

**状态：** ✅ 通过

---

## 代码变更汇总

### 新增文件

| 文件 | 行数 | 说明 |
|------|-----|------|
| `support/package-info.java` | 11 | 包声明文档 |
| `support/JooqTenantSupport.java` | 77 | 工具类实现（含 JavaDoc） |
| `support/JooqTenantSupportTest.java` | 57 | 单元测试 |
| **合计** | **145** | **不含注释空行约 90 行** |

### 修改文件

| 文件 | 变更 |
|------|------|
| `cartisan-data-query/build.gradle.kts` | 添加 `compileOnly` + `testImplementation` 依赖 |
| | 启用 `--enable-preview` 编译选项 |

---

## Gradle 依赖变更

```kotlin
// 添加的可选依赖
compileOnly(project(":cartisan-security"))  // 编译期需要
testImplementation(project(":cartisan-security"))  // 测试运行时需要

// 启用 Java 21 预览功能（ScopedValue 支持）
tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}
tasks.withType<Test> {
    jvmArgs("--enable-preview")
}
```

---

## 验收检查

| 检查项 | 状态 |
|-------|------|
| AC1: 有租户上下文时返回等值条件 | ⏳ F04-05 验证 |
| AC2: 无租户上下文时返回空条件 | ✅ 已验证 |
| AC3: 依赖为可选（compileOnly） | ✅ 已配置 |
| AC4: API 设计符合 jOOQ 惯用法 | ✅ 静态方法 + TableField 参数 |
| 单元测试覆盖 | ✅ 2/2 通过 |
| ArchUnit 通过 | ✅ |
| JavaDoc 完整 | ✅ |

---

## 设计决策记录

### D04-03-01: 简化单元测试范围

**决策：** F04-03 单元测试只覆盖无租户场景，有租户场景留给 F04-05 集成测试。

**理由：**
- `TenantContext.runWithTenant()` 是 package-private，测试类无法直接调用
- 不为测试目的修改 `TenantContext` 的访问级别
- 有租户场景需要真实的 DSLContext 环境，更适合集成测试

**影响：**
- F04-03 单元测试只验证 null 参数和无租户场景
- F04-05 集成测试需补充有租户场景的验证

---

### D04-03-02: 启用 Java 预览功能

**决策：** cartisan-data-query 启用 `--enable-preview`。

**理由：**
- 依赖 cartisan-security（使用 ScopedValue 预览功能）
- compileOnly 依赖仍需加载 TenantContext 类

**影响：**
- 编译和测试都使用预览功能
- 与 cartisan-security 保持一致

---

## 后续工作

- **F04-04:** 代码生成配置指南
- **F04-05:** 集成测试（包含有租户场景验证）

---

## 文档归档

本 Feature 的完整文档位于：
- `01_requirement.md` — 需求规格
- `02_interface.md` — 接口契约
- `03_implementation.md` — 实施计划
- `04_test_spec.md` — 本文档
