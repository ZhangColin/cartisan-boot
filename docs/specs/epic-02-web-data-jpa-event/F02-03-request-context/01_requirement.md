# Feature: F02-03 — RequestContext 请求上下文

> **Epic**: Epic 2 — Web + Data-JPA + Event
>
> **复杂度**: S (50-100 行代码)
>
> **依赖**: 无

---

## 背景

在 Web 应用中，需要为每个请求维护一个上下文，存储请求追踪 ID 和客户端 IP 地址。这些信息用于：

1. **全链路追踪**：requestId 填充到 ApiResponse 和日志中，方便问题排查
2. **审计与限流**：clientIp 用于日志记录、简单审计和限流策略

当前 `ApiResponse` 已预留 `requestId` 字段（F02-01），但尚无机制在请求生命周期中存储和传递这些信息。

---

## 目标

- 提供 `RequestContext` 类，基于 ThreadLocal 存储当前请求的 requestId 和 clientIp
- 提供 `RequestContextFilter`，在每个请求进入时初始化 RequestContext，结束时清理
- 支持链路追踪：优先从 Header 读取 requestId，没有则生成 UUID
- 支持代理环境：按 `X-Forwarded-For` → `X-Real-IP` → `RemoteAddr` 优先级提取 clientIp

---

## 范围

### 包含（In Scope）

- `RequestContext` 类：提供 `getRequestId()` 和 `getClientIp()` 静态方法
- `RequestContextFilter` 类：Filter 实现，初始化和清理 RequestContext
- requestId 生成/读取策略
- clientIp 提取策略（支持反向代理场景）
- ThreadLocal 清理机制
- 容错处理：初始化失败不影响业务请求

### 不包含（Out of Scope）

- ~~requestId 填充到 ApiResponse~~ — 由 F02-03 之后的 ResponseBodyAdvice 或手动填充处理
- ~~MDC 集成（logback 等）~~ — 后续集成点
- ~~用户身份信息（userId、username）~~ — 由 cartisan-security 模块的 SecurityContext 负责
- ~~可配置的代理信任策略~~ — 后续根据需求扩展

---

## 验收标准（Acceptance Criteria）

### AC1: requestId 正确生成和传递

- **Given**: 请求不含 `X-Request-Id` Header
- **When**: Filter 处理请求
- **Then**: 生成新的 UUID 作为 requestId，`RequestContext.getRequestId()` 返回非空字符串

### AC2: requestId 支持链路追踪

- **Given**: 请求包含 `X-Request-Id: trace-123` Header
- **When**: Filter 处理请求
- **Then**: `RequestContext.getRequestId()` 返回 `"trace-123"`（trim 后）

### AC3: 空白 requestId Header 时生成新 ID

- **Given**: 请求包含 `X-Request-Id: "   "` Header（仅空白）
- **When**: Filter 处理请求
- **Then**: 生成新的 UUID，不使用空白值

### AC4: clientIp 从 X-Forwarded-For 提取

- **Given**: 请求包含 `X-Forwarded-For: 1.2.3.4, 5.6.7.8` Header
- **When**: Filter 处理请求
- **Then**: `RequestContext.getClientIp()` 返回 `"1.2.3.4"`（第一个 IP）

### AC5: clientIp 优先级正确

- **Given**: 同时存在 `X-Forwarded-For` 和 `X-Real-IP`
- **When**: Filter 处理请求
- **Then**: 优先使用 `X-Forwarded-For`，忽略 `X-Real-IP`

### AC6: clientIp 回退到 RemoteAddr

- **Given**: 请求不含任何代理 Header
- **When**: Filter 处理请求
- **Then**: `RequestContext.getClientIp()` 返回 `request.getRemoteAddr()` 的值

### AC7: ThreadLocal 正确清理

- **Given**: 请求处理完成
- **When**: Filter 的 `doFilter` 方法返回
- **Then**: `RequestContext.getRequestId()` 返回 `null`（ThreadLocal 已清理）

### AC8: 并发请求互不干扰

- **Given**: 多个线程同时处理不同请求
- **When**: 每个请求调用 `RequestContext.getRequestId()`
- **Then**: 每个线程获取到各自的 requestId，互不串扰

### AC9: 初始化失败时容错降级

- **Given**: requestId 生成过程抛出异常
- **When**: Filter 处理请求
- **Then**: 请求继续执行，`RequestContext.getRequestId()` 返回 `null`，记录 WARN 日志

### AC10: Filter 在最早时机执行

- **Given**: Spring 容器启动，有多个 Filter
- **When**: 请求进入
- **Then**: `RequestContextFilter` 在其他 Filter（如 Spring Security）之前执行

---

## 约束

### 技术约束

- 必须使用 `ThreadLocal` 实现（不使用 Java 21 Preview 特性 ScopedValue）
- 必须继承 `OncePerRequestFilter`（避免 DispatcherServlet 转发时重复执行）
- Filter Order 必须为 `HIGHEST_PRECEDENCE`
- 类必须为 `final`，不可被继承

### 实现约束

- `RequestContext` 的 `init()` 和 `clear()` 方法为包级可见，仅 Filter 调用
- 业务代码只能调用 `getRequestId()` 和 `getClientIp()` 只读方法
- 所有返回值可能为 `null`，调用方需做 null 检查

### 性能约束

- ThreadLocal 操作应在微秒级完成
- Filter 处理不应增加超过 1ms 的延迟

### 安全约束

- `X-Forwarded-For` 等 Header 可能被伪造，不应作为唯一信任源（安全场景需额外处理）
- 不应记录敏感信息到 requestId 或 clientIp

---

## 与现有代码的集成点

| 集成点 | 说明 | 状态 |
|--------|------|------|
| `ApiResponse.requestId` | 已预留字段，F02-03 后续由 ResponseBodyAdvice 填充 | F02-01 已完成 |
| `GlobalExceptionHandler` | 可使用 `RequestContext.getRequestId()` 记录日志 | F02-02 已完成 |
| cartisan-security | `SecurityContext` 将存储用户身份，与 `RequestContext` 分离 | 待开发 |

---

## 非功能需求

- **可观测性**：初始化失败需记录 WARN 日志
- **线程安全**：ThreadLocal 确保并发安全
- **内存安全**：每次请求结束必须清理 ThreadLocal，防止内存泄漏
