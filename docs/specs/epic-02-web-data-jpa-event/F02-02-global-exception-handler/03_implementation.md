# Feature: F02-02 GlobalExceptionHandler — 实施计划

> **本文档描述原子任务清单，按 TDD 红绿循环组织。**
> **Step A: 契约代码化 → Step B: 编写测试（红灯）→ Step C: 编写实现（绿灯）**

---

## 目标复述

提供 `@ControllerAdvice` 全局异常处理器，将 11 种异常类型映射为统一的 `ApiResponse` 响应。
- 扩展 `ApiResponse` 增加 `errors: List<FieldError>` 字段
- 新增 `FieldError` record
- 实现日志策略：4xx → WARN 不打印堆栈，5xx → ERROR 打印堆栈
- 添加 Spring Web/Validation 依赖

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `cartisan-web/build.gradle.kts` | 添加 Spring Web/Validation/Test 依赖 |
| 新增 | `cartisan-web/src/main/java/com/cartisan/web/response/FieldError.java` | 字段级错误 record |
| 修改 | `cartisan-web/src/main/java/com/cartisan/web/response/ApiResponse.java` | 增加 errors 字段，新增 validationError 工厂方法 |
| 新增 | `cartisan-web/src/main/java/com/cartisan/web/exception/GlobalExceptionHandler.java` | @ControllerAdvice 异常处理器 |
| 新增 | `cartisan-web/src/test/java/com/cartisan/web/exception/GlobalExceptionHandlerTest.java` | MockMvc 集成测试 |
| 新增 | `cartisan-web/src/test/java/com/cartisan/web/TestController.java` | 测试用 Controller |

---

## 原子任务清单

### Step 1: 配置依赖

- **文件**: `cartisan-web/build.gradle.kts`
- **内容**: 添加 spring-boot-starter-web、spring-boot-starter-validation、spring-boot-starter-test 依赖
- **验证**: `./gradlew :cartisan-web:dependencies` 确认依赖正确解析

---

### Step 2: 契约代码化 — FieldError + ApiResponse 扩展

- **文件**:
  - `cartisan-web/src/main/java/com/cartisan/web/response/FieldError.java`
  - `cartisan-web/src/main/java/com/cartisan/web/response/ApiResponse.java`
- **内容**:
  - 创建 `FieldError` record（field, message, errorCode）
  - 在 `ApiResponse` 增加 `errors` 字段
  - 新增 `validationError(List<FieldError>)` 静态工厂方法
  - 更新所有现有静态工厂方法，补上 `errors` 参数（传 null）
- **验证**: `./gradlew :cartisan-web:compileJava` 编译通过

---

### Step 3: 编写 FieldError 和 ApiResponse 扩展的单元测试（红灯）

- **文件**: `cartisan-web/src/test/java/com/cartisan/web/response/FieldErrorTest.java`（可选，Record 可跳过）
- **文件**: `cartisan-web/src/test/java/com/cartisan/web/response/ApiResponseTest.java`
- **内容**:
  - 测试 `validationError()` 方法构造正确响应
  - 测试 `errors` 字段在成功响应中为 null
  - 测试现有工厂方法不受影响
- **验证**: 编译通过 + 测试全红（实现未更新，现有测试可能已绿）

---

### Step 4: 更新 ApiResponse 实现以支持 errors（绿灯）

- **文件**: `cartisan-web/src/main/java/com/cartisan/web/response/ApiResponse.java`
- **内容**: 实现 Step 2 中定义的变更
- **验证**:
  - `./gradlew :cartisan-web:compileJava` 编译通过
  - `./gradlew :cartisan-web:test` 测试全绿
  - `./gradlew :cartisan-web:check` 通过（如有 ArchUnit 规则）

---

### Step 5: 编写 GlobalExceptionHandler 骨架测试（红灯）

- **文件**: `cartisan-web/src/test/java/com/cartisan/web/exception/GlobalExceptionHandlerTest.java`
- **文件**: `cartisan-web/src/test/java/com/cartisan/web/TestController.java`
- **内容**:
  - 创建测试 Controller，提供各异常类型的触发端点
  - 测试 `CartisanException` 映射到正确状态码和响应格式
  - 测试 `ConstraintViolationException` 返回 errors 数组
  - 测试 `MethodArgumentNotValidException` 返回 errors 数组
  - 测试 `AccessDeniedException` 返回 403
  - 测试 `HttpRequestMethodNotSupportedException` 返回 405
  - 测试 `HttpMediaTypeNotSupportedException` 返回 415
  - 测试 `HttpMessageNotReadableException` 返回 400
  - 测试 `MissingServletRequestParameterException` 返回 400
  - 测试 `MissingRequestHeaderException` 返回 400
  - 测试 `BindException` 返回 errors 数组
  - 测试兜底 `Exception` 返回 500
- **验证**: 编译通过 + 测试全红（GlobalExceptionHandler 不存在）

---

### Step 6: 编写 GlobalExceptionHandler 实现 — 业务异常（绿灯）

- **文件**: `cartisan-web/src/main/java/com/cartisan/web/exception/GlobalExceptionHandler.java`
- **内容**:
  - 创建 `@ControllerAdvice` 类
  - 实现 `handleCartesanException` 方法
  - 实现 `handleException` 兜底方法
- **验证**:
  - `./gradlew :cartisan-web:test` 相关测试变绿
  - 日志输出符合策略（4xx WARN, 5xx ERROR）

---

### Step 7: 编写 GlobalExceptionHandler 实现 — 校验异常（绿灯）

- **文件**: `cartisan-web/src/main/java/com/cartisan/web/exception/GlobalExceptionHandler.java`
- **内容**:
  - 实现 `handleConstraintViolation` 方法（转换 ConstraintViolation → FieldError）
  - 实现 `handleMethodArgumentNotValid` 方法（转换 FieldError → FieldError）
  - 实现 `handleBindException` 方法
- **验证**:
  - `./gradlew :cartisan-web:test` 校验异常相关测试变绿
  - `errors` 数组结构正确（field, message, errorCode）

---

### Step 8: 编写 GlobalExceptionHandler 实现 — 其他异常（绿灯）

- **文件**: `cartisan-web/src/main/java/com/cartisan/web/exception/GlobalExceptionHandler.java`
- **内容**:
  - 实现 `handleAccessDenied` 方法
  - 实现 `handleMethodNotSupported` 方法（处理 null safe）
  - 实现 `handleMediaTypeNotSupported` 方法
  - 实现 `handleMessageNotReadable` 方法
  - 实现 `handleMissingParameter` 方法
  - 实现 `handleMissingHeader` 方法
- **验证**:
  - `./gradlew :cartisan-web:test` 全部测试变绿
  - `./gradlew :cartisan-web:check` 通过

---

## 核心流程（伪代码）

```
异常处理流程：
1. Controller 抛出异常
2. Spring MVC 捕获，转发给 @ControllerAdvice
3. GlobalExceptionHandler 按异常类型匹配：
   - CartisanException → 提取 CodeMessage → ApiResponse.error()
   - 校验异常（3 种）→ 收集 FieldError → ApiResponse.validationError()
   - AccessDenied → 403 + FORBIDDEN
   - HTTP 方法/媒体类型异常 → 405/415 + 详细消息
   - 请求格式异常 → 400 + 简明消息
   - 参数/请求头缺失 → 400 + 缺失项名称
   - Exception → 500 + INTERNAL_SERVER_ERROR
4. 根据最终 HTTP 状态码决定日志策略
5. 返回 ResponseEntity<ApiResponse<Void>>
```

---

## 辅助方法实现要点

### extractField(javax.validation.Path path)
```
return path.toString()
// 返回: "user.email" 或 "user.addresses[0].city"
```

### extractCode(org.springframework.validation.FieldError fe)
```
String code = fe.getCode()
return code != null ? code : "Invalid"
// 返回: "Email", "Size", "NotNull" 等，或 "Invalid" 作为兜底
```

---

## 进度跟踪

| Step | 描述 | 状态 |
|------|------|------|
| 1 | 配置依赖 | ⬜ |
| 2 | 契约代码化 | ⬜ |
| 3 | FieldError/ApiResponse 测试（红） | ⬜ |
| 4 | FieldError/ApiResponse 实现（绿） | ⬜ |
| 5 | GlobalExceptionHandler 测试（红） | ⬜ |
| 6 | GlobalExceptionHandler 实现 - 业务异常（绿） | ⬜ |
| 7 | GlobalExceptionHandler 实现 - 校验异常（绿） | ⬜ |
| 8 | GlobalExceptionHandler 实现 - 其他异常（绿） | ⬜ |

---

## 注意事项

1. **Logback 配置**: 测试中可能需要配置 `logback-test.xml` 以验证日志级别
2. **测试上下文**: 使用 `@Import(TestController.class)` 确保 Controller 和 Handler 在同一上下文
3. **异常顺序**: 更具体的异常（如 `MethodArgumentNotValidException`）需在更通用的（如 `Exception`）之前
4. **空安全**: `HttpRequestMethodNotSupportedException.getSupportedHttpMethods()` 可能为 null
