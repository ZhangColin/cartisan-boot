# Feature: F01-03 cartisan-core — exception 异常体系 — 实施计划

> 版本：v0.1 | 日期：2026-03-13

---

## 目标复述

实现 cartisan-core 的异常体系，提供统一的错误码接口（CodeMessage）、基础错误码枚举（BaseCodeMessage）、异常基类（CartisanException）和 DDD 分层异常（DomainException、ApplicationException）。

**核心约束：**
- 零外部依赖（仅 JDK 标准库）
- 遵循 F01-02 代码风格
- 完整单元测试覆盖

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/CodeMessage.java` | 错误码接口 |
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/BaseCodeMessage.java` | 基础错误码枚举 |
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/CartisanException.java` | 异常基类 |
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/DomainException.java` | 领域层异常 |
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/ApplicationException.java` | 应用层异常 |
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/package-info.java` | 包文档 |
| 新增 | `cartisan-core/src/test/java/com/cartisan/core/exception/CodeMessageTest.java` | 单元测试 |
| 新增 | `cartisan-core/src/test/java/com/cartisan/core/exception/BaseCodeMessageTest.java` | 单元测试 |
| 新增 | `cartisan-core/src/test/java/com/cartisan/core/exception/CartisanExceptionTest.java` | 单元测试 |
| 新增 | `cartisan-core/src/test/java/com/cartisan/core/exception/DomainExceptionTest.java` | 单元测试 |
| 新增 | `cartisan-core/src/test/java/com/cartisan/core/exception/ApplicationExceptionTest.java` | 单元测试 |
| 修改 | `cartisan-core/src/test/java/com/cartisan/core/arch/ArchitectureTest.java` | 新增 exception 包的 ArchUnit 规则 |

---

## 核心流程（伪代码）

```
异常创建流程：
1. 用户选择异常类型（DomainException / ApplicationException）
2. 传入 CodeMessage + 参数（可选 cause）
3. CartisanException 构造器：
   a. 保存 codeMessage
   b. MessageFormat.format(message, args) → formattedMessage
   c. super(formattedMessage[, cause])
4. 异常就绪，可抛出

异常处理流程（全局异常处理器视角）：
1. 捕获 CartisanException
2. 提取 codeMessage.code() → 响应体 code 字段
3. 提取 codeMessage.httpStatus() → HTTP 响应状态码
4. 提取 exception.getMessage() → 响应体 message 字段
5. 构造统一响应体返回
```

---

## 原子任务清单

### Step 1: 生成 CodeMessage 接口代码

**文件：** `cartisan-core/src/main/java/com/cartisan/core/exception/CodeMessage.java`

**内容：** 将 02_interface.md 中的接口描述转为 Java 接口

**伪代码：**
```
interface CodeMessage {
    String code()           // 错误码标识
    String message()        // 消息模板（可能包含 {0}, {1} 占位符）
    int httpStatus()        // HTTP 状态码
}
```

**验证：** `./gradlew :cartisan-core:compileJava` 通过

---

### Step 2: 编写 CodeMessage 测试（红灯）

**文件：** `cartisan-core/src/test/java/com/cartisan/core/exception/CodeMessageTest.java`

**内容：** 基于 AC1 编写测试

**测试用例：**
- 应定义 code() 方法
- 应定义 message() 方法
- 应定义 httpStatus() 方法

**验证：**
- 编译通过
- 测试编译通过，但接口不存在时红灯（预期在 Step 1 后变绿）

---

### Step 3: 实现 CodeMessage（绿灯）

**文件：** `cartisan-core/src/main/java/com/cartisan/core/exception/CodeMessage.java`

**内容：** 接口定义

**JavaDoc 要点：**
- 说明三个方法的用途
- 说明 message() 支持 MessageFormat 占位符
- 说明 httpStatus() 使错误语义自包含

**验证：**
- `./gradlew :cartisan-core:test --tests CodeMessageTest` 绿灯
- ArchUnit 通过

---

### Step 4: 生成 BaseCodeMessage 枚举代码

**文件：** `cartisan-core/src/main/java/com/cartisan/core/exception/BaseCodeMessage.java`

**内容：** 实现 CodeMessage 接口的枚举

**枚举值：**

HTTP 规范错误码（11 个）：
- BAD_REQUEST(400)
- UNAUTHORIZED(401)
- FORBIDDEN(403)
- NOT_FOUND(404)
- METHOD_NOT_ALLOWED(405)
- CONFLICT(409)
- UNSUPPORTED_MEDIA_TYPE(415)
- UNPROCESSABLE_ENTITY(422)
- TOO_MANY_REQUESTS(429)
- INTERNAL_SERVER_ERROR(500)
- SERVICE_UNAVAILABLE(503)

通用业务错误码（4 个）：
- UNKNOWN_ERROR(500)
- INVALID_PARAMETER(400) - 带占位符 {0}
- RESOURCE_NOT_FOUND(404) - 带占位符 {0}
- DUPLICATE(409) - 带占位符 {0}

**验证：** `./gradlew :cartisan-core:compileJava` 通过

---

### Step 5: 编写 BaseCodeMessage 测试（红灯）

**文件：** `cartisan-core/src/test/java/com/cartisan/core/exception/BaseCodeMessageTest.java`

**内容：** 基于 AC2 编写测试

**测试用例：**
- 所有枚举值应实现 CodeMessage 接口
- 所有枚举值的 httpStatus 应在 400-599 之间
- HTTP 规范错误码的 code() 和 httpStatus() 应正确
- 通用业务错误码的 code()、httpStatus()、message() 应正确
- 通用业务错误码的 message() 应包含占位符 {0}

**验证：** 编译通过 + 测试红灯

---

### Step 6: 实现 BaseCodeMessage（绿灯）

**文件：** `cartisan-core/src/main/java/com/cartisan/core/exception/BaseCodeMessage.java`

**内容：** 枚举实现，包含：
- 私有字段：httpStatus, code, message
- 构造器：接收三个参数
- 接口方法实现

**JavaDoc 要点：**
- 列出所有 HTTP 规范错误码
- 列出所有通用业务错误码
- 说明通用业务错误码带占位符

**验证：**
- `./gradlew :cartisan-core:test --tests BaseCodeMessageTest` 绿灯
- ArchUnit 通过

---

### Step 7: 编写 CartisanException 测试（红灯）

**文件：** `cartisan-core/src/test/java/com/cartisan/core/exception/CartisanExceptionTest.java`

**内容：** 基于 AC3、AC5 编写测试

**测试用例：**
- 应存储 codeMessage
- 应格式化无占位符的消息
- 应格式化单占位符的消息
- 应格式化多占位符的消息
- 应处理空参数数组
- 应保留 cause
- 应保留带参数的 cause
- 应是 RuntimeException
- 应是抽象类（编译时验证）

**注意：** 需要创建测试用子类来实例化 CartisanException

**验证：** 编译通过 + 测试红灯

---

### Step 8: 实现 CartisanException（绿灯）

**文件：** `cartisan-core/src/main/java/com/cartisan/core/exception/CartisanException.java`

**内容：** 抽象基类，包含：
- 私有字段：codeMessage, formattedMessage
- 两个 protected 构造器（有/无 cause）
- 私有静态方法 formatMessage() 使用 MessageFormat.format()
- 公共方法 getCodeMessage()
- 覆盖方法 getMessage()

**核心逻辑：**
```
构造流程：
1. Objects.requireNonNull(codeMessage, "codeMessage cannot be null")
2. formattedMessage = MessageFormat.format(codeMessage.message(), args)
3. 若有 cause：super(formattedMessage, cause)
4. 若无 cause：super(formattedMessage)
```

**JavaDoc 要点：**
- 说明核心特性（携带 CodeMessage、支持参数化、支持异常链）
- 提供使用示例（无参数、带参数、带 cause）
- 说明 getMessage() 返回格式化后的消息

**验证：**
- `./gradlew :cartisan-core:test --tests CartisanExceptionTest` 绿灯
- ArchUnit 通过

---

### Step 9: 编写 DomainException 测试（红灯）

**文件：** `cartisan-core/src/test/java/com/cartisan/core/exception/DomainExceptionTest.java`

**内容：** 基于 AC4 编写测试

**测试用例：**
- 应继承 CartisanException
- 应支持无 cause 构造器
- 应支持有 cause 构造器
- 应正确格式化消息

**验证：** 编译通过 + 测试红灯

---

### Step 10: 实现 DomainException（绿灯）

**文件：** `cartisan-core/src/main/java/com/cartisan/core/exception/DomainException.java`

**内容：** 继承 CartisanException 的类，包含：
- 两个公共构造器（有/无 cause）
- 调用 super(codeMessage, args) 或 super(codeMessage, cause, args)

**JavaDoc 要点：**
- 说明使用场景（聚合根、实体、领域服务）
- 说明与基础设施异常的关系（在端口适配器中转换）
- 提供使用示例

**验证：**
- `./gradlew :cartisan-core:test --tests DomainExceptionTest` 绿灯
- ArchUnit 通过

---

### Step 11: 编写 ApplicationException 测试（红灯）

**文件：** `cartisan-core/src/test/java/com/cartisan/core/exception/ApplicationExceptionTest.java`

**内容：** 基于 AC4 编写测试

**测试用例：**
- 应继承 CartisanException
- 应支持无 cause 构造器
- 应支持有 cause 构造器
- 应正确格式化消息

**验证：** 编译通过 + 测试红灯

---

### Step 12: 实现 ApplicationException（绿灯）

**文件：** `cartisan-core/src/main/java/com/cartisan/core/exception/ApplicationException.java`

**内容：** 继承 CartisanException 的类，包含：
- 两个公共构造器（有/无 cause）
- 调用 super(codeMessage, args) 或 super(codeMessage, cause, args)

**JavaDoc 要点：**
- 说明使用场景（应用服务、权限检查、用例前置条件）
- 说明与 DomainException 的区别（用表格对比）
- 提供使用示例

**验证：**
- `./gradlew :cartisan-core:test --tests ApplicationExceptionTest` 绿灯
- ArchUnit 通过

---

### Step 13: 创建 package-info.java

**文件：** `cartisan-core/src/main/java/com/cartisan/core/exception/package-info.java`

**内容：** 包级 JavaDoc 文档

**章节：**
- 概览
- 核心组件（表格形式）
- 使用指南（选择异常类型、选择/定义错误码、参数化消息、保留异常链）
- 架构原则

**验证：** `./gradlew :cartisan-core:compileJava` 通过

---

### Step 14: 更新 ArchUnit 规则

**文件：** `cartisan-core/src/test/java/com/cartisan/core/arch/ArchitectureTest.java`

**新增规则：**
```
规则 E-001：exception 包不依赖任何第三方库
规则 E-002：CartisanException 是抽象类
规则 E-003：Domain 和 Application 异常继承 CartisanException
```

**验证：**
- `./gradlew :cartisan-core:test --tests ArchitectureTest` 绿灯
- 所有架构规则通过

---

### Step 15: 全量验证

**命令：**
```bash
./gradlew :cartisan-core:test      # 所有测试绿灯
./gradlew :cartisan-core:check     # 包含 ArchUnit
./gradlew :cartisan-core:build     # 完整构建
```

**验证：**
- 所有单元测试绿灯
- ArchUnit 规则通过
- PIT 变异测试（如果配置）杀死率 ≥ 70%
- 模块可独立打包

---

## 验证检查点

每个 Step 完成后检查：
- [ ] 编译通过
- [ ] 测试绿灯（或红灯，按阶段）
- [ ] 代码风格与 F01-02 一致
- [ ] JavaDoc 完整

Feature 完成后检查：
- [ ] 所有 AC 有对应测试
- [ ] 测试覆盖率 ≥ 80%
- [ ] ArchUnit 规则通过
- [ ] 零外部依赖（dependencies task 验证）
- [ ] 01/02/03 文档与代码一致

---

## 相关文档
- [01_requirement.md](./01_requirement.md) — 需求规格
- [02_interface.md](./02_interface.md) — 接口设计
- [00_epic_backlog.md](../00_epic_backlog.md) — Epic Backlog
