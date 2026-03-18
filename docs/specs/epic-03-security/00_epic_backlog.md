# Epic 03: Security — Backlog

> **Epic 目标**：构建 cartisan-security 薄抽象层 + Sa-Token 实现，提供认证授权基础设施和多租户上下文支持，底层实现可替换
>
> **依赖**：Epic 2（cartisan-web 已完成 ApiResponse、GlobalExceptionHandler、RequestContext）
>
> **复杂度**：M（Medium，跨 annotation/context/authentication/config 四个子包）

---

## Epic 目标

建立 cartisan-security 模块，提供：

1. **认证授权薄抽象层**：业务代码使用 `@RequireAuth`/`@RequireRole`/`@RequirePermission` 注解和 `SecurityContext`，不直接依赖 Sa-Token
2. **多租户基础设施**：`TenantContext` 支持从 Header 或 Token 解析租户 ID
3. **可替换性**：底层 Sa-Token 实现封装在模块内部，将来可整体替换为 Spring Security
4. **便捷参数注入**：`@CurrentUser` 注解支持在 Controller 方法中直接注入当前用户 ID

**完成标准：**
1. cartisan-security 模块可正常构建
2. 引入依赖后，注解、Context、Filter 自动生效
3. 集成测试覆盖完整链路
4. 单元测试覆盖率 ≥ 80%

---

## Feature 列表

| ID | Feature | 描述 | 复杂度 | 依赖 |
|----|---------|------|--------|------|
| **F03-01** | cartisan-security 模块骨架 | 创建模块，配置依赖（core + web + sa-token） | S | 无 |
| **F03-02** | 权限注解 + 拦截器 | `@RequireAuth`/`@RequireRole`/`@RequirePermission` + MVC 拦截器 | M | F03-01 |
| **F03-03** | SecurityContext | 当前用户上下文，代理 `StpUtil` 的只读方法 | S | F03-01 |
| **F03-04** | TenantContext | 多租户上下文，支持 Header > Token 优先级 | M | F03-01 |
| **F03-05** | TenantContextFilter | Filter 实现，兼容 Virtual Threads | M | F03-04 |
| **F03-06** | AuthenticationService | 接口 + Sa-Token 实现（login/logout/getTokenInfo） | M | F03-01 |
| **F03-07** | 自动配置 | Spring Boot AutoConfiguration + 条件装配 | M | 全部前置 |
| **F03-08** | 集成测试 | 端到端验证注解、Context、Filter | M | 全部前置 |
| **F03-09** | @CurrentUser 注解 | Controller 方法参数直接注入当前用户 ID | S | F03-01, F03-03 |

---

## 依赖关系图

```
F03-01 (模块骨架)
    |
    +--> F03-02 (权限注解 + 拦截器) -----+
    +--> F03-03 (SecurityContext) -----+ |
    +--> F03-04 (TenantContext) --+      | |
    |       |   (F03-05 Filter)   |      | |
    |                           |      | |
    +--> F03-06 (AuthenticationService) -+
    |                           |
    +--> F03-09 (@CurrentUser 注�) ------+
    |
    +--> 全部依赖 --> F03-07 (自动配置)
                        |
                        +--> F03-08 (集成测试)
```

**关键依赖说明：**
- F03-02 / F03-03 / F03-04 / F03-06 **互不依赖**，可并行开发
- F03-05 依赖 F03-04：Filter 需要操作 TenantContext
- F03-09 依赖 F03-01 和 F03-03：需要模块骨架和 SecurityContext
- F03-07 依赖全部前置：自动配置需要扫描并装配所有组件
- F03-08 最后：验证所有功能的端到端集成

---

## 推荐开发顺序

### 批次 1：基础设施（第 0.5 天）
1. **F03-01**: cartisan-security 模块骨架

### 批次 2：核心能力并行开发（第 1-3 天）
以下 Feature 可并行开发：

2. **F03-02**: 权限注解 + MVC 拦截器
3. **F03-03**: SecurityContext
4. **F03-04**: TenantContext
5. **F03-06**: AuthenticationService

### 批次 3：Filter 与参数注解（第 3.5-4 天）
6. **F03-05**: TenantContextFilter（依赖 F03-04）
7. **F03-09**: @CurrentUser 注解（依赖 F03-01, F03-03）

### 批次 4：自动配置（第 4.5 天）
8. **F03-07**: Spring Boot AutoConfiguration

### 批次 5：集成测试（第 5-5.5 天）
9. **F03-08**: 端到端集成测试

**总计预估：5.5 个工作日**

---

## 各 Feature 详细说明

### F03-01: cartisan-security 模块骨架

| 属性 | 值 |
|------|-----|
| 复杂度 | S |
| 依赖 | 无 |
| 优先级 | P0 |
| 预估工时 | 0.5d |

**描述：**
创建 cartisan-security 模块，配置 Gradle 依赖，建立包结构。

**验收标准：**
- [ ] 模块 `build.gradle.kts` 配置正确
- [ ] 依赖：cartisan-core、cartisan-web、sa-token-spring-boot3-starter
- [ ] 包结构建立：
  - `com.cartisan.security.annotation`
  - `com.cartisan.security.context`
  - `com.cartisan.security.authentication`
  - `com.cartisan.security.config`
- [ ] `./gradlew :cartisan-security:build` 成功

---

### F03-02: 权限注解 + MVC 拦截器

| 属性 | 值 |
|------|-----|
| 复杂度 | M |
| 依赖 | F03-01 |
| 优先级 | P0 |
| 预估工时 | 1.5d |

**描述：**
实现 `@RequireAuth`、`@RequireRole`、`@RequirePermission` 注解，通过 MVC 拦截器完成鉴权。

**交付物：**
- `@RequireAuth`：需登录
- `@RequireRole("admin")`：需指定角色
- `@RequirePermission("user:create")`：需指定权限
- `SecurityInterceptor`（实现 `HandlerInterceptor`）

**验收标准：**
- [ ] 三个注解可作用于 Controller 方法
- [ ] 拦截器从 `HandlerMethod` 读取注解参数
- [ ] `@RequireAuth` → `StpUtil.checkLogin()`
- [ ] `@RequireRole("xxx")` → `StpUtil.checkRole("xxx")`
- [ ] `@RequirePermission("xxx")` → `StpUtil.checkPermission("xxx")`
- [ ] 鉴权失败抛 `NotLoginException`/`NotPermissionException`，被 `GlobalExceptionHandler` 转为 401/403
- [ ] 单元测试覆盖拦截器逻辑

**技术要点：**
```java
// preHandle 伪代码
if (handler instanceof HandlerMethod) {
    HandlerMethod hm = (HandlerMethod) handler;
    if (hm.getMethodAnnotation(RequireAuth.class) != null) {
        StpUtil.checkLogin();
    }
    RequireRole rr = hm.getMethodAnnotation(RequireRole.class);
    if (rr != null) {
        StpUtil.checkRole(rr.value());
    }
    // ...
}
```

---

### F03-03: SecurityContext

| 属性 | 值 |
|------|-----|
| 复杂度 | S |
| 依赖 | F03-01 |
| 优先级 | P0 |
| 预估工时 | 0.5d |

**描述：**
当前用户上下文，提供只读方法访问当前登录用户信息。

**交付物：**
```java
public final class SecurityContext {
    public static Long getCurrentUserId();
    public static String getCurrentUsername();
    public static boolean hasRole(String role);
    public static boolean hasPermission(String permission);
    public static boolean isAuthenticated();
}
```

**验收标准：**
- [ ] 所有方法内部调用 `StpUtil` 获取信息
- [ ] 未登录时 `getCurrentUserId()` 返回 null 或抛异常
- [ ] 单元测试覆盖所有方法

**技术要点：**
- 静态工具类，内部代理 Sa-Token 的 `StpUtil`
- 业务代码通过此类访问用户信息，不直接依赖 Sa-Token

---

### F03-04: TenantContext

| 属性 | 值 |
|------|-----|
| 复杂度 | M |
| 依赖 | F03-01 |
| 优先级 | P0 |
| 预估工时 | 1d |

**描述：**
多租户上下文基础设施，支持两种数据来源（Header > Token），兼容 Virtual Threads。

**交付物：**
```java
public final class TenantContext {
    public static Long getCurrentTenantId();
    static void setCurrentTenantId(Long tenantId);  // package-private for Filter
    static void clear();  // package-private for Filter
}
```

**验收标准：**
- [ ] 存储使用 `ScopedValue`（Java 21+）或 `InheritableThreadLocal`
- [ ] 支持 Virtual Threads 环境
- [ ] `getCurrentTenantId()` 返回当前租户 ID 或 null
- [ ] 单元测试覆盖存取逻辑

**技术要点：**
- **解析优先级**：Header（X-Tenant-Id）> Token/Session
- **职责边界**：只解析并持有 tenantId，不做租户鉴权
- **Virtual Threads 兼容**：使用 `ScopedValue`（推荐）或 `ThreadLocal` + 复制策略

---

### F03-05: TenantContextFilter

| 属性 | 值 |
|------|-----|
| 复杂度 | M |
| 依赖 | F03-04 |
| 优先级 | P0 |
| 预估工时 | 1d |

**描述：**
Filter 实现租户解析逻辑，从 Header 或 Token 中提取 tenantId。

**验收标准：**
- [ ] 实现 `javax.servlet.Filter`
- [ ] 解析顺序：
  1. 优先读取 `X-Tenant-Id` Header
  2. 若 Header 不存在且已登录，从 Sa-Token Session 读取 `tenantId`
  3. 都不存在则 `TenantContext` 为空
- [ ] 请求结束时调用 `TenantContext.clear()`
- [ ] 兼容 Virtual Threads（通过 TenantContext 的存储机制）
- [ ] 单元测试 + 集成测试验证解析逻辑

**技术要点：**
```java
// 伪代码
String headerTenantId = request.getHeader("X-Tenant-Id");
if (headerTenantId != null) {
    TenantContext.setCurrentTenantId(Long.parseLong(headerTenantId));
} else if (StpUtil.isLogin()) {
    String sessionTenantId = (String) StpUtil.getSession().get("tenantId");
    // ...
}
```

---

### F03-06: AuthenticationService

| 属性 | 值 |
|------|-----|
| 复杂度 | M |
| 依赖 | F03-01 |
| 优先级 | P0 |
| 预估工时 | 1d |

**描述：**
认证服务抽象 + Sa-Token 实现，定义登录/登出/Token 信息查询接口。

**交付物：**
- `AuthenticationService` 接口
- `SaTokenAuthenticationService` 实现
- `TokenInfo` Record（定义在本模块内）

**接口定义：**
```java
public interface AuthenticationService {
    TokenInfo login(String username, String password);  // 密码校验由业务层实现
    void logout();
    TokenInfo getTokenInfo();
}

public record TokenInfo(
    String token,
    Long loginId,
    Instant expireTime
) {}
```

**验收标准：**
- [ ] 接口只定义登录/登出/Token 信息查询
- [ ] `login()` 委托业务层验证密码，成功后调用 `StpUtil.login(loginId)`
- [ ] `logout()` 调用 `StpUtil.logout()`
- [ ] `getTokenInfo()` 从 `StpUtil` 获取 token 和会话信息
- [ ] 单元测试覆盖所有方法

**技术要点：**
- **职责边界**：只管登录会话建立/销毁，不管用户查询和密码验证
- **不依赖 SecurityContext**：直接调用 `StpUtil`，两者并行
- **TokenInfo 位置**：定义在 cartisan-security 模块内

---

### F03-07: 自动配置 ✅

| 属性 | 值 |
|------|-----|
| 复杂度 | M |
| 依赖 | 全部前置 |
| 优先级 | P0 |
| 预估工时 | 1d |
| **状态** | **已完成 2026-03-15** |

**描述：**
Spring Boot AutoConfiguration，实现零配置引入 cartisan-security。

**交付物：**
- `CartisanSecurityAutoConfiguration`
- `SecurityInterceptorConfig`
- `CurrentUserMethodArgumentResolver`
- `CartisanSecurityProperties`
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**验收标准：**
- [x] 引入依赖后自动注册 `SecurityInterceptor`
- [x] 配置属性支持路径覆盖（cartisan.security.interceptor.*）
- [x] 条件装配：@ConditionalOnWebApplication + @ConditionalOnClass(StpUtil.class)
- [x] 默认 path-patterns: ["/**"], exclude-path-patterns: ["/error", "/actuator/**"]
- [x] 集成测试验证自动生效

**技术要点：**
- 使用 `@AutoConfiguration` + `@ConditionalOnClass`
- 拦截器配置通过 `WebMvcConfigurer` 注入
- **ADR-056**: 主配置 + @Import 结构
- **ADR-057**: 注入已有 Bean 而非声明新 Bean
- **ADR-058**: 配置属性使用可变 List 确保绑定兼容
- **ADR-059**: 拦截器默认应用于所有路径并排除系统路径

---

### F03-09: @CurrentUser 注解

| 属性 | 值 |
|------|-----|
| 复杂度 | S |
| 依赖 | F03-01, F03-03 |
| 优先级 | P1 |
| 预估工时 | 0.5d |

**描述：**
方法参数注解，支持在 Controller 方法中直接注入当前登录用户的 ID，无需手动调用 `SecurityContext.getCurrentUserId()`。

**交付物：**
- `@CurrentUser` 注解（`@Target(PARAMETER)`）
- `CurrentUserMethodArgumentResolver`（实现 `HandlerMethodArgumentResolver`）
- 自动配置注册 Resolver

**接口定义：**
```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}

// 使用示例
@GetMapping("/profile")
public ApiResponse<UserProfile> getProfile(@CurrentUser Long userId) {
    return ApiResponse.ok(userService.getProfile(userId));
}

// 支持 Optional
@GetMapping("/preferences")
public ApiResponse<Preferences> getPreferences(@CurrentUser Optional<Long> userId) {
    if (userId.isPresent()) {
        return ApiResponse.ok(preferencesService.getForUser(userId.get()));
    }
    return ApiResponse.ok(preferencesService.getDefault());
}
```

**验收标准：**
- [ ] `@CurrentUser Long userId` 注入成功
- [ ] 未登录时访问 `Long userId` 返回 401
- [ ] `@CurrentUser Optional<Long> userId` 支持可选登录，未登录时为 `Optional.empty()`
- [ ] 单元测试覆盖：已登录、未登录、Optional 场景
- [ ] 自动配置自动注册 Resolver

**技术要点：**
- 实现 `HandlerMethodArgumentResolver` 接口
- `supportsParameter()`：检查参数是否有 `@CurrentUser` 注解，类型为 `Long` 或 `Optional<Long>`
- `resolveArgument()`：从 `SecurityContext.getCurrentUserId()` 获取用户 ID
- 对于 `Optional<Long>` 类型，未登录时返回 `Optional.empty()`
- 对于 `Long` 类型，未登录时抛出 `NotLoginException`（被全局异常处理器转为 401）

---

### F03-08: 集成测试

| 属性 | 值 |
|------|-----|
| 复杂度 | M |
| 依赖 | 全部前置 |
| 优先级 | P0 |
| 预估工时 | 1d |

**描述：**
端到端集成测试，验证注解、Context、Filter 的完整链路。

**验收标准：**
- [ ] 测试 `@RequireAuth`：未登录返回 401
- [ ] 测试 `@RequireRole`：无角色返回 403
- [ ] 测试 `@RequirePermission`：无权限返回 403
- [ ] 测试 `SecurityContext`：可读取当前用户信息
- [ ] 测试 `TenantContext`：Header 和 Token 两种来源
- [ ] 测试 `TenantContextFilter`：请求结束后清理
- [ ] 测试 `AuthenticationService`：登录/登出/Token 信息
- [ ] 测试 `@CurrentUser Long userId`：已登录注入成功，未登录返回 401
- [ ] 测试 `@CurrentUser Optional<Long> userId`：已登录注入成功，未登录为 empty

**技术要点：**
- 使用 `@SpringBootTest` + MockMvc
- 测试类继承 `IntegrationTestBase`

---

## 技术约定

### 1. 注解设计
使用自研 `@RequireXxx` 注解，不直接暴露 Sa-Token 注解：
- `@RequireAuth`：需要登录
- `@RequireRole("admin")`：需要指定角色
- `@RequirePermission("user:create")`：需要指定权限
- `@CurrentUser`：Controller 方法参数注入当前用户 ID

### 2. 拦截器实现
使用 Spring MVC `HandlerInterceptor`，从 `HandlerMethod` 读取注解参数，调用 `StpUtil.checkXxx()`。

### 3. 参数注解实现
使用 Spring MVC `HandlerMethodArgumentResolver`，支持 `Long` 和 `Optional<Long>` 两种类型。

### 4. TenantContext 解析顺序
1. 优先读取 `X-Tenant-Id` Header
2. 若 Header 不存在且已登录，从 Sa-Token Session 读取
3. 都不存在则为 null

### 4. TokenInfo 定义
`TokenInfo` 定义在 cartisan-security 模块内，作为 `AuthenticationService` 的配套类型。

### 5. Virtual Threads 兼容
- `TenantContext` 使用 `ScopedValue`（Java 21+）
- 或使用 `InheritableThreadLocal` + 复制策略

---

## 复杂度评估标准

| 复杂度 | 代码量 | 特征 |
|--------|--------|------|
| **S** | 30-60 行 | 工具类、简单上下文、无复杂集成 |
| **M** | 80-200 行 | 涉及 Spring 集成、拦截器/Filter、中等复杂度 |

---

## 复杂度汇总

| 复杂度 | Feature 数 | 占比 |
|--------|-----------|------|
| S | 4 | 44% |
| M | 5 | 56% |
| L | 0 | 0% |

---

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Sa-Token API 变化 | 中 | 使用稳定版本（1.45.0+），封装层隔离变化 |
| Virtual Threads 兼容性 | 低 | 使用 `ScopedValue`（Java 21+）或充分测试 `ThreadLocal` 复制策略 |
| 拦截器与注解参数解析 | 低 | 参考 Sa-Token 源码中的拦截器实现 |

---

## 参考文档

- 设计文档：[cartisan-boot-设计文档.md](../../cartisan-boot-设计文档.md) - 4.4 节
- Sa-Token 官方文档：https://sa-token.cc/doc.html#/
- Epic 1 Backlog：[00_epic_backlog.md](../epic-01-core-and-test/00_epic_backlog.md)
- Epic 2 Backlog：[00_epic_backlog.md](../epic-02-web-data-jpa-event/00_epic_backlog.md)
- AI 协作 SOP：[AI协作开发SOP.md](../sop/AI协作开发SOP.md)
