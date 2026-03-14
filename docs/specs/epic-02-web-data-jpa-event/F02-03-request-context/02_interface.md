# Feature: F02-03 — RequestContext 接口契约

> **注意**: 本文档为技术规范，包含伪代码描述。Java 源代码在 Phase 4 生成。

---

## 1. 类结构概览

```
com.cartisan.web.context
├── RequestContext.java        (final class, ThreadLocal 持有者)
└── RequestContextFilter.java  (extends OncePerRequestFilter, @Component)
```

---

## 2. RequestContext 类规范

### 2.1 类签名

```java
package com.cartisan.web.context;

/**
 * 请求上下文，基于 ThreadLocal 存储当前请求的追踪信息。
 *
 * <p>设计特点：</p>
 * <ul>
 *   <li>不可变：只能通过 Filter 设置，业务代码只读</li>
 *   <li>线程隔离：ThreadLocal 确保并发安全</li>
 *   <li>容错设计：各字段可能为 null（初始化失败时）</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * String requestId = RequestContext.getRequestId();
 * if (requestId != null) {
 *     log.info("Processing request: {}", requestId);
 * }
 * }</pre>
 */
public final class RequestContext {
    // 实现...
}
```

### 2.2 字段

| 访问级别 | 字段名 | 类型 | 说明 |
|---------|--------|------|------|
| `private static final` | `CONTEXT` | `ThreadLocal<RequestContext>` | 持有当前线程的上下文实例 |
| `private final` | `requestId` | `String` | 请求追踪 ID，可能为 null |
| `private final` | `clientIp` | `String` | 客户端 IP 地址，可能为 null |

### 2.3 方法规范

#### getRequestId()

```
方法签名：public static String getRequestId()

前置条件：无
后置条件：
  - 返回当前请求的 requestId
  - 若 RequestContext 未初始化，返回 null
  - 若初始化时 requestId 为 null（失败场景），返回 null

返回值：String，可能为 null
```

#### getClientIp()

```
方法签名：public static String getClientIp()

前置条件：无
后置条件：
  - 返回当前请求的 clientIp
  - 若 RequestContext 未初始化，返回 null
  - 若初始化时 clientIp 为 null（失败场景），返回 null

返回值：String，可能为 null
```

#### init() (包级可见)

```
方法签名：static void init(String requestId, String clientIp)

前置条件：无
后置条件：
  - 创建新的 RequestContext 实例并存入 ThreadLocal
  - 若已有实例，覆盖（防御性编程，正常不应发生）

参数：
  - requestId: 请求追踪 ID，可为 null
  - clientIp: 客户端 IP，可为 null

可见性：包级可见（package-private），仅 RequestContextFilter 调用
```

#### clear() (包级可见)

```
方法签名：static void clear()

前置条件：无
后置条件：
  - 从 ThreadLocal 移除 RequestContext 实例
  - ThreadLocal.get() 返回 null

可见性：包级可见（package-private），仅 RequestContextFilter 调用
```

---

## 3. RequestContextFilter 类规范

### 3.1 类签名

```java
package com.cartisan.web.context;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 请求上下文初始化 Filter。
 *
 * <p>执行顺序：HIGHEST_PRECEDENCE（最早执行）</p>
 *
 * <p>职责：</p>
 * <ol>
 *   <li>requestId：优先从 X-Request-Id Header 读取，否则生成 UUID</li>
 *   <li>clientIp：按 X-Forwarded-For → X-Real-IP → RemoteAddr 优先级</li>
 *   <li>请求结束时清理 ThreadLocal</li>
 * </ol>
 *
 * <p>容错策略：</p>
 * <ul>
 *   <li>初始化失败时使用 null 值，请求继续</li>
 *   <li>记录 WARN 日志便于排查</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {
    // 实现...
}
```

### 3.2 常量

| 常量名 | 值 | 说明 |
|--------|-----|------|
| `HEADER_REQUEST_ID` | `"X-Request-Id"` | requestId Header 名称 |
| `HEADER_X_FORWARDED_FOR` | `"X-Forwarded-For"` | XFF Header 名称 |
| `HEADER_X_REAL_IP` | `"X-Real-IP"` | Real-IP Header 名称 |

### 3.3 方法规范

#### doFilterInternal()

```
方法签名：protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
) throws ServletException, IOException

前置条件：无
后置条件：
  - RequestContext 已初始化
  - filterChain.doFilter() 已执行
  - RequestContext 已清理（finally 块）

核心流程（伪代码）：
  try {
    String requestId = extractRequestId(request)
    String clientIp = extractClientIp(request)
    RequestContext.init(requestId, clientIp)
    filterChain.doFilter(request, response)
  } catch (Exception e) {
    log.warn("RequestContext init failed, using null values", e)
    RequestContext.init(null, null)
    filterChain.doFilter(request, response)
  } finally {
    RequestContext.clear()
  }
```

#### extractRequestId()

```
方法签名：private String extractRequestId(HttpServletRequest request)

返回值：String，可能为 null

算法（伪代码）：
  header = request.getHeader(HEADER_REQUEST_ID)
  if (header != null && header.trim().length() > 0) {
    return header.trim()
  }
  return UUID.randomUUID().toString()

异常处理：
  - UUID.randomUUID() 失败时抛出 RuntimeException
  - 由 doFilterInternal 捕获并降级为 null
```

#### extractClientIp()

```
方法签名：private String extractClientIp(HttpServletRequest request)

返回值：String，可能为 null

算法（伪代码）：
  // 1. 尝试 X-Forwarded-For
  xff = request.getHeader(HEADER_X_FORWARDED_FOR)
  if (xff != null && xff.trim().length() > 0) {
    ips = xff.split(",")
    if (ips.length > 0 && ips[0].trim().length() > 0) {
      return ips[0].trim()
    }
  }

  // 2. 尝试 X-Real-IP
  realIp = request.getHeader(HEADER_X_REAL_IP)
  if (realIp != null && realIp.trim().length() > 0) {
    return realIp.trim()
  }

  // 3. 回退到 RemoteAddr
  return request.getRemoteAddr()

异常处理：
  - 若 request.getRemoteAddr() 返回 null，返回 null
  - 若解析过程抛出异常，由 doFilterInternal 捕获并降级为 null
```

#### shouldNotFilter()

```
方法签名：protected boolean shouldNotFilter(HttpServletRequest request)

返回值：false（不过滤任何请求，所有请求都需初始化 RequestContext）
```

---

## 4. 核心流程

### 4.1 requestId 处理流程

```
┌─────────────────────────────────────────────────────────┐
│  Filter 接收到请求                                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │ Header: X-Request-Id  │
         │   存在且非空白？       │
         └───────┬───────────┬───┘
                 │           │
           是    │           │   否
                 ▼           ▼
         ┌───────────┐  ┌──────────┐
         │ trim 并   │  │ 生成 UUID │
         │ 返回      │  └──────────┘
         └───────────┘
                 │
                 ▼
         RequestContext.init(requestId, ...)
```

### 4.2 clientIp 处理流程

```
┌─────────────────────────────────────────────────────────┐
│  Filter 接收到请求                                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │ Header: X-Forwarded-For│
         │   存在且非空白？       │
         └───────┬───────────┬───┘
                 │           │
           是    │           │   否
                 ▼           ▼
         ┌───────────┐  ┌─────────────────┐
         │ 取第一个 IP│  │ X-Real-IP 存在？ │
         │ (逗号分隔) │  └────┬──────┬────┘
         └─────┬─────┘       是    │      │ 否
               │                  ▼      │
               │            ┌────────┐   │
               │            │ 返回   │   │
               │            │ trim   │   │
               │            └────────┘   │
               │    ┌───────────────────┘
               │    │
               ▼    ▼
         ┌─────────────────┐
         │ getRemoteAddr() │
         └─────────────────┘
```

### 4.3 请求生命周期

```
请求进入
    │
    ▼
┌─────────────────────────────────────────────────┐
│ RequestContextFilter.doFilterInternal()         │
│  ┌─────────────────────────────────────────┐    │
│  │ 1. extractRequestId()                   │    │
│  │ 2. extractClientIp()                    │    │
│  │ 3. RequestContext.init(...)             │    │
│  │ 4. chain.doFilter() ───┐               │    │
│  └────────────────────────┼───────────────┘    │
└─────────────────────────┼──────────────────────┘
                          │
    ┌─────────────────────┼─────────────────────┐
    │                     ▼                     │
    │         ┌───────────────────────────┐     │
    │         │ Controller / Service /    │     │
    │         │ 其他业务处理               │     │
    │         │ - RequestContext.get...() │     │
    │         └───────────────────────────┘     │
    │                     │                     │
    └─────────────────────┼─────────────────────┘
                          │
    ┌─────────────────────┴─────────────────────┐
    │                                           │
    ▼                                           │
┌─────────────────────────────────────────────────┤
│ RequestContext.clear()  (finally 块)             │
└─────────────────────────────────────────────────┘
    │
    ▼
请求返回
```

---

## 5. 错误处理

### 5.1 异常场景与处理

| 场景 | 异常类型 | 处理方式 |
|------|---------|---------|
| UUID 生成失败 | RuntimeException | requestId=null，WARN 日志，请求继续 |
| XFF 解析失败 | RuntimeException | 跳过 XFF，尝试下一个来源 |
| getRemoteAddr() 返回 null | - | clientIp=null，记录 WARN 日志 |
| RequestContext 未初始化时调用 getRequestId | - | 返回 null（无异常） |

### 5.2 日志规范

```
// requestId 生成失败
log.warn("Failed to generate requestId, using null", exception)

// clientIp 提取失败
log.warn("Failed to extract clientIp from all sources, using null", exception)

// 容错兜底
log.warn("RequestContext init failed, continuing with null values", exception)
```

---

## 6. 线程安全设计

### 6.1 ThreadLocal 生命周期

```
线程池线程 T1:
  请求 A 进入 → init(reqA) → reqA 处理 → clear()
  请求 B 进入 → init(reqB) → reqB 处理 → clear()
  请求 C 进入 → init(reqC) → reqC 处理 → clear()

若 clear() 未执行:
  请求 B 可能读到请求 A 的 requestId (串扰风险)
```

### 6.2 并发安全保证

| 机制 | 说明 |
|------|------|
| ThreadLocal | 每个线程独立存储，天然并发安全 |
| OncePerRequestFilter | 确保每个请求只执行一次 |
| try-finally | 确保 clear() 一定会执行 |
| final class | 防止子类破坏不可变性 |

---

## 7. 扩展点（未来）

| 扩展点 | 说明 | 优先级 |
|--------|------|--------|
| MDC 集成 | 自动将 requestId 放入 SLFJ MDC | P1 |
| ResponseBodyAdvice | 自动填充 ApiResponse.requestId | P1 |
| 代理信任配置 | 可配置信任的代理层级和 Header | P2 |
| ScopedValue 迁移 | Java 22+ 迁移到 ScopedValue | P2 |

---

## 8. 技术决策记录

本 Feature 无需额外的技术决策。所有设计在 Epic 2 设计文档和 Phase 1 需求中已明确。
