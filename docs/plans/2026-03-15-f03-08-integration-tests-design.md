# F03-08 集成测试设计文档

> **日期**：2026-03-15
> **Epic**：Epic 03 - Security
> **Feature**：F03-08 - 集成测试
> **状态**：已批准

---

## 1. 设计目标

为 cartisan-security 模块编写端到端集成测试，验证以下功能的完整链路：

- `@RequireAuth` / `@RequireRole` / `@RequirePermission` 注解鉴权
- `SecurityContext` 用户上下文读取
- `TenantContext` / `TenantContextFilter` 多租户上下文
- `AuthenticationService` 认证服务

---

## 2. 整体架构

### 2.1 目录结构

```
cartisan-security/src/test/java/com/cartisan/security/
├── integration/
│   ├── IntegrationTestApplication.java           # 测试启动类
│   ├── AbstractSecurityIntegrationTest.java      # 基类
│   ├── controller/
│   │   ├── TestAuthController.java               # 带 @RequireAuth/Role/Permission
│   │   └── TestTenantController.java             # 验证 TenantContext
│   ├── AuthAnnotationIntegrationTest.java        # 验证注解鉴权
│   ├── TenantContextIntegrationTest.java         # 验证租户上下文
│   ├── SecurityContextIntegrationTest.java       # 验证用户上下文
│   ├── AuthenticationServiceIntegrationTest.java # 验证认证服务
│   └── support/
│       └── SecurityTestHelpers.java              # 工具方法
```

### 2.2 组件关系

```
                    AbstractSecurityIntegrationTest
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
AuthAnnotationIntegrationTest  TenantContext...  SecurityContext...  AuthenticationService...
        │                         │                         │                    │
   使用 MockMvc                使用 MockMvc             使用 MockMvc        注入 Service
   调用 TestAuthController     调用 TestTenantController  + StpUtil          直接测试
```

---

## 3. 测试用 Controller

### 3.1 TestAuthController

```java
@RestController
@RequestMapping("/test/auth")
public class TestAuthController {

    @GetMapping("/require-auth")
    @RequireAuth
    public ApiResponse<String> requireAuth() {
        return ApiResponse.ok("authenticated");
    }

    @GetMapping("/require-admin")
    @RequireRole("ADMIN")
    public ApiResponse<String> requireAdmin() {
        return ApiResponse.ok("admin access");
    }

    @GetMapping("/require-permission")
    @RequirePermission("user:create")
    public ApiResponse<String> requirePermission() {
        return ApiResponse.ok("permission granted");
    }

    @GetMapping("/current-user")
    @RequireAuth
    public ApiResponse<Map<String, Object>> getCurrentUser() {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", SecurityContext.getCurrentUserId());
        result.put("username", SecurityContext.getCurrentUsername());
        return ApiResponse.ok(result);
    }
}
```

### 3.2 TestTenantController

```java
@RestController
@RequestMapping("/test/tenant")
public class TestTenantController {

    @GetMapping("/current")
    public ApiResponse<Map<String, Object>> getCurrentTenant() {
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", TenantContext.getCurrentTenantId());  // 允许 null
        return ApiResponse.ok(result);
    }

    @PostMapping("/with-tenant")
    public ApiResponse<String> withTenant(@RequestBody Map<String, Object> body) {
        Long tenantId = TenantContext.getCurrentTenantId();
        return ApiResponse.ok("processed in tenant: " + tenantId);
    }
}
```

---

## 4. 工具类设计

### 4.1 SecurityTestHelpers

```java
public final class SecurityTestHelpers {

    /**
     * 执行已登录用户的 MockMvc 请求
     */
    public static ResultActions performAsUser(MockMvc mvc, Long userId,
            RequestBuilder requestBuilder) throws Exception {
        StpUtil.login(userId);
        try {
            return mvc.perform(requestBuilder);
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 执行带租户 Header 的请求
     */
    public static MockHttpServletRequestBuilder withTenantHeader(
            MockHttpServletRequestBuilder builder, Long tenantId) {
        if (tenantId != null) {
            return builder.header("X-Tenant-Id", tenantId.toString());
        }
        return builder;
    }

    /**
     * 在 Session 中设置 tenantId
     */
    public static void loginWithTenant(Long userId, Long tenantId) {
        StpUtil.login(userId);
        StpUtil.getSession().set("tenantId", tenantId.toString());
    }

    /**
     * 清理登录状态
     */
    public static void cleanup() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }
}
```

---

## 5. 测试基类

### 5.1 IntegrationTestApplication

```java
package com.cartisan.security.integration;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 集成测试专用启动类
 * 扫描 cartisan-security 模块的所有组件 + 测试用 Controller
 */
@SpringBootApplication(scanBasePackages = {
    "com.cartisan.security",              // 模块内所有组件
    "com.cartisan.security.integration"   // 测试用 Controller
})
public class IntegrationTestApplication {
    // 无需额外配置，依赖 Spring Boot 自动装配
}
```

### 5.2 AbstractSecurityIntegrationTest

```java
@SpringBootTest(classes = IntegrationTestApplication.class)
@AutoConfigureMockMvc
public abstract class AbstractSecurityIntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 清理 Sa-Token 登录状态
        SecurityTestHelpers.cleanup();
    }

    @AfterEach
    void tearDown() {
        // TenantContext 使用 ScopedValue，请求结束后自动清理
        // 若测试中直接调用了 runWithTenant，可在此补充清理
    }
}
```

---

## 6. 测试场景

### 6.1 AuthAnnotationIntegrationTest

| 方法名 | 场景 | 预期 |
|--------|------|------|
| `given_noAuth_when_getRequireAuth_then_401` | 未登录访问 `@RequireAuth` | 401 |
| `given_loggedIn_when_getRequireAuth_then_200` | 已登录访问 `@RequireAuth` | 200 |
| `given_userWithoutRole_when_getRequireAdmin_then_403` | 无角色访问 `@RequireRole` | 403 |
| `given_userWithRole_when_getRequireAdmin_then_200` | 有角色访问 `@RequireRole` | 200 |
| `given_userWithoutPermission_when_getRequirePermission_then_403` | 无权限访问 `@RequirePermission` | 403 |
| `given_userWithPermission_when_getRequirePermission_then_200` | 有权限访问 `@RequirePermission` | 200 |

### 6.2 TenantContextIntegrationTest

| 方法名 | 场景 | 预期 |
|--------|------|------|
| `given_noTenant_when_getCurrentTenant_then_null` | 无租户信息 | `tenantId: null` |
| `given_tenantHeader_when_getCurrentTenant_then_tenantId` | Header 传租户 | `tenantId: 123` |
| `given_sessionWithTenant_when_getCurrentTenant_then_tenantId` | Session 传租户 | `tenantId: 456` |
| `given_bothHeaderAndSession_when_getCurrentTenant_then_headerPriority` | Header + Session | `tenantId: 789` (Header优先) |
| `given_tenantInFirstRequest_when_secondRequest_then_null` | 连续请求 | 第二次为 null |

### 6.3 SecurityContextIntegrationTest

| 方法名 | 场景 | 预期 |
|--------|------|------|
| `given_loggedIn_when_getCurrentUser_then_userId` | 已登录读取用户 ID | 返回 userId |
| `given_loggedWithUsername_when_getCurrentUser_then_username` | 已登录读取用户名 | 返回 username |
| `given_noAuth_when_getCurrentUser_then_exception` | 未登录读取 | 抛异常或 null |

### 6.4 AuthenticationServiceIntegrationTest

| 方法名 | 场景 | 预期 |
|--------|------|------|
| `given_validCredentials_when_login_then_tokenInfo` | `login(100L, "alice")` | 返回 TokenInfo |
| `given_loggedIn_when_isLogin_then_true` | login 后查询 | `true` |
| `given_loggedIn_when_getTokenInfo_then_info` | login 后获取 | 返回 token + loginId + expireTime |
| `given_loggedIn_when_logout_then_notLoggedIn` | login 后 logout | `isLogin() = false` |

---

## 7. 测试命名规范

遵循 `docs/skills/SKILL.md` **TEST-002** 规则：

**格式**：`given_{条件}_when_{操作}_then_{预期结果}`

- `given_` = 前置条件（未登录/已登录/无角色/有角色/无权限/有租户/无租户）
- `when_` = 操作（访问的接口或动作）
- `then_` = 预期结果（状态码或状态描述）

---

## 8. 技术约定

1. **Sa-Token 存储**：使用默认内存存储（`SaSessionDaoDefault`）
2. **TenantContext 清理**：使用 `ScopedValue`，请求结束后自动清理
3. **测试数据**：不依赖外部数据库，使用内存 Session
4. **断言库**：使用 AssertJ

---

## 9. 验收标准

- [ ] 所有测试场景覆盖 Epic Backlog F03-08 验收标准
- [ ] 测试方法命名符合 TEST-002 规范
- [ ] 测试通过率 100%
- [ ] 代码符合项目编码规范
