# Feature: F03-08 集成测试 — 实施计划

> **Epic**: Epic 03 - Security
> **Feature**: F03-08 - 集成测试
> **版本**: v0.1
> **日期**: 2026-03-15

---

## 目标复述

为 cartisan-security 模块编写端到端集成测试，验证：
1. `@RequireAuth`/`@RequireRole`/`@RequirePermission` 注解鉴权（401/403）
2. `SecurityContext` 正确读取当前登录用户信息
3. `TenantContext`/`TenantContextFilter` 从 Header 或 Session 解析租户 ID，Header 优先
4. `AuthenticationService` 的登录/登出/Token 信息查询

使用 `@SpringBootTest` + MockMvc，在 `src/test/java` 下创建测试专用 Controller。

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 创建 | `cartisan-security/src/test/java/com/cartisan/security/integration/IntegrationTestApplication.java` | 测试启动类 |
| 创建 | `cartisan-security/src/test/java/com/cartisan/security/integration/AbstractSecurityIntegrationTest.java` | 测试基类 |
| 创建 | `cartisan-security/src/test/java/com/cartisan/security/integration/controller/TestAuthController.java` | 测试用 Controller（鉴权） |
| 创建 | `cartisan-security/src/test/java/com/cartisan/security/integration/controller/TestTenantController.java` | 测试用 Controller（租户） |
| 创建 | `cartisan-security/src/test/java/com/cartisan/security/integration/support/SecurityTestHelpers.java` | 测试工具类 |
| 创建 | `cartisan-security/src/test/java/com/cartisan/security/integration/AuthAnnotationIntegrationTest.java` | 注解鉴权测试 |
| 创建 | `cartisan-security/src/test/java/com/cartisan/security/integration/TenantContextIntegrationTest.java` | 租户上下文测试 |
| 创建 | `cartisan-security/src/test/java/com/cartisan/security/integration/SecurityContextIntegrationTest.java` | 用户上下文测试 |
| 创建 | `cartisan-security/src/test/java/com/cartisan/security/integration/AuthenticationServiceIntegrationTest.java` | 认证服务测试 |

---

## 核心流程（伪代码）

```
1. 创建测试启动类 IntegrationTestApplication
   - @SpringBootApplication 扫描 security 包 + integration 包

2. 创建测试基类 AbstractSecurityIntegrationTest
   - @SpringBootTest + @AutoConfigureMockMvc
   - @BeforeEach 清理登录状态

3. 创建测试用 Controller
   - TestAuthController: 带 @RequireAuth/Role/Permission 的接口
   - TestTenantController: 验证 TenantContext 的接口

4. 创建测试工具类 SecurityTestHelpers
   - performAsUser(): 已登录请求
   - withTenantHeader(): 添加租户 Header
   - loginWithTenant(): Session 设置租户
   - cleanup(): 清理登录状态

5. 编写集成测试（按顺序）
   - AuthAnnotationIntegrationTest: 6 个测试方法
   - TenantContextIntegrationTest: 5 个测试方法
   - SecurityContextIntegrationTest: 3 个测试方法
   - AuthenticationServiceIntegrationTest: 4 个测试方法
```

---

## 原子任务清单

### Step 1: 创建测试基础设施

**任务 1.1: 创建 IntegrationTestApplication.java**

- 文件：`cartisan-security/src/test/java/com/cartisan/security/integration/IntegrationTestApplication.java`
- 内容：
  ```java
  @SpringBootApplication(scanBasePackages = {
      "com.cartisan.security",
      "com.cartisan.security.integration"
  })
  public class IntegrationTestApplication {}
  ```
- 验证：编译通过

---

**任务 1.2: 创建 AbstractSecurityIntegrationTest.java**

- 文件：`cartisan-security/src/test/java/com/cartisan/security/integration/AbstractSecurityIntegrationTest.java`
- 内容：
  - `@SpringBootTest(classes = IntegrationTestApplication.class)`
  - `@AutoConfigureMockMvc`
  - 注入 `MockMvc mvc`
  - 注入 `ObjectMapper objectMapper`
  - `@BeforeEach setUp()` 调用 `SecurityTestHelpers.cleanup()`
  - `@AfterEach tearDown()` 留空（带注释说明 ScopedValue 自动清理）
- 验证：编译通过（暂时注释掉 cleanup 调用，因为 SecurityTestHelpers 还不存在）

---

**任务 1.3: 创建 SecurityTestHelpers.java**

- 文件：`cartisan-security/src/test/java/com/cartisan/security/integration/support/SecurityTestHelpers.java`
- 内容：
  - `performAsUser(MockMvc, Long userId, RequestBuilder)` → ResultActions
  - `withTenantHeader(MockHttpServletRequestBuilder, Long tenantId)` → MockHttpServletRequestBuilder
  - `loginWithTenant(Long userId, Long tenantId)` → void
  - `cleanup()` → void
- 验证：编译通过

---

**任务 1.4: 更新 AbstractSecurityIntegrationTest 启用 cleanup**

- 文件：`cartisan-security/src/test/java/com/cartisan/security/integration/AbstractSecurityIntegrationTest.java`
- 内容：取消 `@BeforeEach setUp()` 中对 `SecurityTestHelpers.cleanup()` 的注释
- 验证：编译通过

---

### Step 2: 创建测试用 Controller

**任务 2.1: 创建 TestAuthController.java**

- 文件：`cartisan-security/src/test/java/com/cartisan/security/integration/controller/TestAuthController.java`
- 内容：
  - `@RestController @RequestMapping("/test/auth")`
  - `GET /require-auth` → `@RequireAuth` → 返回 "authenticated"
  - `GET /require-admin` → `@RequireRole("ADMIN")` → 返回 "admin access"
  - `GET /require-permission` → `@RequirePermission("user:create")` → 返回 "permission granted"
  - `GET /current-user` → `@RequireAuth` → 返回 Map(userId, username)
- 验证：编译通过

---

**任务 2.2: 创建 TestTenantController.java**

- 文件：`cartisan-security/src/test/java/com/cartisan/security/integration/controller/TestTenantController.java`
- 内容：
  - `@RestController @RequestMapping("/test/tenant")`
  - `GET /current` → 返回 Map(tenantId)，使用 HashMap 允许 null
  - `POST /with-tenant` → 返回 "processed in tenant: {tenantId}"
- 验证：编译通过

---

### Step 3: 编写 AuthAnnotationIntegrationTest（红灯 + 绿灯）

**任务 3.1: 编写 given_noAuth_when_getRequireAuth_then_401（红灯）**

- 文件：`cartisan-security/src/test/java/com/cartisan/security/integration/AuthAnnotationIntegrationTest.java`
- 内容：
  - 继承 `AbstractSecurityIntegrationTest`
  - 测试方法：`given_noAuth_when_getRequireAuth_then_401()`
  - 测试逻辑：`mvc.perform(get("/test/auth/require-auth")).andExpect(status().isUnauthorized())`
- 验证：编译通过 + 测试红灯

---

**任务 3.2: 实现使 given_noAuth_when_getRequireAuth_then_401 通过（绿灯）**

- 文件：无需修改（依赖已有组件）
- 验证：测试绿灯（`@RequireAuth` 拦截器已工作）

---

**任务 3.3: 编写 given_loggedIn_when_getRequireAuth_then_200（红灯）**

- 文件：`AuthAnnotationIntegrationTest.java`
- 内容：
  - 测试方法：`given_loggedIn_when_getRequireAuth_then_200()`
  - 测试逻辑：先 `StpUtil.login(100L)`，发请求，断言 200，最后 `logout()`
- 验证：编译通过 + 测试绿灯

---

**任务 3.4: 编写 given_userWithoutRole_when_getRequireAdmin_then_403（红灯）**

- 文件：`AuthAnnotationIntegrationTest.java`
- 内容：
  - 测试方法：`given_userWithoutRole_when_getRequireAdmin_then_403()`
  - 测试逻辑：login(100L)，发请求，断言 403，logout
- 验证：编译通过 + 测试绿灯

---

**任务 3.5: 编写 given_userWithRole_when_getRequireAdmin_then_200（红灯）**

- 文件：`AuthAnnotationIntegrationTest.java`
- 内容：
  - 测试方法：`given_userWithRole_when_getRequireAdmin_then_200()`
  - 测试逻辑：login(100L) 并设置角色，发请求，断言 200，logout
- 验证：编译通过 + 测试绿灯

---

**任务 3.6: 编写 given_userWithoutPermission_when_getRequirePermission_then_403（红灯）**

- 文件：`AuthAnnotationIntegrationTest.java`
- 内容：
  - 测试方法：`given_userWithoutPermission_when_getRequirePermission_then_403()`
  - 测试逻辑：login(100L)，发请求，断言 403，logout
- 验证：编译通过 + 测试绿灯

---

**任务 3.7: 编写 given_userWithPermission_when_getRequirePermission_then_200（红灯）**

- 文件：`AuthAnnotationIntegrationTest.java`
- 内容：
  - 测试方法：`given_userWithPermission_when_getRequirePermission_then_200()`
  - 测试逻辑：login(100L) 并设置权限，发请求，断言 200，logout
- 验证：编译通过 + 测试绿灯

---

### Step 4: 编写 TenantContextIntegrationTest

**任务 4.1: 编写 given_noTenant_when_getCurrentTenant_then_null（红灯）**

- 文件：`cartisan-security/src/test/java/com/cartisan/security/integration/TenantContextIntegrationTest.java`
- 内容：
  - 继承 `AbstractSecurityIntegrationTest`
  - 测试方法：`given_noTenant_when_getCurrentTenant_then_null()`
  - 测试逻辑：`mvc.perform(get("/test/tenant/current"))`，断言 200 + `$.data.tenantId` 为 null
- 验证：编译通过 + 测试绿灯

---

**任务 4.2: 编写 given_tenantHeader_when_getCurrentTenant_then_tenantId（红灯）**

- 文件：`TenantContextIntegrationTest.java`
- 内容：
  - 测试方法：`given_tenantHeader_when_getCurrentTenant_then_tenantId()`
  - 测试逻辑：使用 `SecurityTestHelpers.withTenantHeader()` 添加 Header，断言 200 + `$.data.tenantId == 123`
- 验证：编译通过 + 测试绿灯

---

**任务 4.3: 编写 given_sessionWithTenant_when_getCurrentTenant_then_tenantId（红灯）**

- 文件：`TenantContextIntegrationTest.java`
- 内容：
  - 测试方法：`given_sessionWithTenant_when_getCurrentTenant_then_tenantId()`
  - 测试逻辑：使用 `SecurityTestHelpers.loginWithTenant(100L, 456L)`，发请求，断言 `$.data.tenantId == 456`
- 验证：编译通过 + 测试绿灯

---

**任务 4.4: 编写 given_bothHeaderAndSession_when_getCurrentTenant_then_headerPriority（红灯）**

- 文件：`TenantContextIntegrationTest.java`
- 内容：
  - 测试方法：`given_bothHeaderAndSession_when_getCurrentTenant_then_headerPriority()`
  - 测试逻辑：`loginWithTenant(100L, 456L)`，Header 设置 789，断言 `$.data.tenantId == 789`
- 验证：编译通过 + 测试绿灯

---

**任务 4.5: 编写 given_tenantInFirstRequest_when_secondRequest_then_null（红灯）**

- 文件：`TenantContextIntegrationTest.java`
- 内容：
  - 测试方法：`given_tenantInFirstRequest_when_secondRequest_then_null()`
  - 测试逻辑：
    1. 第一个请求带 Header 123
    2. 第二个请求不带 Header
    3. 断言第二个请求的 `$.data.tenantId` 为 null
- 验证：编译通过 + 测试绿灯

---

### Step 5: 编写 SecurityContextIntegrationTest

**任务 5.1: 编写 given_loggedIn_when_getCurrentUser_then_userId（红灯）**

- 文件：`cartisan-security/src/test/java/com/cartisan/security/integration/SecurityContextIntegrationTest.java`
- 内容：
  - 继承 `AbstractSecurityIntegrationTest`
  - 测试方法：`given_loggedIn_when_getCurrentUser_then_userId()`
  - 测试逻辑：login(100L)，调用 `/test/auth/current-user`，断言 `$.data.userId == 100`
- 验证：编译通过 + 测试绿灯

---

**任务 5.2: 编写 given_loggedWithUsername_when_getCurrentUser_then_username（红灯）**

- 文件：`SecurityContextIntegrationTest.java`
- 内容：
  - 测试方法：`given_loggedWithUsername_when_getCurrentUser_then_username()`
  - 测试逻辑：login 并在 Session 设置 username "alice"，调用接口，断言 `$.data.username == "alice"`
- 验证：编译通过 + 测试绿灯

---

**任务 5.3: 编写 given_noAuth_when_getCurrentUser_then_exception（红灯）**

- 文件：`SecurityContextIntegrationTest.java`
- 内容：
  - 测试方法：`given_noAuth_when_getCurrentUser_then_exception()`
  - 测试逻辑：未登录调用 `/test/auth/current-user`，断言 401
- 验证：编译通过 + 测试绿灯

---

### Step 6: 编写 AuthenticationServiceIntegrationTest

**任务 6.1: 编写 given_validCredentials_when_login_then_tokenInfo（红灯）**

- 文件：`cartisan-security/src/test/java/com/cartisan/security/integration/AuthenticationServiceIntegrationTest.java`
- 内容：
  - 继承 `AbstractSecurityIntegrationTest`
  - 注入 `AuthenticationService`
  - 测试方法：`given_validCredentials_when_login_then_tokenInfo()`
  - 测试逻辑：`service.login(100L, "alice")`，断言 tokenInfo 各字段非空
- 验证：编译通过 + 测试绿灯

---

**任务 6.2: 编写 given_loggedIn_when_isLogin_then_true（红灯）**

- 文件：`AuthenticationServiceIntegrationTest.java`
- 内容：
  - 测试方法：`given_loggedIn_when_isLogin_then_true()`
  - 测试逻辑：login 后断言 `StpUtil.isLogin() == true`
- 验证：编译通过 + 测试绿灯

---

**任务 6.3: 编写 given_loggedIn_when_getTokenInfo_then_info（红灯）**

- 文件：`AuthenticationServiceIntegrationTest.java`
- 内容：
  - 测试方法：`given_loggedIn_when_getTokenInfo_then_info()`
  - 测试逻辑：login 后 `service.getTokenInfo()`，断言返回值
- 验证：编译通过 + 测试绿灯

---

**任务 6.4: 编写 given_loggedIn_when_logout_then_notLoggedIn（红灯）**

- 文件：`AuthenticationServiceIntegrationTest.java`
- 内容：
  - 测试方法：`given_loggedIn_when_logout_then_notLoggedIn()`
  - 测试逻辑：login 后 `service.logout()`，断言 `StpUtil.isLogin() == false`
- 验证：编译通过 + 测试绿灯

---

### Step 7: 全量验证与提交

**任务 7.1: 运行全部集成测试**

- 命令：`./gradlew :cartisan-security:test --tests "*IntegrationTest"`
- 验证：所有测试绿灯

---

**任务 7.2: 提交代码**

- 命令：
  ```bash
  git add cartisan-security/src/test/java/com/cartisan/security/integration/
  git commit -m "feat(security): F03-08 add integration tests for security module"
  ```

---

## 验证门禁

- [ ] 第一个 Step 是"创建测试基础设施"
- [ ] 测试类和实现类按 Step 分开（由于本 Feature 是集成测试，依赖已有实现，大部分测试直接绿灯）
- [ ] 每个 Step 在 150 行以内
- [ ] 所有 AC 有对应的测试覆盖（AC1-AC11 对应 18 个测试方法）

---

## 备注

- 本 Feature 的测试大部分会直接绿灯，因为被测试的组件（拦截器、Filter、服务）已在 F03-01 ~ F03-07 中实现
- Sa-Token 角色和权限的设置方式需参考 Sa-Token 文档（`StpUtil.getSession().setRoleList()` / `setPermissionList()`）
- 测试执行需要 Docker 可用（如果启用了 Testcontainers），但本 Feature 使用 Sa-Token 内存存储，不依赖 Docker
