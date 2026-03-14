# Feature: F02-01 cartisan-web 响应体

> **Epic**: Epic 2 - Web + Data-JPA + Event
>
> **依赖**: Epic 1（cartisan-core 的 CodeMessage、CartisanException）
>
> **复杂度**: S（Small，50-80 行）

---

## 背景

cartisan-boot 作为业务无关的技术基础框架，需要为所有基于其构建的 Spring Boot 项目提供统一的 HTTP 响应格式。当前 cartisan-core 已定义了 `CodeMessage` 错误码接口和 `CartisanException` 异常体系，但缺少统一的响应体封装。

本 Feature 是 cartisan-web 模块的第一个交付物，为后续的异常处理器（F02-02）、请求上下文（F02-03）提供基础。

## 目标

1. 创建 `cartisan-web` 模块，作为 Web 层能力的统一封装
2. 定义 `ApiResponse<T>` 响应体，提供成功和错误场景的静态工厂方法
3. 定义 `PageResponse<T>` 分页响应体
4. 补充 `BaseCodeMessage.SUCCESS` 枚举值（cartisan-core 遗留内容）

## 范围

### 包含（In Scope）

**cartisan-core 补充：**
- 在 `BaseCodeMessage` 枚举中添加 `SUCCESS(200, "success", "Success")`

**cartisan-web 模块：**
- `ApiResponse<T>` Record
  - 字段：`code: int`, `message: String`, `data: T`, `requestId: String`
  - 静态工厂：
    - `ok(T data)` — 成功响应，带数据
    - `ok()` — 成功响应，无数据
    - `error(CodeMessage codeMessage)` — 使用枚举错误码
    - `error(CodeMessage codeMessage, Object... args)` — 支持参数化消息
    - `error(int code, String message)` — 自定义错误（用于第三方异常转换）
- `PageResponse<T>` Record
  - 字段：`items: List<T>`, `total: long`, `page: int`, `size: int`
  - 使用 Record 规范构造器，无额外工厂方法

**测试：**
- `ApiResponse` 工厂方法的单元测试
- `PageResponse` 构造器的单元测试
- 泛型类型安全验证

### 不包含（Out of Scope）

| 不包含内容 | 原因 |
|-----------|------|
| `requestId` 的填充逻辑 | 由 F02-03（RequestContext）提供，本 Feature 只保留字段 |
| JSON 序列化配置 | Jackson 序列化行为由 F02-09（自动配置）统一处理 |
| 全局异常处理器 | 由 F02-02 实现本 Feature 的响应体与异常的映射 |
| Spring Data Page 转换 | 当前使用 Record 构造器，后续如需要可在 F02-04 补充工厂方法 |

## 验收标准（Acceptance Criteria）

### AC1: BaseCodeMessage.SUCCESS 补充

- [ ] `BaseCodeMessage` 新增 `SUCCESS` 枚举值
- [ ] `SUCCESS.code()` 返回 `"success"`
- [ ] `SUCCESS.message()` 返回 `"Success"`
- [ ] `SUCCESS.httpStatus()` 返回 `200`

### AC2: ApiResponse.ok() 成功响应

- [ ] `ApiResponse.ok(data)` 返回 `code=200, message="success", data=传入值, requestId=null`
- [ ] `ApiResponse.ok()` 返回 `code=200, message="success", data=null, requestId=null`

### AC3: ApiResponse.error(CodeMessage) 错误响应

- [ ] `error(CodeMessage)` 返回 `code=枚举.httpStatus(), message=枚举.message(), data=null`
- [ ] 示例：`error(BaseCodeMessage.NOT_FOUND)` → `code=404, message="Resource not found"`

### AC4: ApiResponse.error(CodeMessage, Object...) 参数化消息

- [ ] `args` 为空数组时，直接使用 `codeMessage.message()`
- [ ] `args` 非空时，使用 `MessageFormat.format(codeMessage.message(), args)` 格式化
- [ ] 支持占位符格式：`{0}`、`{1}` 等（与 BaseCodeMessage 一致）
- [ ] 示例：`error(BaseCodeMessage.INVALID_PARAMETER, "email")` → `message="Invalid parameter: email"`（枚举定义是 `"Invalid parameter: {0}"`）

### AC5: ApiResponse.error(int, String) 自定义错误

- [ ] `error(500, "Third party service unavailable")` → `code=500, message="Third party service unavailable"`
- [ ] 用于第三方异常转换、临时错误等场景

### AC6: PageResponse 构造器

- [ ] `new PageResponse<>(items, total, page, size)` 正确设置所有字段
- [ ] `items` 可为空列表，不为 null

### AC7: 泛型类型安全

- [ ] `ApiResponse<String>` 类型推导正确
- [ ] `ApiResponse<User>` 类型推导正确
- [ ] `PageResponse<OrderItem>` 类型推导正确

### AC8: 模块依赖正确

- [ ] `cartisan-web` 依赖 `cartisan-core`
- [ ] 不依赖 Spring Web（本 Feature 是纯数据类，不引入 Spring 依赖）

## 约束

### 技术约束

- 使用 Java Record（Java 21+）
- 泛型类型参数命名：`T` 表示数据类型
- `requestId` 字段允许为 `null`，等 F02-03 完成后再集成

### 编码规范

- 遵循 cartisan-boot 编码规范（CLAUDE.md）
- 公共方法必须有 JavaDoc
- 测试方法命名遵循 `docs/skills/SKILL.md` 的 TEST-002 规则

### 质量门禁

- 编译通过：`./gradlew :cartisan-web:compileJava`
- 测试通过：`./gradlew :cartisan-web:test`
- ArchUnit 通过（如有架构规则）

## 参考文档

- 设计文档：[cartisan-boot-设计文档.md](../../../cartisan-boot-设计文档.md)
- Epic Backlog：[00_epic_backlog.md](../00_epic_backlog.md)
- AI 协作 SOP：[AI协作开发SOP.md](../../../sop/AI协作开发SOP.md)
- cartisan-core 代码：`cartisan-core/src/main/java/com/cartisan/core/exception/`
