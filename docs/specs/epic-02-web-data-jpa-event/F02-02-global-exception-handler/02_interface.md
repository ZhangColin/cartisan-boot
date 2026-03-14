# Feature: F02-02 GlobalExceptionHandler — 接口契约

> 本文档描述 GlobalExceptionHandler 的接口设计，使用伪代码和表格形式。
> Java 源代码在 Phase 4 执行阶段生成。

---

## 1. 数据结构定义

### 1.1 FieldError（新增）

**类型**: Record
**包路径**: `com.cartisan.web.response`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| field | String | 是 | 字段路径，支持嵌套，如 `user.email` 或 `user.addresses[0].city` |
| message | String | 是 | 可读错误说明，由 Bean Validation 或自定义提供 |
| errorCode | String | 是 | 约束类型简称，如 `Email`、`NotNull`、`Size`、`NotBlank` |

**伪代码**：
```
record FieldError(
    String field,      // 字段路径
    String message,    // 可读错误说明
    String errorCode   // 约束类型
) {}
```

---

### 1.2 ApiResponse（变更）

**类型**: Record
**包路径**: `com.cartisan.web.response`

| 字段 | 类型 | 说明 | 变更 |
|------|------|------|------|
| code | int | HTTP 状态码 | 不变 |
| message | String | 响应消息 | 不变 |
| data | T | 响应数据 | 不变 |
| requestId | String | 请求追踪 ID | 不变 |
| **errors** | **List&lt;FieldError&gt;** | **字段级错误，默认 null** | **新增** |

**新增静态工厂方法**：

```
static ApiResponse<Void> validationError(List<FieldError> errors)
    返回: code=400, message="Parameter validation failed", data=null, requestId=null, errors=传入值
```

**现有静态工厂方法**（内部实现需补上 errors 参数传 null）：
- `ok(T data)` → code=200, message="success", data=传入值, requestId=null, errors=null
- `ok()` → code=200, message="success", data=null, requestId=null, errors=null
- `error(CodeMessage codeMessage)` → code=codeMessage.code(), message=codeMessage.message(), data=null, requestId=null, errors=null
- `error(int code, String message)` → code=传入值, message=传入值, data=null, requestId=null, errors=null

---

## 2. 异常处理器接口

### 2.1 GlobalExceptionHandler（新增）

**类型**: Class
**包路径**: `com.cartisan.web.exception`
**注解**: `@ControllerAdvice`
**依赖**: `@Slf4j`（日志）

#### 方法列表

| # | 方法名 | 异常类型 | HTTP 状态码 | 返回值 | 日志级别 | 堆栈 |
|--|--------|---------|-----------|--------|---------|------|
| 1 | handleCartisanException | CartisanException | codeMessage.code() | ApiResponse.error(codeMessage) | 4xx→WARN, 5xx→ERROR | 4xx→否, 5xx→是 |
| 2 | handleConstraintViolation | ConstraintViolationException | 400 | ApiResponse.validationError(errors) | WARN | 否 |
| 3 | handleMethodArgumentNotValid | MethodArgumentNotValidException | 400 | ApiResponse.validationError(errors) | WARN | 否 |
| 4 | handleAccessDenied | IllegalArgumentException (消息含"Access denied") | 403 | ApiResponse.error(FORBIDDEN) | WARN | 否 |
| 5 | handleMethodNotSupported | HttpRequestMethodNotSupportedException | 405 | ApiResponse.error(405, message) | WARN | 否 |
| 6 | handleMediaTypeNotSupported | HttpMediaTypeNotSupportedException | 415 | ApiResponse.error(415, message) | WARN | 否 |
| 7 | handleMessageNotReadable | HttpMessageNotReadableException | 400 | ApiResponse.error(400, "Malformed request body") | WARN | 否 |
| 8 | handleMissingParameter | MissingServletRequestParameterException | 400 | ApiResponse.error(400, "Missing required parameter: X") | WARN | 否 |
| 9 | handleMissingHeader | MissingRequestHeaderException | 400 | ApiResponse.error(400, "Missing required header: X") | WARN | 否 |
| 10 | handleBindException | BindException | 400 | ApiResponse.validationError(errors) | WARN | 否 |
| 11 | handleNotFound | NoHandlerFoundException, MethodArgumentTypeMismatchException | 404 | ApiResponse.error(NOT_FOUND) | WARN | 否 |
| 12 | handleException | Exception | 500 | ApiResponse.error(INTERNAL_SERVER_ERROR) | ERROR | 是 |

**注**: 方法 4 使用 `IllegalArgumentException` 作为临时权限拒绝处理（检查消息是否包含 "Access denied"）。实际项目中应替换为 Spring Security 的 `AccessDeniedException`。

#### 方法签名（伪代码）

```
// 1. CartisanException
ResponseEntity<ApiResponse<Void>> handleCartisanException(CartisanException ex)
    提取: CodeMessage cm = ex.getCodeMessage()
    判断: if (cm.code() >= 500) → log.error(..., ex); else → log.warn(...)
    返回: ResponseEntity.status(cm.code()).body(ApiResponse.error(cm))

// 2. ConstraintViolationException
ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex)
    转换: errors = ex.getConstraintViolations().stream()
        .map(cv → new FieldError(
            field = cv.getPropertyPath().toString(),
            message = cv.getMessage(),
            errorCode = cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
        ))
        .toList()
    日志: log.warn("Validation failed: {}", errors)
    返回: ResponseEntity.badRequest().body(ApiResponse.validationError(errors))

// 3. MethodArgumentNotValidException
ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex)
    转换: errors = ex.getBindingResult().getFieldErrors().stream()
        .map(fe → new FieldError(
            field = fe.getField(),
            message = fe.getDefaultMessage(),
            errorCode = fe.getCode() ?? "Invalid"
        ))
        .toList()
    日志: log.warn("Validation failed: {}", errors)
    返回: ResponseEntity.badRequest().body(ApiResponse.validationError(errors))

// 4. IllegalArgumentException (Access denied)
ResponseEntity<ApiResponse<Void>> handleAccessDenied(IllegalArgumentException ex)
    判断: if (ex.getMessage() != null && ex.getMessage().contains("Access denied"))
        日志: log.warn("Access denied: {}", ex.getMessage())
        返回: ResponseEntity.status(403).body(ApiResponse.error(BaseCodeMessage.FORBIDDEN))
    否则:
        日志: log.warn("Bad request: {}", ex.getMessage())
        返回: ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()))

// 5. HttpRequestMethodNotSupportedException
ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex)
    提取: supported = ex.getSupportedHttpMethods()
    转换: supportedMethods = (supported != null)
        ? supported.stream().map(HttpMethod::name).joining(", ")
        : ""
    日志: log.warn("Method not supported: {} (supported: {})", ex.getMethod(), supportedMethods)
    返回: ResponseEntity.status(405)
        .body(ApiResponse.error(405, "Method " + ex.getMethod() + " not supported. Supported: " + supportedMethods))

// 6. HttpMediaTypeNotSupportedException
ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex)
    日志: log.warn("Media type not supported: {}", ex.getContentType())
    返回: ResponseEntity.status(415)
        .body(ApiResponse.error(415, "Media type not supported: " + ex.getContentType()))

// 7. HttpMessageNotReadableException
ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex)
    日志: log.warn("Request body not readable")
    返回: ResponseEntity.badRequest()
        .body(ApiResponse.error(400, "Malformed request body"))

// 8. MissingServletRequestParameterException
ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex)
    日志: log.warn("Missing parameter: {}", ex.getParameterName())
    返回: ResponseEntity.badRequest()
        .body(ApiResponse.error(400, "Missing required parameter: " + ex.getParameterName()))

// 9. MissingRequestHeaderException
ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException ex)
    日志: log.warn("Missing header: {}", ex.getHeaderName())
    返回: ResponseEntity.badRequest()
        .body(ApiResponse.error(400, "Missing required header: " + ex.getHeaderName()))

// 10. BindException
ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex)
    转换: 同 MethodArgumentNotValidException，使用 ex.getBindingResult().getFieldErrors()
    日志: log.warn("Binding failed: {}", errors)
    返回: ResponseEntity.badRequest().body(ApiResponse.validationError(errors))

// 11. NoHandlerFoundException / MethodArgumentTypeMismatchException
ResponseEntity<ApiResponse<Void>> handleNotFound()
    返回: ResponseEntity.status(404).body(ApiResponse.error(BaseCodeMessage.NOT_FOUND))

// 12. Exception（兜底）
ResponseEntity<ApiResponse<Void>> handleException(Exception ex)
    日志: log.error("Unexpected error", ex)
    返回: ResponseEntity.status(500)
        .body(ApiResponse.error(BaseCodeMessage.INTERNAL_SERVER_ERROR))
```

---

## 3. 响应示例

### 3.1 成功响应（无变更）

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 123, "name": "test" },
  "requestId": null,
  "errors": null
}
```

### 3.2 业务异常（CartisanException）

**请求**: `GET /api/users/999`
**异常**: `DomainException(BaseCodeMessage.RESOURCE_NOT_FOUND, "user/999")`

```json
{
  "code": 404,
  "message": "Resource not found: user/999",
  "data": null,
  "requestId": null,
  "errors": null
}
```

### 3.3 校验失败（ConstraintViolationException）

**请求**: `POST /api/users`
**参数**: `email=invalid`, `password=123`

```json
{
  "code": 400,
  "message": "Parameter validation failed",
  "data": null,
  "requestId": null,
  "errors": [
    {
      "field": "email",
      "message": "must be well-formed",
      "errorCode": "Email"
    },
    {
      "field": "password",
      "message": "size must be between 8 and 20",
      "errorCode": "Size"
    }
  ]
}
```

### 3.4 HTTP 方法不支持

**请求**: `POST /api/users/123`（只支持 GET/PUT）
**异常**: `HttpRequestMethodNotSupportedException`

```json
{
  "code": 405,
  "message": "Method POST not supported. Supported: GET, PUT",
  "data": null,
  "requestId": null,
  "errors": null
}
```

### 3.5 兜底异常

**请求**: 任何触发未预期异常的请求
**异常**: `NullPointerException` 等

```json
{
  "code": 500,
  "message": "Internal server error",
  "data": null,
  "requestId": null,
  "errors": null
}
```

---

## 4. 依赖配置

### 4.1 新增依赖（cartisan-web/build.gradle.kts）

```kotlin
dependencies {
    // 现有依赖
    implementation(project(":cartisan-core"))

    // 新增：Spring Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // 新增：Spring Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // 新增：测试依赖
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}
```

### 4.2 传递依赖说明

| 依赖 | 提供 | 用途 |
|------|------|------|
| spring-boot-starter-web | @ControllerAdvice, @ExceptionHandler, ResponseEntity, MockMvc | Web MVC 功能 |
| spring-boot-starter-validation | Bean Validation, @Valid, @Validated, ConstraintViolation | 参数校验 |
| spring-boot-starter-test | MockMvc, @SpringBootTest, @AutoConfigureMockMvc | 集成测试 |

---

## 5. 日志策略

| HTTP 状态码范围 | 日志级别 | 堆栈 | 示例 |
|---------------|---------|------|------|
| 4xx | WARN | 否 | `Business error: Resource not found: user/999` |
| 5xx | ERROR | 是 | `Unexpected error` + 完整异常堆栈 |

**规则**：
- `CartisanException`: 根据 `codeMessage.code()` 判断，>= 500 使用 ERROR，否则使用 WARN
- 校验异常（3 种）: 统一使用 WARN，不打印堆栈
- `IllegalArgumentException` (Access denied): WARN
- `NoHandlerFoundException` / `MethodArgumentTypeMismatchException`: WARN
- 其他 Spring MVC 异常: WARN
- 兜底 `Exception`: ERROR + 堆栈
