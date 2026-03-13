# Feature: F01-05 — util 工具类 — 实施计划

> 版本：v0.1 | 日期：2026-03-13
> 依赖：F01-03 (exception)

---

## 目标复述

为 cartisan-core 模块添加 `Assertions` 工具类，提供 Design by Contract 风格的断言方法（require、ensure、requirePresent），简化领域模型和应用服务中的防御式编程。

核心约束：
- 零外部依赖（仅 JDK + 本模块 exception 包）
- final 类 + 私有构造函数（不可实例化）
- 所有方法为 static

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/util/Assertions.java` | 断言工具类 |
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/util/package-info.java` | 包声明 |
| 新增 | `cartisan-core/src/test/java/com/cartisan/core/util/AssertionsRequireTest.java` | require 方法测试 |
| 新增 | `cartisan-core/src/test/java/com/cartisan/core/util/AssertionsEnsureTest.java` | ensure 方法测试 |
| 新增 | `cartisan-core/src/test/java/com/cartisan/core/util/AssertionsRequirePresentTest.java` | requirePresent 方法测试 |

---

## 核心流程（伪代码）

### require 方法

```
IF condition IS false THEN
    THROW NEW DomainException(codeMessage)
END IF
// 条件为 true 时静默通过
```

### ensure 方法

```
IF condition IS false THEN
    THROW NEW IllegalStateException("Postcondition violated: " + message)
END IF
```

### requirePresent(可选) 快捷版

```
IF optional.isEmpty() THEN
    THROW NEW DomainException(BaseCodeMessage.NOT_FOUND)
END IF
RETURN optional.get()
```

### requirePresent(可选, CodeMessage) 完整版

```
IF optional.isEmpty() THEN
    THROW NEW DomainException(codeMessage)
END IF
RETURN optional.get()
```

---

## 原子任务清单

### Step 1: 创建 util 包结构和 Assertions 类骨架

- 文件：`cartisan-core/src/main/java/com/cartisan/core/util/package-info.java`
- 文件：`cartisan-core/src/main/java/com/cartisan/core/util/Assertions.java`
- 内容：
  - package-info.java：包声明
  - Assertions.java：final 类、私有构造函数、类级 JavaDoc
- 验证：编译通过 `./gradlew :cartisan-core:compileJava`

---

### Step 2: 编写 require 方法测试（红灯）

- 文件：`cartisan-core/src/test/java/com/cartisan/core/util/AssertionsRequireTest.java`
- 内容：
  - AC2：条件为 true 时静默通过
  - AC1：条件为 false 时抛出 DomainException，携带正确 codeMessage
  - AC1 额外：异常消息格式正确
- 验证：编译通过 + 测试全红（Assertions.require 方法尚未实现）

---

### Step 3: 编写 require 方法实现（绿灯）

- 文件：`cartisan-core/src/main/java/com/cartisan/core/util/Assertions.java`
- 内容：添加 `require(boolean, CodeMessage)` 方法实现
- 验证：
  - 编译通过
  - `AssertionsRequireTest` 全绿
  - `./gradlew :cartisan-core:test` 通过

---

### Step 4: 编写 ensure 方法测试（红灯）

- 文件：`cartisan-core/src/test/java/com/cartisan/core/util/AssertionsEnsureTest.java`
- 内容：
  - AC4：条件为 true 时静默通过
  - AC3：条件为 false 时抛出 IllegalStateException
  - AC3 额外：异常消息包含 "Postcondition violated: " 前缀
- 验证：编译通过 + 测试全红

---

### Step 5: 编写 ensure 方法实现（绿灯）

- 文件：`cartisan-core/src/main/java/com/cartisan/core/util/Assertions.java`
- 内容：添加 `ensure(boolean, String)` 方法实现
- 验证：
  - 编译通过
  - `AssertionsEnsureTest` 全绿
  - `./gradlew :cartisan-core:test` 通过

---

### Step 6: 编写 requirePresent 快捷版测试（红灯）

- 文件：`cartisan-core/src/test/java/com/cartisan/core/util/AssertionsRequirePresentTest.java`
- 内容：
  - AC6：Optional 有值时返回值
  - AC5：Optional 为空时抛出 DomainException，错误码为 BaseCodeMessage.NOT_FOUND
- 验证：编译通过 + 测试全红

---

### Step 7: 编写 requirePresent 快捷版实现（绿灯）

- 文件：`cartisan-core/src/main/java/com/cartisan/core/util/Assertions.java`
- 内容：添加 `requirePresent(Optional<T>)` 方法实现
- 验证：
  - 编译通过
  - `AssertionsRequirePresentTest` 中快捷版相关测试全绿
  - `./gradlew :cartisan-core:test` 通过

---

### Step 8: 编写 requirePresent 完整版测试（红灯）

- 文件：`cartisan-core/src/test/java/com/cartisan/core/util/AssertionsRequirePresentTest.java`（追加测试方法）
- 内容：
  - AC8：Optional 有值时返回值（忽略 codeMessage）
  - AC7：Optional 为空时抛出 DomainException，携带指定 codeMessage
- 验证：编译通过 + 测试全红

---

### Step 9: 编写 requirePresent 完整版实现（绿灯）

- 文件：`cartisan-core/src/main/java/com/cartisan/core/util/Assertions.java`
- 内容：添加 `requirePresent(Optional<T>, CodeMessage)` 方法实现
- 验证：
  - 编译通过
  - `AssertionsRequirePresentTest` 全绿
  - `./gradlew :cartisan-core:test` 通过

---

### Step 10: 完整性验证

- 内容：
  - 运行所有测试：`./gradlew :cartisan-core:test`
  - 运行 ArchUnit 验证：`./gradlew :cartisan-core:check`
  - 检查覆盖率：`./gradlew :cartisan-core:testCoverage`
- 验证：
  - 所有测试绿灯
  - ArchUnit 通过（零外部依赖）
  - 测试覆盖率 ≥ 80%

---

## 任务规模估算

| Step | 预估代码行数 | 预估时间 |
|------|-------------|---------|
| Step 1 | ~30 行 | 5 分钟 |
| Step 2 | ~40 行 | 10 分钟 |
| Step 3 | ~5 行 | 5 分钟 |
| Step 4 | ~40 行 | 10 分钟 |
| Step 5 | ~5 行 | 5 分钟 |
| Step 6 | ~30 行 | 10 分钟 |
| Step 7 | ~5 行 | 5 分钟 |
| Step 8 | ~40 行 | 10 分钟 |
| Step 9 | ~5 行 | 5 分钟 |
| Step 10 | 验证步骤 | 10 分钟 |
| **总计** | **~200 行** | **~75 分钟** |

---

## 依赖检查清单

开始执行前确认：

- [ ] F01-03 (exception) 已完成，`DomainException` 和 `CodeMessage` 可用
- [ ] `BaseCodeMessage.NOT_FOUND` 已定义
- [ ] Java 21 工具链已配置
- [ ] Gradle 构建环境正常
