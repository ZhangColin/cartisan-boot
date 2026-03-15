# F03-08 集成测试 — 测试策略与用例清单

## 测试目标

验证 cartisan-security 模块的核心功能在 Web 环境中的端到端行为，确保：
- 注解鉴权 (`@RequireAuth`/`@RequireRole`/`@RequirePermission`) 正确工作
- 租户上下文 (`TenantContext`) 从 Header/Session 正确解析
- 安全上下文 (`SecurityContext`) 正确读取用户信息
- 认证服务 (`AuthenticationService`) 的 token 管理功能正常

## 测试范围

### 包含 (In Scope)
- Spring Boot `@SpringBootTest` 集成测试
- MockMvc 模拟 HTTP 请求/响应
- Sa-Token Session 存储的集成测试
- TenantContext Filter 的端到端验证
- 测试专用 Controller 用于测试辅助

### 不包含 (Out Scope)
- 单元测试（已由 `*Test.java` 覆盖）
- 性能测试
- 压力测试
- 安全漏洞扫描

## 测试策略

### 1. 测试分层

```
┌─────────────────────────────────────────────────────┐
│                 集成测试层                          │
│  (Spring Boot Test + MockMvc + Sa-Token)           │
├─────────────────────────────────────────────────────┤
│  TenantContextIntegrationTest                       │
│  AuthAnnotationIntegrationTest                      │
│  SecurityContextIntegrationTest                    │
│  AuthenticationServiceIntegrationTest                 │
├─────────────────────────────────────────────────────┤
│            AbstractSecurityIntegrationTest          │
│  (基类：MockMvc + ObjectMapper + SaToken配置)        │
├─────────────────────────────────────────────────────┤
│  TestAuthController / TestTenantController          │
│  (测试专用 Controller：提供登录/查询端点)             │
└─────────────────────────────────────────────────────┘
```

### 2. 测试环境配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 上下文 | `IntegrationTestApplication` | 测试专用启动类 |
| Sa-Token 存储 | 内存存储 | 测试隔离，无外部依赖 |
| MockMvc | 启用 | 模拟 HTTP 请求/响应 |
| 清理策略 | `@BeforeEach` 清理登录状态 | 测试隔离 |

### 3. 测试命名规范

遵循 `docs/skills/SKILL.md` 中的 **TEST-002**：
```
given_{条件}_when_{操作}_then_{预期结果}
```

## 验收标准覆盖

| AC | 描述 | 测试类 | 测试方法 |
|----|------|--------|----------|
| AC1 | 未登录访问 @RequireAuth 返回 401 | AuthAnnotationIntegrationTest | `given_noAuth_when_getRequireAuth_then_401` |
| AC2 | 已登录访问 @RequireAuth 返回 200 | AuthAnnotationIntegrationTest | `given_loggedIn_when_getRequireAuth_then_200` |
| AC3 | 无 ADMIN 角色访问 @RequireRole 返回 403 | AuthAnnotationIntegrationTest | `given_userWithoutRole_when_getRequireAdmin_then_403` |
| AC4 | 有 ADMIN 角色访问 @RequireRole 返回 200 | AuthAnnotationIntegrationTest | `given_userWithRole_when_getRequireAdmin_then_200` |
| AC5 | 无 user:create 权限访问 @RequirePermission 返回 403 | AuthAnnotationIntegrationTest | `given_userWithoutPermission_when_getRequirePermission_then_403` |
| AC6 | 有 user:create 权限访问 @RequirePermission 返回 200 | AuthAnnotationIntegrationTest | `given_userWithPermission_when_getRequirePermission_then_200` |
| AC7 | 无租户信息时 `TenantContext.getCurrentTenantId()` 返回 null | TenantContextIntegrationTest | `given_noTenant_when_getCurrentTenant_then_null` |
| AC8 | Header 传租户 ID 时正确解析 | TenantContextIntegrationTest | `given_tenantHeader_when_getCurrentTenant_then_tenantId` |
| AC9 | Session 中有租户 ID 时正确解析 | TenantContextIntegrationTest | `given_sessionWithTenant_when_getCurrentTenant_then_tenantId` |
| AC10 | Header 优先级高于 Session | TenantContextIntegrationTest | `given_bothHeaderAndSession_when_getCurrentTenant_then_headerPriority` |
| AC11 | 请求结束后租户上下文清理 | TenantContextIntegrationTest | `given_tenantInFirstRequest_when_secondRequest_then_null` |

## 最终用例清单

### AuthAnnotationIntegrationTest (6 个测试)

| # | 测试方法 | 验证点 |
|---|----------|--------|
| 1 | `given_noAuth_when_getRequireAuth_then_401` | 未登录返回 401 |
| 2 | `given_loggedIn_when_getRequireAuth_then_200` | 已登录返回 200 |
| 3 | `given_userWithoutRole_when_getRequireAdmin_then_403` | 无角色返回 403 |
| 4 | `given_userWithRole_when_getRequireAdmin_then_200` | 有角色返回 200 |
| 5 | `given_userWithoutPermission_when_getRequirePermission_then_403` | 无权限返回 403 |
| 6 | `given_userWithPermission_when_getRequirePermission_then_200` | 有权限返回 200 |

### TenantContextIntegrationTest (5 个测试)

| # | 测试方法 | 验证点 |
|---|----------|--------|
| 1 | `given_noTenant_when_getCurrentTenant_then_null` | 无租户返回 null |
| 2 | `given_tenantHeader_when_getCurrentTenant_then_tenantId` | Header 解析正确 |
| 3 | `given_sessionWithTenant_when_getCurrentTenant_then_tenantId` | Session 解析正确 |
| 4 | `given_bothHeaderAndSession_when_getCurrentTenant_then_headerPriority` | Header 优先级高 |
| 5 | `given_tenantInFirstRequest_when_secondRequest_then_null` | 上下文清理正确 |

### SecurityContextIntegrationTest (3 个测试)

| # | 测试方法 | 验证点 |
|---|----------|--------|
| 1 | `given_notLoggedIn_when_getCurrentUser_then_401` | 未登录返回 401 |
| 2 | `given_loggedIn_when_getCurrentUser_then_userInfo` | 已登录返回用户信息 |
| 3 | `given_loggedOut_when_getCurrentUser_then_401` | 登出后再访问返回 401 |

### AuthenticationServiceIntegrationTest (4 个测试)

| # | 测试方法 | 验证点 |
|---|----------|--------|
| 1 | `given_loginId_when_login_then_returnToken` | 登录返回有效 token |
| 2 | `given_loggedIn_when_requestWithToken_then_success` | Token 可访问受保护接口 |
| 3 | `given_validToken_when_getUserInfo_then_success` | 有效 token 查询用户信息 |
| 4 | `given_invalidToken_when_getUserInfo_then_401` | 无效 token 返回 401 |

## 测试基础设施

### 测试启动类
`IntegrationTestApplication` - 扫描 `com.cartisan.security` 和测试 Controller

### 测试基类
`AbstractSecurityIntegrationTest` - 提供 MockMvc、ObjectMapper、extractToken()

### 测试配置
`SaTokenTestConfig` - 初始化 Sa-Token 上下文（`SaTokenContextForThreadLocal`）

### 测试辅助
`SecurityTestHelpers.cleanup()` - 清理登录状态

### 测试 Controller
- `TestAuthController` - 登录/查询用户信息端点
- `TestTenantController` - 登录并设置租户/查询当前租户端点

### StpInterface 实现
`TestStpInterface` - 从 Session 读取角色/权限列表

## 关键修复

### TenantContextFilter 测试模式增强

**问题**：MockMvc 环境下 SaServletFilter 未执行，`StpUtil.isLogin()` 抛出 `SaTokenContextException`

**解决方案**：增加测试模式降级逻辑
```java
// 正常模式：Sa-Token 上下文已初始化
if (StpUtil.isLogin()) {
    return StpUtil.getSession().get(TENANT_ID_SESSION_KEY);
}

// 测试模式：通过 token 手动查询 Session
String token = extractSaToken(request);
Object loginId = StpUtil.getLoginIdByToken(token);
return StpUtil.getSessionByLoginId(loginId).get(TENANT_ID_SESSION_KEY);
```

## 交叉审查

**审查人/模型**：superpowers:code-reviewer subagent
**审查范围**：Spec (01/02/03) + 代码变更 (407fa22...2ac7930)
**结论**：通过
**待办**：
- ✅ 代码重复已修复（extractToken 提取到基类）
- ✅ 断言已修复（验证确切值而非类型）

## PIT 变异测试

**状态**：不适用
**原因**：cartisan-security 模块未配置 PIT（集成测试为主，单元测试已覆盖核心逻辑）
