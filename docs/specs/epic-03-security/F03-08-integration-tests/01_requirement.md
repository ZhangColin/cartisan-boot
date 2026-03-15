# Feature: F03-08 集成测试 — 需求规格

> **Epic**: Epic 03 - Security
> **Feature**: F03-08 - 集成测试
> **版本**: v0.1
> **日期**: 2026-03-15

---

## 背景

cartisan-security 模块已完成 F03-01 ~ F03-07 的开发，包括：
- 权限注解（`@RequireAuth`/`@RequireRole`/`@RequirePermission`）
- 拦截器（`SecurityInterceptor`）
- 上下文（`SecurityContext`/`TenantContext`/`TenantContextFilter`）
- 认证服务（`AuthenticationService`）
- 自动配置（`CartisanSecurityAutoConfiguration`）

当前已有单元测试覆盖各组件，但缺少端到端集成测试验证完整链路。

## 目标

编写集成测试，验证以下功能在真实 Spring Boot 环境中的完整工作流程：

1. 注解鉴权：`@RequireAuth` → 拦截器 → `GlobalExceptionHandler` → 401/403
2. 用户上下文：`SecurityContext` 正确读取当前登录用户信息
3. 租户上下文：`TenantContext`/`TenantContextFilter` 从 Header 或 Session 解析租户 ID
4. 认证服务：`AuthenticationService` 的登录/登出/Token 信息查询

## 范围

### 包含（In Scope）

- 在 `src/test/java` 下创建测试专用 Controller
- 使用 `@SpringBootTest` + MockMvc 进行集成测试
- 验证 HTTP → Filter → 拦截器 → Controller → 响应 的完整链路
- 使用 Sa-Token 默认内存存储（不依赖 Redis）
- 测试类按功能拆分（Auth 注解、TenantContext、SecurityContext、AuthenticationService）

### 不包含（Out of Scope）

- 分布式 Session 测试
- Redis 存储测试
- 性能测试
- 与其他模块（如 cartisan-data-jpa）的集成测试

---

## 验收标准（Acceptance Criteria）

### AC1: @RequireAuth 注解鉴权

| 场景 | 预期 |
|------|------|
| 未登录访问带 `@RequireAuth` 的接口 | 返回 401 |
| 已登录访问带 `@RequireAuth` 的接口 | 返回 200 |

### AC2: @RequireRole 注解鉴权

| 场景 | 预期 |
|------|------|
| 无指定角色访问带 `@RequireRole` 的接口 | 返回 403 |
| 有指定角色访问带 `@RequireRole` 的接口 | 返回 200 |

### AC3: @RequirePermission 注解鉴权

| 场景 | 预期 |
|------|------|
| 无指定权限访问带 `@RequirePermission` 的接口 | 返回 403 |
| 有指定权限访问带 `@RequirePermission` 的接口 | 返回 200 |

### AC4: SecurityContext 读取用户信息

| 场景 | 预期 |
|------|------|
| 已登录时调用 `SecurityContext.getCurrentUserId()` | 返回当前用户 ID |
| 已登录时调用 `SecurityContext.getCurrentUsername()` | 返回当前用户名 |
| 未登录时调用 `SecurityContext.getCurrentUserId()` | 抛异常或返回 null |

### AC5: TenantContext 从 Header 读取租户 ID

| 场景 | 预期 |
|------|------|
| 请求带 `X-Tenant-Id` Header | `TenantContext.getCurrentTenantId()` 返回 Header 中的值 |
| 请求不带 `X-Tenant-Id` Header 且未登录 | `TenantContext.getCurrentTenantId()` 返回 null |

### AC6: TenantContext 从 Session 读取租户 ID

| 场景 | 预期 |
|------|------|
| Sa-Token Session 中存在 `tenantId` | `TenantContext.getCurrentTenantId()` 返回 Session 中的值 |

### AC7: Header 优先级高于 Session

| 场景 | 预期 |
|------|------|
| 同时存在 Header 和 Session 中的 tenantId | `TenantContext.getCurrentTenantId()` 返回 Header 中的值 |

### AC8: TenantContext 请求后清理

| 场景 | 预期 |
|------|------|
| 第一个请求设置租户 ID，第二个请求不带租户信息 | 第二个请求中 `getCurrentTenantId()` 返回 null |

### AC9: AuthenticationService 登录

| 场景 | 预期 |
|------|------|
| 调用 `login(loginId, username)` | 返回 `TokenInfo`（token + loginId + expireTime） |
| 登录后调用 `isLogin()` | 返回 `true` |

### AC10: AuthenticationService Token 信息查询

| 场景 | 预期 |
|------|------|
| 登录后调用 `getTokenInfo()` | 返回包含 token、loginId、expireTime 的 `TokenInfo` |

### AC11: AuthenticationService 登出

| 场景 | 预期 |
|------|------|
| 登录后调用 `logout()`，再调用 `isLogin()` | 返回 `false` |

---

## 约束

### 测试环境

- Java 21
- Spring Boot 3.4.x
- JUnit 5 + AssertJ
- MockMvc（`@AutoConfigureMockMvc`）

### 测试命名规范

遵循项目 `docs/skills/SKILL.md` 中的 **TEST-002** 规则：

```
given_{条件}_when_{操作}_then_{预期结果}
```

### 依赖

- 依赖 F03-01 ~ F03-07 全部完成
- cartisan-test 模块提供 `IntegrationTestBase`/`ApiTestBase`
- cartisan-web 模块提供 `ApiResponse`、`GlobalExceptionHandler`

---

## 非功能需求

- 测试执行时间：单个测试类 < 10 秒
- 测试稳定性：无并发问题，可重复运行
- 测试独立性：每个测试方法独立，不依赖执行顺序
