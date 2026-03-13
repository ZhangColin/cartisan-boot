# Feature: F01-05 — util 工具类 — 测试规格

> 版本：v0.1 | 日期：2026-03-13

---

## 测试策略

### 测试层次

| 测试类型 | 工具 | 覆盖内容 |
|---------|------|---------|
| 单元测试 | JUnit 5 + AssertJ | 所有公开 API 的正常路径和异常路径 |
| 架构测试 | ArchUnit | 零外部依赖约束（F01-06 中统一验证） |

### 测试覆盖原则

1. **每个公开方法都有测试** — 包括重载版本
2. **正常路径和异常路径都覆盖** — Given-When-Then 结构
3. **断言有意义** — 验证异常类型、消息内容、CodeMessage 对象
4. **构造函数不可实例化** — 通过反射验证私有构造函数抛出异常

---

## 测试用例清单

### AssertionsRequireTest（require 方法）

| 测试方法 | 验收标准 | 描述 |
|---------|---------|------|
| `should_pass_silently_when_condition_is_true` | AC2 | 条件为 true 时静默通过 |
| `should_throw_DomainException_when_condition_is_false` | AC1 | 条件为 false 时抛出 DomainException |
| `should_include_codeMessage_in_exception` | AC1 | 异常携带正确的 CodeMessage |
| `should_throw_exception_when_attempting_instantiation_via_reflection` | AC9 | 私有构造函数防止实例化 |

### AssertionsEnsureTest（ensure 方法）

| 测试方法 | 验收标准 | 描述 |
|---------|---------|------|
| `should_pass_silently_when_condition_is_true` | AC4 | 条件为 true 时静默通过 |
| `should_throw_IllegalStateException_when_condition_is_false` | AC3 | 条件为 false 时抛出 IllegalStateException |
| `should_include_message_prefix_in_exception` | AC3 | 异常消息包含 "Postcondition violated: " 前缀 |

### AssertionsRequirePresentTest（requirePresent 方法）

| 测试方法 | 验收标准 | 描述 |
|---------|---------|------|
| `should_return_value_when_optional_is_present` | AC6, AC8 | Optional 有值时返回值 |
| `should_throw_DomainException_with_NOT_FOUND_when_optional_is_empty` | AC5 | 快捷版：空 Optional 抛出 NOT_FOUND |
| `should_return_value_when_optional_is_present_with_codeMessage` | AC8 | 完整版：Optional 有值时返回值 |
| `should_throw_DomainException_with_custom_codeMessage_when_optional_is_empty` | AC7 | 完整版：空 Optional 抛出指定 CodeMessage |

---

## 测试覆盖率

### 语句覆盖

| 类 | 目标 | 实际 | 状态 |
|-----|------|------|------|
| Assertions | ≥ 80% | ~95% | ✅ |

### 分支覆盖

| 方法 | 分支数 | 覆盖 | 状态 |
|------|--------|------|------|
| require | 2 (true/false) | 2 | ✅ |
| ensure | 2 (true/false) | 2 | ✅ |
| requirePresent(Optional) | 2 (empty/present) | 2 | ✅ |
| requirePresent(Optional, CodeMessage) | 2 (empty/present) | 2 | ✅ |

---

## 边界场景测试

| 场景 | 测试方法 | 结果 |
|------|---------|------|
| 条件为 true | 所有测试类 | ✅ 静默通过 |
| 条件为 false | 所有测试类 | ✅ 抛出正确异常 |
| Optional.empty() | requirePresent 测试 | ✅ 抛出 DomainException |
| Optional.of(value) | requirePresent 测试 | ✅ 返回 value |
| 反射实例化 | 构造函数测试 | ✅ 抛出 UnsupportedOperationException |

---

## 验收标准验证矩阵

| AC | 描述 | 测试方法 | 状态 |
|----|------|---------|------|
| AC1 | require(false) → DomainException | should_throw_DomainException... | ✅ |
| AC2 | require(true) → 静默通过 | should_pass_silently... | ✅ |
| AC3 | ensure(false) → IllegalStateException | should_throw_IllegalStateException... | ✅ |
| AC4 | ensure(true) → 静默通过 | should_pass_silently... | ✅ |
| AC5 | requirePresent(empty) → NOT_FOUND | should_throw_DomainException_with_NOT_FOUND | ✅ |
| AC6 | requirePresent(present) → 返回值 | should_return_value_when_optional_is_present | ✅ |
| AC7 | requirePresent(empty, cm) → 指定 cm | should_throw_DomainException_with_custom... | ✅ |
| AC8 | requirePresent(present, cm) → 返回值 | should_return_value_when_optional_is_present... | ✅ |
| AC9 | 类设计约束（final, 私有构造函数） | should_throw_exception_when_attempting... | ✅ |
| AC10 | 零外部依赖 | ArchUnit (F01-06) | ⏭️ |

---

## 运行测试

```bash
# 单个测试类
./gradlew :cartisan-core:test --tests AssertionsRequireTest
./gradlew :cartisan-core:test --tests AssertionsEnsureTest
./gradlew :cartisan-core:test --tests AssertionsRequirePresentTest

# 所有测试
./gradlew :cartisan-core:test

# 包含 ArchUnit 验证
./gradlew :cartisan-core:check
```

---

## 测试数据示例

### CodeMessage 示例

```java
// 使用 BaseCodeMessage
BaseCodeMessage.CONFLICT           // HTTP 409
BaseCodeMessage.NOT_FOUND          // HTTP 404
BaseCodeMessage.INVALID_PARAMETER  // HTTP 400
BaseCodeMessage.UNPROCESSABLE_ENTITY  // HTTP 422
```

### Optional 测试数据

```java
Optional<String> empty = Optional.empty();
Optional<String> present = Optional.of("test-value");
```

---

## 已知限制

1. **ArchUnit 测试** — 零外部依赖的架构验证将在 F01-06（模块完整性验证）中统一添加，不在本 Feature 中重复
2. **变异测试（PIT）** — 暂不要求，可在 Phase 5 中作为增强项添加
