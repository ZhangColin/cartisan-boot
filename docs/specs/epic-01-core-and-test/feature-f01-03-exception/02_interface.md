# Feature: F01-03 cartisan-core — exception 异常体系 — 接口契约

> 版本：v0.1 | 日期：2026-03-13

---

## 一、包结构

```
com.cartisan.core.exception
├── CodeMessage              // 接口
├── BaseCodeMessage          // 枚举
├── CartisanException        // 类
├── DomainException          // 类
└── ApplicationException     // 类
```

---

## 二、接口定义

### 2.1 CodeMessage 接口

**职责：** 定义错误码的结构化契约

**方法签名：**

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `code()` | String | 错误码标识，如 `"USER_NOT_FOUND"` |
| `message()` | String | 消息模板，支持 MessageFormat 占位符，如 `"User {0} not found"` |
| `httpStatus()` | int | HTTP 状态码，如 `404` |

**接口描述（伪代码）：**
```
interface CodeMessage {
    // 获取错误码标识（字符串）
    String code()

    // 获取消息模板（可能包含 {0}, {1} 等占位符）
    String message()

    // 获取对应的 HTTP 状态码
    int httpStatus()
}
```

---

### 2.2 BaseCodeMessage 枚举

**职责：** 提供 HTTP 规范错误码和最小通用业务错误码

**枚举值定义：**

#### HTTP 规范错误码

| 枚举值 | code() | httpStatus() | message() |
|--------|--------|--------------|-----------|
| `BAD_REQUEST` | "BAD_REQUEST" | 400 | "Invalid request" |
| `UNAUTHORIZED` | "UNAUTHORIZED" | 401 | "Authentication required" |
| `FORBIDDEN` | "FORBIDDEN" | 403 | "Access denied" |
| `NOT_FOUND` | "NOT_FOUND" | 404 | "Resource not found" |
| `METHOD_NOT_ALLOWED` | "METHOD_NOT_ALLOWED" | 405 | "Method not allowed" |
| `CONFLICT` | "CONFLICT" | 409 | "Resource conflict" |
| `UNSUPPORTED_MEDIA_TYPE` | "UNSUPPORTED_MEDIA_TYPE" | 415 | "Unsupported media type" |
| `UNPROCESSABLE_ENTITY` | "UNPROCESSABLE_ENTITY" | 422 | "Unprocessable entity" |
| `TOO_MANY_REQUESTS` | "TOO_MANY_REQUESTS" | 429 | "Too many requests" |
| `INTERNAL_SERVER_ERROR` | "INTERNAL_SERVER_ERROR" | 500 | "Internal server error" |
| `SERVICE_UNAVAILABLE` | "SERVICE_UNAVAILABLE" | 503 | "Service unavailable" |

#### 通用业务错误码（最小集）

| 枚举值 | code() | httpStatus() | message() | 说明 |
|--------|--------|--------------|-----------|------|
| `UNKNOWN_ERROR` | "UNKNOWN_ERROR" | 500 | "Unknown error occurred" | 兜底错误 |
| `INVALID_PARAMETER` | "INVALID_PARAMETER" | 400 | "Invalid parameter: {0}" | 参数校验失败 |
| `RESOURCE_NOT_FOUND` | "RESOURCE_NOT_FOUND" | 404 | "Resource not found: {0}" | 资源不存在 |
| `DUPLICATE` | "DUPLICATE" | 409 | "Duplicate resource: {0}" | 重复冲突 |

**枚举结构（伪代码）：**
```
enum BaseCodeMessage implements CodeMessage {
    // HTTP 规范错误码
    BAD_REQUEST(400, "BAD_REQUEST", "Invalid request"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "Authentication required"),
    // ... 其他 HTTP 状态码

    // 通用业务错误码
    UNKNOWN_ERROR(500, "UNKNOWN_ERROR", "Unknown error occurred"),
    INVALID_PARAMETER(400, "INVALID_PARAMETER", "Invalid parameter: {0}"),
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "Resource not found: {0}"),
    DUPLICATE(409, "DUPLICATE", "Duplicate resource: {0}");

    // 字段
    private final int httpStatus
    private final String code
    private final String message

    // 构造器
    BaseCodeMessage(int httpStatus, String code, String message)

    // 实现 CodeMessage 接口
    String code() { return this.code }
    String message() { return this.message }
    int httpStatus() { return this.httpStatus }
}
```

---

### 2.3 CartisanException 类

**职责：** 异常基类，携带 CodeMessage 并支持参数化和异常链

**字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `codeMessage` | CodeMessage | 结构化错误信息 |
| `formattedMessage` | String | 格式化后的最终消息 |

**构造器：**

| 签名 | 说明 |
|------|------|
| `CartisanException(CodeMessage codeMessage, Object... args)` | 无异常链，使用 MessageFormat 格式化消息 |
| `CartisanException(CodeMessage codeMessage, Throwable cause, Object... args)` | 带异常链 |

**方法：**

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `getCodeMessage()` | CodeMessage | 获取结构化错误信息 |
| `getMessage()` | String | 返回格式化后的消息（覆盖 Throwable） |

**核心流程（伪代码）：**
```
构造过程：
1. 保存 codeMessage 字段
2. 用 MessageFormat.format(codeMessage.message(), args) 生成 formattedMessage
3. 若有 cause，调用 super(formattedMessage, cause)
4. 若无 cause，调用 super(formattedMessage)

使用 MessageFormat 规则：
- args 为 null 或空数组时，MessageFormat 返回原始模板
- 占位符 {0}, {1}, {2} 按顺序对应 args
```

---

### 2.4 DomainException 类

**职责：** 领域层异常，表示业务规则违反

**继承：** `extends CartisanException`

**构造器：**

| 签名 | 说明 |
|------|------|
| `DomainException(CodeMessage codeMessage, Object... args)` | 无异常链 |
| `DomainException(CodeMessage codeMessage, Throwable cause, Object... args)` | 带异常链（如基础设施异常转换） |

**使用场景：**
- 聚合根中业务规则校验失败（如余额不足、订单已关闭）
- 实体中不变量被违反
- 领域服务中业务逻辑失败

---

### 2.5 ApplicationException 类

**职责：** 应用层异常，表示用例/流程层面的问题

**继承：** `extends CartisanException`

**构造器：**

| 签名 | 说明 |
|------|------|
| `ApplicationException(CodeMessage codeMessage, Object... args)` | 无异常链 |
| `ApplicationException(CodeMessage codeMessage, Throwable cause, Object... args)` | 带异常链 |

**使用场景：**
- 应用服务中参数校验失败
- 权限检查失败
- 用例前置条件不满足
- 基础设施异常在端口适配器中转换后抛出

---

## 三、核心流程

### 3.1 异常创建流程

```
用户代码调用
    ↓
选择异常类型（DomainException / ApplicationException）
    ↓
传入 CodeMessage + 参数（可选 cause）
    ↓
CartisanException 构造器：
    1. 保存 codeMessage
    2. MessageFormat.format(message, args) → formattedMessage
    3. super(formattedMessage[, cause])
    ↓
异常就绪，可抛出
```

### 3.2 异常处理流程（全局异常处理器视角）

```
Controller 抛出异常
    ↓
全局异常处理器捕获 CartisanException
    ↓
提取 codeMessage：
    - codeMessage.code() → 响应体 code 字段
    - codeMessage.httpStatus() → HTTP 响应状态码
    - exception.getMessage() → 响应体 message 字段（已格式化）
    ↓
构造统一响应体：
{
    "code": "RESOURCE_NOT_FOUND",
    "message": "Resource not found: 123",
    "status": 404  // 或通过 httpStatus 设置
}
    ↓
返回给前端
```

---

## 四、MessageFormat 占位符规范

### 占位符语法

| 占位符 | 含义 | 示例模板 | 示例输入 | 输出 |
|--------|------|---------|---------|------|
| `{0}` | 第一个参数 | "User {0} not found" | ["john@example.com"] | "User john@example.com not found" |
| `{1}` | 第二个参数 | "{0} {1}" | ["Hello", "World"] | "Hello World" |
| `{0,number}` | 数字格式 | "Amount: {0,number}" | [1234.567] | "Amount: 1,234.567" |
| `{0,date}` | 日期格式 | "Created: {0,date}" | [Instant] | "Created: 2026-03-13" |

### 边界场景

| 场景 | 行为 |
|------|------|
| 无参数 | 返回原始模板字符串 |
| 参数数量少于占位符 | MessageFormat 抛出 IllegalArgumentException |
| 参数数量多于占位符 | 多余参数被忽略 |

---

## 五、ArchUnit 架构规则

**新增规则（在 F01-07 实现，这里先定义）：**

```
规则 E-001：exception 包不依赖任何第三方库
验证：classes().that().resideInAPackage("..exception..")
        .should().onlyDependOnClassesThat()
        .areAssignableTo("java..")

规则 E-002：CartisanException 是抽象类
验证：classes().that().haveSimpleName("CartisanException")
        .should().beAbstract()
        .and().should().beAssignableTo(RuntimeException.class)
```

---

## 六、变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/CodeMessage.java` | 接口 |
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/BaseCodeMessage.java` | 枚举 |
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/CartisanException.java` | 抽象类 |
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/DomainException.java` | 类 |
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/ApplicationException.java` | 类 |
| 新增 | `cartisan-core/src/main/java/com/cartisan/core/exception/package-info.java` | 包文档 |
| 新增 | `cartisan-core/src/test/java/com/cartisan/core/exception/*Test.java` | 单元测试 |

---

## 七、技术决策记录

（待 Phase 5 归档时补充到 DECISIONS.md）

| 决策点 | 选择 | 理由 |
|--------|------|------|
| BaseCodeMessage 内容 | HTTP 规范 + 4 个通用业务错误码 | 最小集，避免框架替业务做过多假设 |
| 异常层次 | 两层（Domain + Application） | 符合 DDD 六边形架构，基础设施异常在适配器中转换 |
| 消息格式化 | MessageFormat 占位符 + varargs | JDK 标准，支持复杂格式，便于国际化 |
| HTTP 状态码 | CodeMessage 包含 httpStatus() | 错误语义自包含，全局异常处理器逻辑简单 |
| code() 返回类型 | String | 自描述、可读、易扩展 |
| 异常链 | 格式化消息 + 标准 Throwable cause | 栈可读性好，符合 Java 标准 |
