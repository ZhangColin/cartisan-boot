# Feature: F03-02 权限注解 + MVC 拦截器 — 接口契约

## 注解定义（伪代码）

### @RequireAuth

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {
    /**
     * 是否必须登录，默认 true
     * 预留扩展：支持 @RequireAuth(false) 作为类级别注解的覆盖
     */
    boolean value() default true;
}
```

### @RequireRole

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /**
     * 角色标识列表，满足任一即可（OR 逻辑）
     * 示例：@RequireRole({"admin", "super"})
     */
    String[] value();
}
```

### @RequirePermission

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /**
     * 权限标识列表，满足任一即可（OR 逻辑）
     * 示例：@RequirePermission({"user:create", "user:update"})
     */
    String[] value();
}
```

---

## 拦截器接口描述（伪代码）

### 组件：SecurityInterceptor

**包路径**：`com.cartisan.security.config.SecurityInterceptor`

**接口**：实现 `org.springframework.web.servlet.HandlerInterceptor`

**核心方法签名**：
```java
// 无 @Component — 由 SecurityInterceptorConfig 以 @Bean @ConditionalOnMissingBean 声明
public class SecurityInterceptor implements HandlerInterceptor {

    @Override
    boolean preHandle(HttpServletRequest request,
                     HttpServletResponse response,
                     Object handler) throws Exception;
}
```

> **【2026-03-19 修订】** 移除 `@Component`。业务项目的组件扫描不覆盖 `com.cartisan.security.*`，Bean 必须由自动配置声明。

**前置条件**：无

**后置条件**：
- 鉴权通过返回 `true`，请求继续到 Controller
- 鉴权失败抛 Sa-Token 异常，由 `SecurityExceptionHandler` 处理

**核心流程（伪代码）**：
```
1. if (handler 不是 HandlerMethod)
       return true;  // 静态资源等放行

2. 提取 HandlerMethod 的 method 和 beanType

3. // @RequireAuth 检查
   RequireAuth requireAuth = findAnnotation(method, beanType, RequireAuth.class);
   if (requireAuth 存在 && requireAuth.value() == true)
       StpUtil.checkLogin();  // 失败抛 NotLoginException

4. // @RequireRole 检查
   RequireRole requireRole = findAnnotation(method, beanType, RequireRole.class);
   if (requireRole 存在)
       StpUtil.checkRoleOr(requireRole.value());  // 失败抛 NotRoleException

5. // @RequirePermission 检查
   RequirePermission requirePerm = findAnnotation(method, beanType, RequirePermission.class);
   if (requirePerm 存在)
       StpUtil.checkPermissionOr(requirePerm.value());  // 失败抛 NotPermissionException

6. return true;
```

**辅助方法签名**：
```java
/**
 * 方法注解优先，无则查找类注解
 * @param method Controller 方法
 * @param beanType Controller 类
 * @param annotationType 注解类型
 * @return 注解实例，不存在返回 null
 */
private <A extends Annotation> A findAnnotation(
    Method method,
    Class<?> beanType,
    Class<A> annotationType);
```

**异常**：
- 不直接捕获异常，由 `StpUtil.checkXxx()` 抛出
- Sa-Token 异常类型：
  - `cn.dev33.satoken.exception.NotLoginException`
  - `cn.dev33.satoken.exception.NotRoleException`
  - `cn.dev33.satoken.exception.NotPermissionException`

---

## 异常处理器接口描述（伪代码）

### 组件：SecurityExceptionHandler

**包路径**：`com.cartisan.security.config.SecurityExceptionHandler`

**接口**：Spring `@ControllerAdvice`

**异常处理方法**：

| 异常类型 | HTTP 状态码 | 响应消息 |
|---------|------------|---------|
| `NotLoginException` | 401 UNAUTHORIZED | 未登录或登录已过期 |
| `NotRoleException` | 403 FORBIDDEN | 无权限访问 |
| `NotPermissionException` | 403 FORBIDDEN | 无权限访问 |

**核心方法签名（伪代码）**：
```
@ControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException ex) {
        return ResponseEntity.status(401)
                .body(ApiResponse.error(401, "未登录或登录已过期"));
    }

    @ExceptionHandler(NotRoleException.class)
    ResponseEntity<ApiResponse<Void>> handleNotRole(NotRoleException ex) {
        return ResponseEntity.status(403)
                .body(ApiResponse.error(403, "无权限访问"));
    }

    @ExceptionHandler(NotPermissionException.class)
    ResponseEntity<ApiResponse<Void>> handleNotPermission(NotPermissionException ex) {
        return ResponseEntity.status(403)
                .body(ApiResponse.error(403, "无权限访问"));
    }
}
```

**依赖**：
- `com.cartisan.web.response.ApiResponse`（来自 cartisan-web 模块）
- `cn.dev33.satoken.exception.*`（来自 Sa-Token）

---

## 数据结构

### 响应体格式（ApiResponse）

成功响应（200）：
```json
{
  "success": true,
  "code": 200,
  "message": "ok",
  "data": { ... }
}
```

鉴权失败响应（401）：
```json
{
  "success": false,
  "code": 401,
  "message": "未登录或登录已过期",
  "data": null
}
```

鉴权失败响应（403）：
```json
{
  "success": false,
  "code": 403,
  "message": "无权限访问",
  "data": null
}
```

---

## 核心流程时序图

```
HTTP Request
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ SecurityInterceptor.preHandle()                             │
│                                                             │
│  ┌────────────────────────────────────────────────────┐    │
│  │ 1. 检查是否为 HandlerMethod                          │    │
│  │    非 → return true (放行)                           │    │
│  └────────────────────────────────────────────────────┘    │
│                          ↓                                  │
│  ┌────────────────────────────────────────────────────┐    │
│  │ 2. 提取注解（方法优先，无则用类注解）                  │    │
│  │    findAnnotation(method, beanType, RequireAuth)    │    │
│  │    findAnnotation(method, beanType, RequireRole)    │    │
│  │    findAnnotation(method, beanType, RequirePerm)    │    │
│  └────────────────────────────────────────────────────┘    │
│                          ↓                                  │
│  ┌────────────────────────────────────────────────────┐    │
│  │ 3. 执行鉴权                                         │    │
│  │    if (@RequireAuth 存在)                           │    │
│  │        StpUtil.checkLogin()                         │    │
│  │    if (@RequireRole 存在)                           │    │
│  │        StpUtil.checkRoleOr(roles)                   │    │
│  │    if (@RequirePermission 存在)                     │    │
│  │        StpUtil.checkPermissionOr(perms)             │    │
│  └────────────────────────────────────────────────────┘    │
│                          ↓                                  │
│  ┌─────────────┬────────────────────────────────────────┐ │
│  │ 全部通过    │ return true → 进入 Controller            │ │
│  │ 任意失败    │ 抛 Sa-Token 异常                        │ │
│  └─────────────┴────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼ (异常)
┌─────────────────────────────────────────────────────────────┐
│ SecurityExceptionHandler                                    │
│                                                             │
│  NotLoginException → 401 + ApiResponse.error(401, "...")    │
│  NotRoleException → 403 + ApiResponse.error(403, "...")    │
│  NotPermissionException → 403 + ApiResponse.error(403, "...")│
└─────────────────────────────────────────────────────────────┘
```

---

## 注解使用示例

### 示例 1：类级别注解

```java
@RestController
@RequireAuth  // 类内所有方法都需要登录
@RequestMapping("/api/v1/users")
public class UserController {
    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser() { ... }
}
```

### 示例 2：方法级别注解

```java
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @RequireRole({"admin"})
    @PostMapping("/users")
    public ApiResponse<Void> createUser() { ... }

    @RequirePermission({"user:delete"})
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) { ... }
}
```

### 示例 3：方法覆盖类注解

```java
@RestController
@RequireAuth  // 默认需要登录
@RequestMapping("/api/v1/public")
public class PublicController {

    @GetMapping("/info")
    public ApiResponse<Info> getInfo() { ... }  // 需要登录

    @RequireAuth(false)  // 覆盖类注解，允许匿名访问
    @GetMapping("/ping")
    public ApiResponse<String> ping() { ... }
}
```

### 示例 4：类 + 方法叠加（AND 逻辑）

```java
@RestController
@RequireAuth  // 需要登录
@RequestMapping("/api/v1/admin")
public class AdminController {

    @RequireRole({"admin", "super"})  // 需要（登录 AND admin/super 角色）
    @PostMapping("/users")
    public ApiResponse<Void> createUser() { ... }
}
```

---

## 技术实现要点

### 1. Sa-Token API 对应关系

| 功能 | Sa-Token API | 异常类型 |
|------|-------------|---------|
| 检查登录 | `StpUtil.checkLogin()` | `NotLoginException` |
| 检查角色（OR） | `StpUtil.checkRoleOr(String... roles)` | `NotRoleException` |
| 检查权限（OR） | `StpUtil.checkPermissionOr(String... perms)` | `NotPermissionException` |

### 2. 注解查找逻辑

```
查找顺序：
1. method.getAnnotation(annotationType)
2. 如果存在 → 返回方法注解
3. 如果不存在 → beanType.getAnnotation(annotationType)
4. 都不存在 → 返回 null
```

### 3. 多值 OR 逻辑

- `@RequireRole({"admin", "super"})` → `StpUtil.checkRoleOr("admin", "super")`
- 满足任一角色即可，不要求全部满足

### 4. 静态 Mock（测试用）

```java
// 测试时需要 Mock StpUtil 静态方法
MockedStatic<StpUtil> mockedStpUtil = Mockito.mockStatic(StpUtil.class);
mockedStpUtil.when(StpUtil::checkLogin).thenAnswer(invocation -> null);  // 通过
// 或
mockedStpUtil.when(StpUtil::checkLogin).thenThrow(new NotLoginException());  // 失败
```

---

## 与其他模块的依赖关系

| 模块 | 依赖类型 | 说明 |
|------|---------|------|
| cartisan-core | compile | 基础类型（如有需要） |
| cartisan-web | compile | `ApiResponse` 响应格式 |
| sa-token-spring-boot3-starter | compile | Sa-Token 核心 API |

---

## 数据库变更

无。F03-02 仅涉及应用层鉴权逻辑，不涉及数据库变更。

---

## 配置变更

无。F03-02 只实现拦截器和异常处理器，不涉及配置文件。拦截器注册由 F03-07（自动配置）负责。
