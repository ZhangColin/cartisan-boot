# F01-04: cartisan-core — stereotype 架构注解 — 测试规格

## 元数据

| 属性 | 值 |
|------|-----|
| Feature | F01-04: cartisan-core — stereotype 架构注解 |
| 文档版本 | v0.1.0 |
| 日期 | 2026-03-13 |
| 状态 | Phase 5: 归档 |
| 前置文档 | [03_implementation.md](./03_implementation.md) |

---

## 1. 测试策略

### 1.1 测试分层

| 测试层 | 工具 | 验证内容 |
|--------|------|----------|
| 单元测试 | JUnit 5 + AssertJ | 注解元数据契约、枚举完整性 |
| 架构测试 | ArchUnit | 零外部依赖约束 |

### 1.2 测试原则

1. **守护契约**：测试验证元注解配置，防止误改
2. **精确匹配**：枚举值必须精确匹配，防止新增值无规则
3. **零外部依赖**：ArchUnit 验证 stereotype 包只依赖 JDK

---

## 2. 单元测试规格

### 2.1 StereotypeAnnotationsTest

**文件路径**: `cartisan-core/src/test/java/com/cartisan/core/stereotype/StereotypeAnnotationsTest.java`

**测试用例清单**:

| # | 测试方法 | 验证内容 | 断言 |
|---|----------|----------|------|
| 1 | `stereotypeAnnotations_shouldBeRetainedAtRuntime` | 所有注解有 @Retention(RUNTIME) | retention.value() == RUNTIME |
| 2 | `boundedContext_shouldTargetPackageOnly` | @BoundedContext 的 @Target | @Target == {PACKAGE} |
| 3 | `aggregate_shouldTargetType` | @Aggregate 的 @Target | @Target == {TYPE} |
| 4 | `domainService_shouldTargetType` | @DomainService 的 @Target | @Target == {TYPE} |
| 5 | `port_shouldTargetType` | @Port 的 @Target | @Target == {TYPE} |
| 6 | `adapter_shouldTargetType` | @Adapter 的 @Target | @Target == {TYPE} |
| 7 | `subDomain_shouldHaveExactlyThreeValues` | SubDomain 枚举值 | {CORE, SUPPORTING, GENERIC} |
| 8 | `portType_shouldHaveExactlyThreeValues` | PortType 枚举值 | {REPOSITORY, CLIENT, PUBLISHER} |

**测试结果**:
```
tests="12" failures="0" errors="0"
```

---

## 3. 架构测试规格

### 3.1 ArchitectureTest - stereotype 包规则

**文件路径**: `cartisan-core/src/test/java/com/cartisan/core/arch/ArchitectureTest.java`

**新增规则**:

| 规则 ID | 规则名称 | 验证内容 | 测试方法 |
|---------|----------|----------|----------|
| S-001 | stereotype 包零第三方依赖 | 不依赖 Spring/Apache/Google 等 | `stereotypePackage_shouldNotDependOnAnyThirdPartyLibrary()` |
| S-002 | stereotype 包仅依赖 JDK | 只依赖 java.*/javax.*/自身 | `stereotypePackage_shouldOnlyDependOnJdkAndItself()` |
| S-003 | 注解 RUNTIME 保留 | 由单元测试验证 | `stereotypeAnnotations_shouldHaveRuntimeRetention()` |
| S-004 | 注解 TARGET 正确 | 由单元测试验证 | `stereotypeAnnotations_shouldHaveCorrectTarget()` |

**测试结果**:
```
tests="13" failures="0" errors="0"
```

---

## 4. 测试覆盖

### 4.1 行覆盖率目标

| 目标 | 实际 | 状态 |
|------|------|------|
| > 90% | 100% | ✅ |

### 4.2 分支覆盖率目标

| 目标 | 实际 | 状态 |
|------|------|------|
| > 80% | 100% | ✅ |

**说明**: 注解和枚举无复杂分支逻辑，覆盖率 100%。

---

## 5. 边界场景覆盖

| 场景 | 覆盖方式 |
|------|----------|
| 枚举值新增导致规则缺失 | 精确匹配测试防止 |
| @Retention 被误改为 SOURCE | 元注解契约测试防止 |
| @Target 配置错误 | 元注解契约测试防止 |
| 意外引入外部依赖 | ArchUnit 规则防止 |

---

## 6. 测试执行结果

### 6.1 完整构建验证

```bash
./gradlew :cartisan-core:build
```

**结果**: ✅ BUILD SUCCESSFUL

### 6.2 所有测试汇总

| 测试套件 | 测试数 | 失败 | 错误 |
|----------|--------|------|------|
| StereotypeAnnotationsTest | 12 | 0 | 0 |
| ArchitectureTest | 13 | 0 | 0 |
| Domain Tests | 34 | 0 | 0 |
| Exception Tests | 39 | 0 | 0 |
| **总计** | **98** | **0** | **0** |

---

## 7. 遗留问题

| ID | 问题描述 | 影响 | 计划 |
|----|----------|------|------|
| - | 无 | - | - |

---

## 8. 相关文档

- [01_requirement.md](./01_requirement.md) — 需求文档
- [02_interface.md](./02_interface.md) — 接口设计
- [03_implementation.md](./03_implementation.md) — 实施计划
- [00_epic_backlog.md](../00_epic_backlog.md) — Epic Backlog

---

## 9. 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v0.1.0 | 2026-03-13 | 初始版本，Phase 5 归档 | Claude |
