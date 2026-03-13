# Feature: F01-03 cartisan-core — exception 异常体系

> 版本：v0.1 | 日期：2026-03-13
> 依赖：F01-02 (cartisan-core domain 基础类型)
> 复杂度：S | 预估工时：0.5d

---

## 背景

cartisan-core 需要提供统一的异常处理基础设施，供所有业务项目复用。异常体系应遵循 DDD 六边形架构，区分领域层和应用层错误，并提供结构化的错误码机制。

当前 Java 项目的常见问题：
- 错误码分散、格式不统一（有的用数字、有的用字符串）
- 异常类型过多，全局异常处理器逻辑复杂
- 异常消息不支持参数化，导致相似错误重复定义
- HTTP 状态码映射逻辑散落在各处

## 目标

1. 提供统一的 `CodeMessage` 接口，定义错误码结构
2. 提供 `BaseCodeMessage` 枚举，包含 HTTP 规范错误码和最小通用业务错误码
3. 提供 `CartisanException` 基类，携带 `CodeMessage` 并支持参数化消息
4. 提供 `DomainException` 和 `ApplicationException`，按 DDD 分层区分错误来源
5. 异常消息支持 `MessageFormat` 风格的参数化

## 范围

### 包含（In Scope）

| 组件 | 说明 |
|------|------|
| `CodeMessage` 接口 | 定义 `code()`、`message()`、`httpStatus()` 三个方法 |
| `BaseCodeMessage` 枚举 | HTTP 规范错误码 + 4 个通用业务错误码 |
| `CartisanException` 类 | 异常基类，携带 `CodeMessage`，支持参数化和异常链 |
| `DomainException` 类 | 领域层异常，继承 `CartisanException` |
| `ApplicationException` 类 | 应用层异常，继承 `CartisanException` |
| 单元测试 | 覆盖所有公开 API 和异常场景 |

### 不包含（Out of Scope）

| 内容 | 原因 |
|------|------|
| `InfrastructureException` | 基础设施异常应在端口适配器中转换为 `DomainException` 或 `ApplicationException` |
| 全局异常处理器 | 属于 cartisan-web 模块，不在 core 范围内 |
| 国际化支持 | 当前版本仅支持中文，国际化作为未来扩展 |
| 错误码注册表/动态管理 | 错误码通过枚举静态定义，无需运行时动态管理 |

## 验收标准（Acceptance Criteria）

### AC1: CodeMessage 接口
- [ ] 接口定义 `String code()` 方法
- [ ] 接口定义 `String message()` 方法（返回带占位符的模板，如 `"User {0} not found"`）
- [ ] 接口定义 `int httpStatus()` 方法

### AC2: BaseCodeMessage 枚举
- [ ] 实现 `CodeMessage` 接口
- [ ] 包含 HTTP 规范错误码：400, 401, 403, 404, 405, 409, 415, 422, 429, 500, 503
- [ ] 包含通用业务错误码：
  - `UNKNOWN_ERROR` (500) - 未分类/未知错误
  - `INVALID_PARAMETER` (400) - 参数无效
  - `RESOURCE_NOT_FOUND` (404) - 资源不存在
  - `DUPLICATE` (409) - 重复（唯一约束冲突）

### AC3: CartisanException 基类
- [ ] 继承 `RuntimeException`
- [ ] 构造器接受 `(CodeMessage codeMessage, Object... args)`
- [ ] 构造器接受 `(CodeMessage codeMessage, Throwable cause, Object... args)`
- [ ] 构造时使用 `MessageFormat.format()` 格式化消息
- [ ] 提供 `getCodeMessage()` 方法返回结构化信息
- [ ] `getMessage()` 返回格式化后的消息
- [ ] 异常链通过标准 `Throwable.getCause()` 机制保留

### AC4: 异常子类
- [ ] `DomainException` 继承 `CartisanException`，提供与父类相同的构造器
- [ ] `ApplicationException` 继承 `CartisanException`，提供与父类相同的构造器

### AC5: 零外部依赖
- [ ] 模块不引入任何第三方依赖
- [ ] 仅使用 JDK 标准库（`java.text.MessageFormat`）

### AC6: 测试覆盖
- [ ] 单元测试覆盖所有异常类的构造方法
- [ ] 测试验证 `MessageFormat` 参数化消息正确格式化
- [ ] 测试验证异常链（cause）正确保留
- [ ] 测试验证 `CodeMessage` 的 `code()`、`message()`、`httpStatus()` 正确返回
- [ ] 测试覆盖边界场景：无参数、null cause、多参数

## 约束

### 技术约束
- **零外部依赖**：仅使用 JDK 标准库
- **遵循 F01-02 约定**：代码风格与现有 `domain` 包一致
- **ArchUnit 验证**：不依赖任何第三方库

### API 约束
- `CodeMessage.code()` 返回 `String` 类型（非数值）
- `CodeMessage.httpStatus()` 返回 `int` 类型（HTTP 状态码）
- 异常消息模板使用 `MessageFormat` 占位符语法（`{0}`, `{1}`）

## 使用示例（非代码，仅描述）

### 场景 1：领域层抛出业务规则违反异常
```java
// 在领域模型中
if (balance.compareTo(amount) < 0) {
    throw new DomainException(
        BaseCodeMessage.INVALID_PARAMETER,
        "Balance insufficient for withdrawal"
    );
}
```

### 场景 2：应用层抛出参数无效异常（带参数）
```java
// 在应用服务中
throw new ApplicationException(
    BaseCodeMessage.RESOURCE_NOT_FOUND,
    userId
);
// 最终消息：资源不存在：123
```

### 场景 3：端口适配器中转换基础设施异常
```java
// 在 Repository 实现中
try {
    jpaRepository.save(entity);
} catch (DataIntegrityViolationException e) {
    throw new DomainException(
        BaseCodeMessage.DUPLICATE,
        e,
        entity.getEmail()
    );
}
```

## 相关文档
- Epic Backlog: [F01-03](../00_epic_backlog.md#f01-03-cartisan-core--exception-异常体系)
- DECISIONS.md: (Phase 5 归档后补充)
