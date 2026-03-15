# Feature: F03-08 集成测试 — 接口契约

> **Epic**: Epic 03 - Security
> **Feature**: F03-08 - 集成测试
> **版本**: v0.1
> **日期**: 2026-03-15

---

## 接口定义

本 Feature 不对外暴露新接口，而是定义测试专用接口。

### 测试用 HTTP 接口

#### GET /test/auth/require-auth

**描述**：验证 `@RequireAuth` 注解鉴权

**鉴权**：需要登录（通过注解）

**Request:** 无

**Response (成功):**
| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200 |
| data | string | 固定值 "authenticated" |

**错误码：**
| 错误码 | HTTP Status | 触发条件 |
|--------|-------------|---------|
| - | 401 | 未登录 |

---

#### GET /test/auth/require-admin

**描述**：验证 `@RequireRole("ADMIN")` 注解鉴权

**鉴权**：需要 ADMIN 角色

**Request:** 无

**Response (成功):**
| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200 |
| data | string | 固定值 "admin access" |

**错误码：**
| 错误码 | HTTP Status | 触发条件 |
|--------|-------------|---------|
| - | 401 | 未登录 |
| - | 403 | 已登录但无 ADMIN 角色 |

---

#### GET /test/auth/require-permission

**描述**：验证 `@RequirePermission("user:create")` 注解鉴权

**鉴权**：需要 user:create 权限

**Request:** 无

**Response (成功):**
| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200 |
| data | string | 固定值 "permission granted" |

**错误码：**
| 错误码 | HTTP Status | 触发条件 |
|--------|-------------|---------|
| - | 401 | 未登录 |
| - | 403 | 已登录但无 user:create 权限 |

---

#### GET /test/auth/current-user

**描述**：获取当前登录用户信息

**鉴权**：需要登录

**Request:** 无

**Response (成功):**
| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200 |
| data.userId | long | 当前用户 ID |
| data.username | string | 当前用户名 |

---

#### GET /test/tenant/current

**描述**：获取当前租户 ID

**鉴权**：无需登录

**Request:**
| Header | 类型 | 必填 | 说明 |
|--------|------|------|------|
| X-Tenant-Id | string | 否 | 租户 ID |

**Response (成功):**
| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200 |
| data.tenantId | long/null | 当前租户 ID，可能为 null |

---

#### POST /test/tenant/with-tenant

**描述**：在指定租户上下文中处理请求

**鉴权**：无需登录

**Request:**
| Header | 类型 | 必填 | 说明 |
|--------|------|------|------|
| X-Tenant-Id | string | 否 | 租户 ID |
| Body | object | 是 | 任意 JSON 对象 |

**Response (成功):**
| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200 |
| data | string | 处理结果消息 |

---

## 领域接口描述（伪代码）

### 测试工具类：SecurityTestHelpers

```java
/**
 * 集成测试工具类，提供登录、带租户 Header 请求等辅助方法
 */
public final class SecurityTestHelpers {

    /**
     * 执行已登录用户的 MockMvc 请求
     *
     * 前置条件：mvc 已注入，userId > 0
     * 后置条件：请求执行后自动登出
     * 异常：MockMvc 执行异常向上抛出
     */
    public static ResultActions performAsUser(
        MockMvc mvc,
        Long userId,
        RequestBuilder requestBuilder
    ) throws Exception;

    /**
     * 为请求添加租户 Header
     *
     * 前置条件：builder 非空
     * 后置条件：返回带 X-Tenant-Id Header 的新 builder
     * 异常：无
     */
    public static MockHttpServletRequestBuilder withTenantHeader(
        MockHttpServletRequestBuilder builder,
        Long tenantId
    );

    /**
     * 登录并在 Session 中设置租户 ID
     *
     * 前置条件：userId > 0，tenantId 可为 null
     * 后置条件：StpUtil.isLogin() = true，Session 中存在 tenantId
     * 异常：无
     */
    public static void loginWithTenant(Long userId, Long tenantId);

    /**
     * 清理登录状态
     *
     * 前置条件：无
     * 后置条件：StpUtil.isLogin() = false
     * 异常：无
     */
    public static void cleanup();
}
```

### 测试基类：AbstractSecurityIntegrationTest

```java
/**
 * 集成测试基类
 *
 * 职责：
 * - 启动完整 Spring 上下文（@SpringBootTest）
 * - 注入 MockMvc（@AutoConfigureMockMvc）
 * - 每个测试前清理 Sa-Token 登录状态
 */
@SpringBootTest(classes = IntegrationTestApplication.class)
@AutoConfigureMockMvc
public abstract class AbstractSecurityIntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUp();

    @AfterEach
    void tearDown();
}
```

### 测试启动类：IntegrationTestApplication

```java
/**
 * 集成测试专用启动类
 *
 * 职责：
 * - 扫描 cartisan-security 包：加载拦截器、Filter、自动配置
 * - 扫描 integration 包：加载测试用 Controller
 */
@SpringBootApplication(scanBasePackages = {
    "com.cartisan.security",
    "com.cartisan.security.integration"
})
public class IntegrationTestApplication {
    // 无需额外配置
}
```

---

## 核心流程（伪代码）

### 注解鉴权测试流程

```
given_noAuth_when_getRequireAuth_then_401:
    1. 确保 StpUtil.isLogin() = false
    2. mvc.perform(GET "/test/auth/require-auth")
    3. 断言 status().isUnauthorized()

given_loggedIn_when_getRequireAuth_then_200:
    1. StpUtil.login(100L)
    2. mvc.perform(GET "/test/auth/require-auth")
    3. 断言 status().isOk()
    4. 断言 body.data == "authenticated"
    5. StpUtil.logout()
```

### 租户上下文测试流程

```
given_tenantHeader_when_getCurrentTenant_then_tenantId:
    1. 构建 RequestBuilder：GET "/test/tenant/current"
    2. 添加 Header: X-Tenant-Id = 123
    3. mvc.perform(request)
    4. 断言 status().isOk()
    5. 断言 JSON path $.data.tenantId == 123

given_sessionWithTenant_when_getCurrentTenant_then_tenantId:
    1. loginWithTenant(100L, 456L)
    2. mvc.perform(GET "/test/tenant/current")
    3. 断言 status().isOk()
    4. 断言 JSON path $.data.tenantId == 456
    5. cleanup()

given_bothHeaderAndSession_when_getCurrentTenant_then_headerPriority:
    1. loginWithTenant(100L, 456L)
    2. 构建 RequestBuilder：GET "/test/tenant/current"
    3. 添加 Header: X-Tenant-Id = 789
    4. mvc.perform(request)
    5. 断言 status().isOk()
    6. 断言 JSON path $.data.tenantId == 789 (Header 优先)
    7. cleanup()
```

### 认证服务测试流程

```
given_validCredentials_when_login_then_tokenInfo:
    1. 注入 AuthenticationService
    2. TokenInfo info = service.login(100L, "alice")
    3. 断言 info.loginId == 100L
    4. 断言 info.token 非空
    5. 断言 info.expireTime > Instant.now()

given_loggedIn_when_logout_then_notLoggedIn:
    1. service.login(100L, "alice")
    2. 断言 StpUtil.isLogin() == true
    3. service.logout()
    4. 断言 StpUtil.isLogin() == false
```

---

## 数据结构

### TestAuthController 响应

```java
// 成功响应
ApiResponse<String>
{
    "code": 200,
    "data": "authenticated"
}

// 用户信息响应
ApiResponse<Map<String, Object>>
{
    "code": 200,
    "data": {
        "userId": 100,
        "username": "alice"
    }
}
```

### TestTenantController 响应

```java
// 租户信息响应
ApiResponse<Map<String, Object>>
{
    "code": 200,
    "data": {
        "tenantId": 123  // 或 null
    }
}
```

---

## 数据库变更

无（集成测试使用 Sa-Token 内存存储）

---

## 备选方案及取舍

### 方案 A：测试专用 Controller（已采纳）

**方案**：在 `src/test/java` 下创建测试专用 Controller

**优点**：
- 真实模拟 MVC 调用链
- 验证拦截器 + 注解 + 异常处理完整流程
- 不污染生产代码

**缺点**：
- 测试代码中包含 Controller

### 方案 B：独立示例模块（未采纳）

**方案**：创建 `cartisan-security-example` 模块

**优点**：
- 更贴近真实使用场景
- 可作为文档示例

**缺点**：
- 需要新建模块，复杂度高
- 增加构建和 CI 复杂度

### 方案 C：仅组件集成（未采纳）

**方案**：不涉及 HTTP 层，只测试 Filter + Context + Service

**优点**：
- 范围更聚焦

**缺点**：
- 无法验证拦截器注解的完整链路
- 不满足 Epic 验收标准

**选择理由**：方案 A 平衡了完整性和成本，最符合验收要求
