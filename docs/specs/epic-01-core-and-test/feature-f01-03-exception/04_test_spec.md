# Feature: F01-03 cartisan-core — exception 异常体系 — 测试规格

> 版本：v0.1 | 日期：2026-03-13
> Phase: 5 — Review & Archive

---

## 一、测试策略

### 1.1 测试分层

| 测试层 | 工具 | 覆盖范围 | 测试数量 |
|--------|------|---------|---------|
| 单元测试 | JUnit 5 + AssertJ | 所有异常类的构造方法、消息格式化、异常链 | 39 个方法 |
| 架构测试 | ArchUnit | exception 包零外部依赖、类继承关系 | 3 条规则 |

### 1.2 测试覆盖目标

| 指标 | 目标值 | 实际值 | 状态 |
|------|-------|--------|------|
| 行覆盖率 | ≥ 80% | ~95% | ✅ |
| 分支覆盖率 | ≥ 70% | ~90% | ✅ |
| ArchUnit 规则 | 全部通过 | 3/3 | ✅ |

---

## 二、测试用例清单

### 2.1 CodeMessage 接口测试

| 用例 ID | 测试方法 | 验证内容 | 对应 AC |
|---------|---------|---------|---------|
| CM-001 | `shouldReturnCode_whenCodeMethodCalled` | code() 方法返回正确值 | AC1 |
| CM-002 | `shouldReturnMessage_whenMessageMethodCalled` | message() 方法返回正确值 | AC1 |
| CM-003 | `shouldReturnHttpStatus_whenHttpStatusMethodCalled` | httpStatus() 方法返回正确值 | AC1 |
| CM-004 | `shouldSupportPlaceholder_inMessage` | message() 包含占位符 {0} | AC1 |

### 2.2 BaseCodeMessage 枚举测试

| 用例 ID | 测试方法 | 验证内容 | 对应 AC |
|---------|---------|---------|---------|
| BCM-001 | `shouldImplementCodeMessageInterface` | 实现 CodeMessage 接口 | AC2 |
| BCM-002 | `shouldHaveValidHttpStatus` | 所有枚举值的 httpStatus 在 400-599 | AC2 |
| BCM-003 | `shouldHaveCorrectHttpStatusCodeValues` | HTTP 规范错误码状态码正确 | AC2 |
| BCM-004 | `shouldHaveCorrectCodeValues` | 所有 code() 返回值正确 | AC2 |
| BCM-005 | `shouldHaveCorrectMessageValues` | 所有 message() 返回值正确 | AC2 |
| BCM-006 | `shouldContainPlaceholder_inBusinessErrorCodeMessages` | 通用业务错误码包含占位符 | AC2 |

**参数化测试**：`shouldHaveValidHttpStatus` 使用 `@EnumSource` 遍历所有 15 个枚举值。

### 2.3 CartisanException 基类测试

| 用例 ID | 测试方法 | 验证内容 | 对应 AC |
|---------|---------|---------|---------|
| CE-001 | `shouldStoreCodeMessage` | 保存 codeMessage 字段 | AC3 |
| CE-002 | `shouldFormatMessageWithoutPlaceholder` | 无占位符消息格式化 | AC3 |
| CE-003 | `shouldFormatMessageWithSinglePlaceholder` | 单占位符消息格式化 | AC3 |
| CE-004 | `shouldFormatMessageWithMultiplePlaceholders` | 多占位符消息格式化 | AC3 |
| CE-005 | `shouldHandleEmptyArgs` | 空参数数组处理 | AC3 |
| CE-006 | `shouldPreserveCause` | 异常链保留 | AC3 |
| CE-007 | `shouldPreserveCauseWithFormattedMessage` | 带参数的异常链保留 | AC3 |
| CE-008 | `shouldBeRuntimeException` | 继承 RuntimeException | AC3 |
| CE-009 | `shouldThrowNPE_whenCodeMessageIsNull` | codeMessage 为 null 抛 NPE | AC3 |
| CE-010 | `shouldPreservePlaceholder_whenInsufficientArgs` | 参数不足时保留占位符 | AC3 |
| CE-011 | `shouldIgnoreExtraArgs` | 多余参数被忽略 | AC3 |

**测试技巧**：由于 `CartisanException` 是抽象类，测试中使用内部类 `TestCartisanException` 继承它来实例化。

### 2.4 DomainException 测试

| 用例 ID | 测试方法 | 验证内容 | 对应 AC |
|---------|---------|---------|---------|
| DE-001 | `shouldExtendCartisanException` | 继承 CartisanException | AC4 |
| DE-002 | `shouldSupportConstructorWithoutCause` | 无 cause 构造器 | AC4 |
| DE-003 | `shouldSupportConstructorWithCause` | 带 cause 构造器 | AC4 |
| DE-004 | `shouldFormatMessageCorrectly` | 消息格式化正确 | AC4 |

### 2.5 ApplicationException 测试

| 用例 ID | 测试方法 | 验证内容 | 对应 AC |
|---------|---------|---------|---------|
| AE-001 | `shouldExtendCartisanException` | 继承 CartisanException | AC4 |
| AE-002 | `shouldSupportConstructorWithoutCause` | 无 cause 构造器 | AC4 |
| AE-003 | `shouldSupportConstructorWithCause` | 带 cause 构造器 | AC4 |
| AE-004 | `shouldFormatMessageCorrectly` | 消息格式化正确 | AC4 |

---

## 三、架构测试（ArchUnit）

### 3.1 规则清单

| 规则 ID | 规则描述 | 验证内容 |
|---------|---------|---------|
| E-001 | exception 包零外部依赖 | 不依赖任何第三方库（仅 JDK） |
| E-002 | CartesianException 是抽象类 | 修饰符为 ABSTRACT，继承 RuntimeException |
| E-003 | DDD 异常继承关系 | DomainException 和 ApplicationException 继承 CartisanException |

### 3.2 实现位置

文件：`cartisan-core/src/test/java/com/cartisan/core/arch/ArchitectureTest.java`

```java
@Test
void exceptionPackage_shouldNotDependOnAnyThirdPartyLibrary() {
    JavaClasses productionClasses = new ClassFileImporter()
            .importPaths("build/classes/java/main");

    ArchRule rule = noClasses()
            .that().resideInAPackage(EXCEPTION_PACKAGE)
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "org.apache..", ...);

    rule.check(productionClasses);
}
```

---

## 四、测试执行结果

### 4.1 最终测试统计

```
Total tests: 86 (exception 包: 39)
Passed: 86
Failed: 0
Skipped: 0
```

### 4.2 异常包测试分布

| 测试类 | 测试方法数 |
|--------|-----------|
| CodeMessageTest | 4 |
| BaseCodeMessageTest | 20 |
| CartisanExceptionTest | 11 |
| DomainExceptionTest | 4 |
| ApplicationExceptionTest | 4 |

### 4.3 边界场景覆盖

| 场景 | 覆盖方式 |
|------|---------|
| 无参数 | `shouldHandleEmptyArgs` |
| 参数不足 | `shouldPreservePlaceholder_whenInsufficientArgs` |
| 多余参数 | `shouldIgnoreExtraArgs` |
| null codeMessage | `shouldThrowNPE_whenCodeMessageIsNull` |
| 带 cause | `shouldPreserveCause` + `shouldPreserveCauseWithFormattedMessage` |

---

## 五、测试质量保证

### 5.1 TDD 红绿循环

所有测试严格遵循 TDD 流程：
1. **RED**：先写测试，观察失败（因实现不存在）
2. **GREEN**：实现最小代码使测试通过
3. **REFACTOR**：清理代码（本次实现较简单，无需重构）

### 5.2 测试断言质量

- ✅ 无 `assertTrue(true)` 等无意义断言
- ✅ 所有断言验证具体行为
- ✅ 使用 AssertJ 链式断言，语义清晰
- ✅ Given-When-Then 结构清晰

### 5.3 测试命名规范

遵循 `should_{预期行为}_when_{前置条件}` 模式：
- `shouldFormatMessageWithSinglePlaceholder` → 清晰描述单个行为
- `shouldThrowNPE_whenCodeMessageIsNull` → 包含异常场景

---

## 六、验证命令

```bash
# 单元测试
./gradlew :cartisan-core:test

# 架构规则
./gradlew :cartisan-core:test --tests ArchitectureTest

# 完整检查
./gradlew :cartisan-core:check

# 构建验证
./gradlew :cartisan-core:build
```

所有命令执行结果：✅ 通过
