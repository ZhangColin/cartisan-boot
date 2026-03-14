# Feature: F02-02 GlobalExceptionHandler — 全局异常处理

> **Epic**: Epic 2: Web + Data-JPA + Event
> **依赖**: F02-01 (ApiResponse/PageResponse)
> **复杂度**: M
> **预估代码量**: 100-150 行

---

## 背景

在 Spring Boot 应用中，异常处理分散在各处会导致不一致的响应格式。需要一个全局异常处理器，将各类异常统一映射为标准的 `ApiResponse` 响应，确保：
- 正确的 HTTP 状态码（4xx vs 5xx）
- 统一的响应体结构
- 字段级校验错误信息
- 合理的日志策略

## 目标

- 提供 `@ControllerAdvice` 全局异常处理器，拦截并处理 11 种异常类型
- 将参数校验失败的字段级错误以结构化方式返回（`errors` 数组）
- 实现合理的日志策略：4xx 使用 WARN 级别不打印堆栈，5xx 使用 ERROR 级别打印完整堆栈
- 确保错误响应不暴露敏感信息

## 范围

### 包含（In Scope）

1. **异常类型覆盖**（共 11 种）：
   - `CartisanException` — 业务异常基类
   - `ConstraintViolationException` — Bean Validation `@Validated` 校验失败
   - `MethodArgumentNotValidException` — `@Valid @RequestBody` 校验失败
   - `BindException` — 非 `@Valid` 的绑定失败（如 `@RequestParam` 对象绑定）
   - `AccessDeniedException` — Spring Security 权限拒绝
   - `HttpRequestMethodNotSupportedException` — HTTP 方法不支持
   - `HttpMediaTypeNotSupportedException` — Content-Type 不支持
   - `HttpMessageNotReadableException` — 请求体 JSON 解析失败
   - `MissingServletRequestParameterException` — 缺少必填请求参数
   - `MissingRequestHeaderException` — 缺少必填请求头
   - `Exception` — 兜底处理

2. **ApiResponse 结构扩展**：
   - 新增 `errors: List<FieldError>` 字段
   - 新增 `FieldError` record（field, message, errorCode）

3. **依赖管理**：
   - 添加 `spring-boot-starter-web`
   - 添加 `spring-boot-starter-validation`
   - 添加 `spring-boot-starter-test`（MockMvc 测试）

### 不包含（Out of Scope）

- 国际化（i18n）错误消息（由业务项目自行实现）
- 错误码枚举扩展（只使用 `BaseCodeMessage`）
- 请求追踪 ID（requestId 填充由 F02-03 负责，当前为 null）
- 日志级别动态配置（固定策略：4xx → WARN，5xx → ERROR）

## 验收标准（Acceptance Criteria）

### AC1: CartisanException 映射正确

| 场景 | 预期 HTTP 状态码 | 预期响应体 | 日志级别 | 堆栈 |
|------|----------------|-----------|---------|------|
| `CartisanException(httpStatus=400)` | 400 | `ApiResponse.error(codeMessage)` | WARN | 不打印 |
| `CartisanException(httpStatus=500)` | 500 | `ApiResponse.error(codeMessage)` | ERROR | 打印 |

**注**：日志策略由实现保证，单测重点验证响应结构。

### AC2: 参数校验失败返回字段级错误

| 异常类型 | 预期 HTTP 状态码 | 预期响应体结构 |
|---------|----------------|---------------|
| `ConstraintViolationException` | 400 | `code=400, message="Parameter validation failed", errors=[...]` |
| `MethodArgumentNotValidException` | 400 | `code=400, message="Parameter validation failed", errors=[...]` |
| `BindException` | 400 | `code=400, message="Parameter validation failed", errors=[...]` |

`errors` 数组中每项包含：
- `field`: 字段路径（如 `"user.email"` 或 `"user.addresses[0].city"`）
- `message`: 可读错误说明
- `errorCode`: 约束类型（如 `"Email"`, `"NotNull"`, `"Size"`）

**示例**：
```json
{
  "code": 400,
  "message": "Parameter validation failed",
  "data": null,
  "requestId": null,
  "errors": [
    {"field": "email", "message": "must be well-formed", "errorCode": "Email"},
    {"field": "password", "message": "size must be between 8 and 20", "errorCode": "Size"}
  ]
}
```

### AC3: AccessDeniedException 返回 403

| 场景 | 预期 HTTP 状态码 | 预期响应体 |
|------|----------------|-----------|
| `AccessDeniedException` | 403 | `ApiResponse.error(FORBIDDEN)` |

### AC4: HttpRequestMethodNotSupportedException 返回 405

| 场景 | 预期 HTTP 状态码 | 预期响应体 |
|------|----------------|-----------|
| `HttpRequestMethodNotSupportedException` | 405 | `ApiResponse.error(405, "Method X not supported. Supported: Y, Z")` |

### AC5: HttpMediaTypeNotSupportedException 返回 415

| 场景 | 预期 HTTP 状态码 | 预期响应体 |
|------|----------------|-----------|
| `HttpMediaTypeNotSupportedException` | 415 | `ApiResponse.error(415, "Media type not supported: ...")` |

### AC6: HttpMessageNotReadableException 返回 400

| 场景 | 预期 HTTP 状态码 | 预期响应体 |
|------|----------------|-----------|
| `HttpMessageNotReadableException` | 400 | `ApiResponse.error(400, "Malformed request body")` |

**注**：不暴露原始异常消息（避免泄露内部结构）。

### AC7: MissingServletRequestParameterException 返回 400

| 场景 | 预期 HTTP 状态码 | 预期响应体 |
|------|----------------|-----------|
| `MissingServletRequestParameterException` | 400 | `ApiResponse.error(400, "Missing required parameter: X")` |

### AC8: MissingRequestHeaderException 返回 400

| 场景 | 预期 HTTP 状态码 | 预期响应体 |
|------|----------------|-----------|
| `MissingRequestHeaderException` | 400 | `ApiResponse.error(400, "Missing required header: X")` |

### AC9: 兜底 Exception 返回 500

| 场景 | 预期 HTTP 状态码 | 预期响应体 | 日志级别 | 堆栈 |
|------|----------------|-----------|---------|------|
| 未捕获的 `Exception` | 500 | `ApiResponse.error(INTERNAL_SERVER_ERROR)` | ERROR | 打印 |

### AC10: 测试覆盖

使用 MockMvc 集成测试验证所有 11 种异常的映射正确性。

**测试上下文配置**：确保 `GlobalExceptionHandler` 和测试 `TestController` 在同一 Spring 上下文中，可通过以下方式之一实现：
- 在 `src/test/java` 下创建 `@SpringBootApplication`，确保扫描到两者
- 在测试类上使用 `@Import(TestController.class)`

### AC11: 依赖正确配置

- `cartisan-web` 的 `build.gradle.kts` 正确声明 `implementation` 依赖
- 编译通过，无版本冲突

## 约束

### 性能
- 异常处理不应显著增加响应延迟（目标 < 10ms）

### 安全
- 错误消息不暴露敏感信息（密码、内部路径、完整堆栈）
- 不返回用户输入的错误值（rejectedValue）

### 兼容性
- 支持 Spring Boot 3.4.x
- 遵循 cartisan-boot 设计文档约定（`CodeMessage.code()` 返回 `int`）

### 依赖
- 必须先完成 F02-01（ApiResponse/PageResponse）
- 依赖 cartisan-core 的 `CartisanException`、`CodeMessage`、`BaseCodeMessage`

## 设计决策记录

本 Feature 实现过程中做出的技术决策将记录到 `docs/decisions/DECISIONS.md`。
