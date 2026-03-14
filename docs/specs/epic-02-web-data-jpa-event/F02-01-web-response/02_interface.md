# Feature: F02-01 cartisan-web 响应体 — 接口契约

> **对应需求**: [01_requirement.md](./01_requirement.md)
>
> **注意**: 本文档为接口描述，使用伪代码和表格。Java 源代码在 Phase 4 生成。

---

## 1. 模块结构

### 1.1 新增模块

```
cartisan-web/
├── build.gradle.kts
└── src/main/java/com/cartisan/web/response/
    ├── ApiResponse.java
    └── PageResponse.java
```

### 1.2 依赖关系

```
cartisan-web
    └── depends on → cartisan-core
                      (CodeMessage, BaseCodeMessage)
```

**不依赖**: Spring Web、Jackson 等（本 Feature 为纯数据类）

---

## 2. cartisan-core 变更

### 2.1 BaseCodeMessage 枚举补充

在 `cartisan-core/src/main/java/com/cartisan/core/exception/BaseCodeMessage.java` 中添加：

```java
// 在枚举开头添加
/**
 * 200 OK - 请求成功。
 */
SUCCESS(200, "success", "Success");
```

**变更范围**: 单行插入，在 `BAD_REQUEST` 之前

---

## 3. ApiResponse 接口描述

### 3.1 数据结构（伪代码）

```java
package com.cartisan.web.response;

import com.cartisan.core.exception.CodeMessage;

/**
 * 统一 API 响应体。
 *
 * <p>使用 Record 实现，不可变对象。</p>
 *
 * @param <T> 响应数据类型
 */
public record ApiResponse<T>(
    /** HTTP 状态码/业务错误码 */
    int code,

    /** 响应消息 */
    String message,

    /** 响应数据，成功时为业务数据，错误时为 null */
    T data,

    /** 请求追踪 ID，可选字段（由 F02-03 填充） */
    String requestId
) {
    // 静态工厂方法见 3.2
}
```

### 3.2 静态工厂方法

#### 3.2.1 ok(T data) - 成功响应（带数据）

```java
/**
 * 构造成功响应。
 *
 * <p>使用 {@link BaseCodeMessage#SUCCESS} 作为响应码。</p>
 *
 * @param data 响应数据
 * @param <T>  数据类型
 * @return code=200, message="success", data=传入值, requestId=null
 */
public static <T> ApiResponse<T> ok(T data)
```

**实现逻辑**:
```
return new ApiResponse<>(
    BaseCodeMessage.SUCCESS.httpStatus(),  // 200
    BaseCodeMessage.SUCCESS.message(),     // "Success"
    data,
    null                                   // requestId 暂为 null
);
```

#### 3.2.2 ok() - 成功响应（无数据）

```java
/**
 * 构造成功响应（无数据）。
 *
 * @return code=200, message="success", data=null, requestId=null
 */
public static ApiResponse<Void> ok()
```

**实现逻辑**:
```
return ok(null);
```

#### 3.2.3 error(CodeMessage) - 错误响应（枚举）

```java
/**
 * 构造错误响应（使用错误码枚举）。
 *
 * @param codeMessage 错误码枚举
 * @return code=枚举.httpStatus(), message=枚举.message(), data=null
 */
public static ApiResponse<Void> error(CodeMessage codeMessage)
```

**实现逻辑**:
```
return new ApiResponse<>(
    codeMessage.httpStatus(),
    codeMessage.message(),
    null,
    null
);
```

#### 3.2.4 error(CodeMessage, Object...) - 错误响应（参数化）

```java
/**
 * 构造错误响应（支持参数化消息）。
 *
 * <p>消息格式使用 {@link java.text.MessageFormat#format(String, Object...)}，
 * 支持占位符：{@code {0}}、{@code {1}} 等。</p>
 *
 * <p>示例：
 * <pre>{@code
 * // 枚举定义为 INVALID_PARAMETER(400, "Invalid parameter: {0}")
 * ApiResponse.error(BaseCodeMessage.INVALID_PARAMETER, "email")
 * // → message="Invalid parameter: email"
 * }</pre></p>
 *
 * @param codeMessage 错误码枚举
 * @param args        消息参数（可为空）
 * @return 格式化后的错误响应
 */
public static ApiResponse<Void> error(CodeMessage codeMessage, Object... args)
```

**实现逻辑**:
```
String formattedMessage;
if (args == null || args.length == 0) {
    formattedMessage = codeMessage.message();
} else {
    formattedMessage = MessageFormat.format(codeMessage.message(), args);
}
return new ApiResponse<>(
    codeMessage.httpStatus(),
    formattedMessage,
    null,
    null
);
```

#### 3.2.5 error(int, String) - 错误响应（自定义）

```java
/**
 * 构造错误响应（自定义错误码和消息）。
 *
 * <p>用于以下场景：
 * <ul>
 *   <li>第三方异常转换（如 SQLException → 500 错误）</li>
 *   <li>临时/尚未纳入枚举的错误</li>
 *   <li>运行时动态错误码</li>
 * </ul></p>
 *
 * @param code    自定义错误码
 * @param message 自定义错误消息
 * @return 自定义错误响应
 */
public static ApiResponse<Void> error(int code, String message)
```

**实现逻辑**:
```
return new ApiResponse<>(code, message, null, null);
```

---

## 4. PageResponse 接口描述

### 4.1 数据结构（伪代码）

```java
package com.cartisan.web.response;

/**
 * 分页响应体。
 *
 * <p>使用 Record 实现，不可变对象。</p>
 *
 * @param <T> 列表项类型
 */
public record PageResponse<T>(
    /** 当前页数据列表 */
    List<T> items,

    /** 总记录数 */
    long total,

    /** 当前页码（从 1 开始） */
    int page,

    /** 每页大小 */
    int size
) {
    // 使用 Record 规范构造器，无额外工厂方法
}
```

### 4.2 字段约束

| 字段 | 约束 | 说明 |
|------|------|------|
| items | 非null | 可为空列表 `Collections.emptyList()` |
| total | ≥ 0 | 总记录数 |
| page | ≥ 1 | 页码从 1 开始 |
| size | > 0 | 每页大小 |

---

## 5. 核心流程（伪代码）

### 5.1 成功响应流程

```
Controller 层:
    result = service.doSomething()
    return ApiResponse.ok(result)

序列化后 JSON:
    {
      "code": 200,
      "message": "Success",
      "data": { ... },
      "requestId": null
    }
```

### 5.2 错误响应流程

```
ControllerAdvice 层 (F02-02):
    catch (CartisanException e) {
        return ApiResponse.error(e.getCodeMessage())
    }

序列化后 JSON:
    {
      "code": 404,
      "message": "Resource not found",
      "data": null,
      "requestId": "abc-123"
    }
```

### 5.3 参数化错误消息流程

```
Service 层:
    throw new DomainException(BaseCodeMessage.INVALID_PARAMETER, "email")

ControllerAdvice 层:
    CodeMessage cm = exception.getCodeMessage()
    // 需要从异常中获取原始参数，或重新格式化
    return ApiResponse.error(cm, "email")

序列化后 JSON:
    {
      "code": 400,
      "message": "Invalid parameter: email",
      "data": null,
      "requestId": null
    }
```

---

## 6. 技术方案说明

### 6.1 为什么使用 Record

| 特性 | 说明 |
|------|------|
| 不可变性 | 响应体不应被修改，Record 天然保证 |
| 样板代码减少 | 自动生成构造器、getter、equals、hashCode、toString |
| Java 21+ | 项目目标版本支持 |
| JSON 序列化 | Jackson 2.12+ 原生支持 Record |

### 6.2 为什么 requestId 不立即填充

| 方案 | 说明 | 不采用原因 |
|------|------|-----------|
| 在 ApiResponse 内部读 RequestContext | 引入 ThreadLocal 隐式依赖，不利测试 | ✗ |
| 现在立即实现 RequestContext | 增加 F02-01 复杂度，阻塞开发 | ✗ |
| requestId 暂为 null，后续由调用方传入 | 保持本 Feature 独立，职责单一 | ✓ |

### 6.3 MessageFormat vs String.format

选择 `MessageFormat`（{0} 风格）而非 `String.format`（%s 风格）：

| 理由 | 说明 |
|------|------|
| 一致性 | 与 `CartisanException` 和现有 `BaseCodeMessage` 保持一致 |
| 兼容性 | BaseCodeMessage 已使用 {0} 占位符，避免不兼容 |
| 规范性 | MessageFormat 是 Java 标准库的国际化消息格式化方案 |

**设计调整**：初期计划使用 String.format，但考虑到 BaseCodeMessage 已采用 MessageFormat 风格，为确保一致性，调整为使用 MessageFormat。

---

## 7. 测试策略

### 7.1 单元测试覆盖

| 类 | 测试方法 | 验证内容 |
|------|---------|---------|
| ApiResponseTest | testOkWithData | 验证 ok(data) 返回正确字段 |
| ApiResponseTest | testOkWithoutData | 验证 ok() 返回 data=null |
| ApiResponseTest | testErrorCodeMessage | 验证 error(CodeMessage) 映射 |
| ApiResponseTest | testErrorCodeMessageWithArgs | 验证 MessageFormat 格式化 |
| ApiResponseTest | testErrorCodeMessageWithEmptyArgs | 验证空 args 使用原 message |
| ApiResponseTest | testErrorCustom | 验证自定义 code/message |
| PageResponseTest | testConstructor | 验证构造器设置所有字段 |
| ApiResponseTest | testGenericTypeSafety | 验证泛型类型推导 |

### 7.2 不包含的测试

- JSON 序列化测试：Record 的序列化行为由 Jackson 保证，配置在 F02-09 处理
- 集成测试：本 Feature 无外部依赖，单元测试足够

---

## 8. 变更范围汇总

| 操作 | 模块 | 文件路径 | 说明 |
|------|------|---------|------|
| 修改 | cartisan-core | `exception/BaseCodeMessage.java` | 添加 SUCCESS 枚举值 |
| 新增 | cartisan-web | `response/ApiResponse.java` | 响应体 Record + 工厂方法 |
| 新增 | cartisan-web | `response/PageResponse.java` | 分页响应体 Record |
| 新增 | cartisan-web | `build.gradle.kts` | 模块构建配置 |
| 新增 | cartisan-web | `response/ApiResponseTest.java` | 单元测试 |
| 新增 | cartisan-web | `response/PageResponseTest.java` | 单元测试 |

---

## 9. 后续集成点

本 Feature 的交付物将在以下 Feature 中被使用：

| Feature | 集成方式 |
|---------|---------|
| F02-02（异常处理） | GlobalExceptionHandler 返回 `ApiResponse.error(...)` |
| F02-03（请求上下文） | Controller/Advice 从 RequestContext 读取 requestId 并传入 |
| F02-09（自动配置） | 统一 Jackson 序列化配置（如命名策略） |
