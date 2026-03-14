# Feature: TenantContextFilter

## 背景

F03-04 已提供 `TenantContext` 存储机制，但需要 Filter 在请求进入时自动解析租户 ID 并设置到上下文中，请求结束时自动清理。这是多租户系统的"最后一公里"，确保后续代码（如 SecurityInterceptor、Repository）可以透明访问当前租户信息。

## 目标

- 实现 HTTP Filter，从请求中解析租户 ID 并设置到 `TenantContext`
- 支持 Header 和 Sa-Token Session 两种数据来源（Header 优先）
- 兼容 Java 21+ Virtual Threads 环境
- 请求结束后确保清理上下文，防止线程复用导致的租户串扰

## 范围

### 包含（In Scope）

- `TenantContextFilter` 类，实现 `jakarta.servlet.Filter`
- 从 `X-Tenant-Id` Header 解析租户 ID
- Header 不存在时，从 Sa-Token Session 读取 `tenantId`（用户已登录场景）
- 请求结束后调用 `TenantContext.clear()`
- DEBUG 级别日志记录解析结果
- WARN 级别日志记录格式错误
- 执行顺序：最高优先级，确保在 SecurityInterceptor 之前执行

### 不包含（Out of Scope）

- 租户 ID 的业务校验（如验证租户是否存在、是否有效）—— 由业务层实现
- 租户级数据隔离 —— 由 Repository 层或 AOP 实现
- `@RequireTenant` 注解 —— 后续 Feature 可扩展
- Token 中租户信息的写入 —— 由 `AuthenticationService.login()` 实现

## 验收标准（Acceptance Criteria）

### 主流程

- **AC1**: 请求携带 `X-Tenant-Id: 123` 时，`TenantContext.getCurrentTenantId()` 返回 `123L`
- **AC2**: Header 不存在且用户已登录（Session 中有 `tenantId`），从 Session 读取租户 ID
- **AC3**: Header 和 Session 都不存在时，`TenantContext.getCurrentTenantId()` 返回 `null`

### 边界场景

- **AC4**: Header 和 Session 都存在时，**Header 优先**（Header 值生效）
- **AC5**: Header 为空字符串（`X-Tenant-Id: `）时，视为不存在，继续从 Session 读取
- **AC6**: Header 格式错误（非数字如 `abc`）时，记录 WARN 日志，忽略该 Header，继续从 Session 读取

### 异常处理

- **AC7**: 请求处理过程中抛出异常，`TenantContext.clear()` 仍在 `finally` 块中执行
- **AC8**: Filter 不应该抛出任何未捕获异常影响请求链（格式错误应记录日志并忽略）

### Virtual Threads 兼容

- **AC9**: 在 Java 21+ Virtual Threads 环境下正常工作（依赖 F03-04 的 `TenantContext` 实现）

### 日志

- **AC10**: 解析成功时记录 DEBUG 日志：`Resolved tenantId: 123 from [header|session]`
- **AC11**: Header 格式错误时记录 WARN 日志：`Invalid X-Tenant-Id header value: abc`

### 执行顺序

- **AC12**: Filter 执行顺序优先于 SecurityInterceptor（使用 `@Order(Ordered.HIGHEST_PRECEDENCE + 10)`）

## 约束

### 技术约束

- **依赖 F03-04**：`TenantContext` 类已实现并可用
- **依赖 Sa-Token**：需要调用 `StpUtil.isLogin()` 和 `StpUtil.getSession().get("tenantId")`
- **Servlet API**：实现 `jakarta.servlet.Filter`（Spring Boot 3.x）

### 设计约束

- **单一职责**：只负责解析和设置，不做业务校验
- **宽容接收**：格式错误不阻塞请求，记录日志后继续
- **性能**：Filter 逻辑应极简，不涉及数据库或远程调用

## 技术决策

### 决策 1：Header 格式错误的处理策略

**决策**：忽略 Header，记录 WARN 日志，继续处理请求。

**理由**：
1. Filter 的职责是"尽力解析"，不是"强制校验"
2. 格式错误可能是客户端 bug，不应阻塞业务流程
3. 记录日志便于运维发现问题
4. 如需强制校验，可在业务层实现 `@RequireTenant` 注解

### 决策 2：Token 中租户信息的存储位置

**决策**：使用固定 key `"tenantId"`。

**理由**：
1. YAGNI 原则，当前无可配置需求
2. 保持简单，减少配置复杂度
3. 后续如需扩展，可在 `AuthenticationService` 层面增加配置属性

### 决策 3：日志级别

**决策**：
- 解析成功：DEBUG 级别（生产环境通常不开启，不影响性能）
- 格式错误：WARN 级别（运维需要感知）

### 决策 4：Filter 执行顺序

**决策**：使用 `@Order(Ordered.HIGHEST_PRECEDENCE + 10)`。

**理由**：
1. 必须在 SecurityInterceptor（F03-02）之前执行
2. SecurityInterceptor 可能需要读取 `TenantContext` 做租户级权限校验
3. 保留一些前置位置给其他可能的基础设施 Filter（如链路追踪）

## 伪代码（核心逻辑）

```java
@Override
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
    try {
        Long tenantId = resolveTenantId((HttpServletRequest) request);
        if (tenantId != null) {
            TenantContext.setCurrentTenantId(tenantId);
        }
        chain.doFilter(request, response);
    } finally {
        TenantContext.clear();
    }
}

private Long resolveTenantId(HttpServletRequest request) {
    // 1. 优先从 Header 读取
    String headerValue = request.getHeader("X-Tenant-Id");
    if (headerValue != null && !headerValue.isBlank()) {
        try {
            Long tenantId = Long.parseLong(headerValue.trim());
            log.debug("Resolved tenantId: {} from header", tenantId);
            return tenantId;
        } catch (NumberFormatException e) {
            log.warn("Invalid X-Tenant-Id header value: {}", headerValue);
        }
    }

    // 2. Header 不存在或无效，从 Session 读取
    if (StpUtil.isLogin()) {
        Object sessionTenantId = StpUtil.getSession().get("tenantId");
        if (sessionTenantId != null) {
            Long tenantId = Long.parseLong(sessionTenantId.toString());
            log.debug("Resolved tenantId: {} from session", tenantId);
            return tenantId;
        }
    }

    return null;
}
```

## 风险评估

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| Header 格式错误导致解析失败 | 中 | 低 | 记录 WARN 日志，降级到 Session 读取 |
| Session 中 tenantId 格式异常 | 低 | 低 | try-catch 包裹，记录日志，返回 null |
| Virtual Threads 线程复用导致租户串扰 | 低 | 高 | 确保 `finally` 块调用 `clear()` |
| Filter 顺序错误导致 SecurityInterceptor 无法获取租户 | 低 | 中 | 使用 `@Order` 确保优先执行 |

## 参考

- Epic Backlog: `docs/specs/epic-03-security/00_epic_backlog.md`
- F03-04 TenantContext: `docs/specs/epic-03-security/F03-04-TenantContext/`
- Sa-Token 文档: https://sa-token.cc/doc.html#/
