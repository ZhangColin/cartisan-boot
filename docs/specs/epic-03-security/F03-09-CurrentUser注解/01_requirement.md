# Feature: F03-09 @CurrentUser 注解

> **Epic**: E03 Security
> **复杂度**: S（Small，30-60 行）
> **依赖**: F03-01（模块骨架）、F03-03（SecurityContext）
> **预估工时**: 0.5d

---

## 背景

`SecurityContext`（F03-03）已提供获取当前用户信息的能力，但 Controller 层使用时仍需手动调用：

```java
@GetMapping("/profile")
public ApiResponse<UserProfile> getProfile() {
    Long userId = SecurityContext.getCurrentUserId();
    if (userId == null) {
        throw new NotLoginException();  // 手动处理未登录
    }
    return ApiResponse.ok(userService.getProfile(userId));
}
```

这种方式存在以下问题：
- 每个方法都需要重复检查 `null`
- 未登录场景需要手动抛异常
- 无法表达「可选登录」语义（允许匿名访问的接口）

Spring MVC 提供了 `HandlerMethodArgumentResolver` 机制，允许自定义参数解析逻辑，可实现对带注解参数的自动注入。

## 目标

创建 `@CurrentUser` 注解 + `CurrentUserMethodArgumentResolver`，支持在 Controller 方法中直接注入当前用户 ID：

1. **简化代码**：不再需要手动调用 `SecurityContext.getCurrentUserId()` 和 null 检查
2. **类型安全**：通过参数类型区分必需/可选登录
3. **自动集成**：通过 AutoConfiguration 自动注册，零配置使用

## 范围

### 包含（In Scope）

- `@CurrentUser` 注解（`com.cartisan.security.annotation`）
- `CurrentUserMethodArgumentResolver`（实现 `HandlerMethodArgumentResolver`）
- `CurrentUserArgumentResolverConfig`（独立配置类）
- 修改 `CartisanSecurityAutoConfiguration` 添加 `@Import`
- 单元测试（Resolver 逻辑）
- 集成测试（端到端验证）

### 不包含（Out of Scope）

- 不支持 `Long`/`Optional<Long>` 以外的类型
- 不支持注入完整用户对象（业务层可通过 userId 查询）
- 无数据库变更
- 无 application.yml 配置变更

## 验收标准（Acceptance Criteria）

| ID | 验收标准 | 测试方法命名 |
|----|---------|-------------|
| AC1 | `@CurrentUser Long userId` 已登录时注入用户 ID | `given_authenticatedUser_when_resolveLong_then_returnUserId` |
| AC2 | `@CurrentUser Long userId` 未登录时抛 `NotLoginException` → 401 | `given_unauthenticatedUser_when_resolveLong_then_throwNotLoginException` |
| AC3 | `@CurrentUser Optional<Long> userId` 已登录时注入 `Optional.of(userId)` | `given_authenticatedUser_when_resolveOptional_then_returnPresent` |
| AC4 | `@CurrentUser Optional<Long> userId` 未登录时返回 `Optional.empty()` | `given_unauthenticatedUser_when_resolveOptional_then_returnEmpty` |
| AC5 | 参数无 `@CurrentUser` 注解时不解析 | `given_parameterWithoutAnnotation_when_supportsParameter_then_false` |
| AC6 | 参数类型不支持时不解析 | `given_unsupportedType_when_supportsParameter_then_false` |
| AC7 | 引入依赖后 Resolver 自动注册 | `given_contextLoaded_when_resolverRegistered_then_success` |
| AC8 | 集成测试：已登录访问接口返回 200 | `given_authenticatedUser_when_getProfile_then_return200` |
| AC9 | 集成测试：未登录访问 `@CurrentUser Long` 接口返回 401 | `given_unauthenticatedUser_when_getProfile_then_return401` |
| AC10 | 集成测试：未登录访问 `@CurrentUser Optional<Long>` 接口返回 200 | `given_unauthenticatedUser_when_getPreferences_then_return200` |

## 约束

### 功能约束

| 约束项 | 说明 |
|--------|------|
| **支持的类型** | 仅支持 `Long` 和 `Optional<Long>` |
| **必需登录行为** | `@CurrentUser Long userId` 未登录时抛 `NotLoginException` |
| **可选登录行为** | `@CurrentUser Optional<Long> userId` 未登录时返回 `Optional.empty()` |
| **数据来源** | 调用 `SecurityContext.getCurrentUserId()`，不直接依赖 Sa-Token |
| **异常处理** | 复用 `SecurityExceptionHandler`，不新增异常处理器 |

### 非功能约束

| 约束项 | 说明 |
|--------|------|
| **线程安全** | 无状态（数据来自 `SecurityContext`，其来自 `StpUtil`） |
| **性能** | 每次请求解析一次参数，开销可忽略 |
| **测试覆盖** | 单元测试覆盖率 ≥ 80% |

## 使用示例

### 必需登录场景

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // 必须登录，未登录返回 401
    @GetMapping("/profile")
    public ApiResponse<UserProfile> getProfile(@CurrentUser Long userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@CurrentUser Long userId,
                                           @RequestBody UpdateProfileCommand cmd) {
        userService.updateProfile(userId, cmd);
        return ApiResponse.ok();
    }
}
```

### 可选登录场景

```java
@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {

    // 允许匿名访问，已登录返回个性化，未登录返回默认
    @GetMapping
    public ApiResponse<Preferences> getPreferences(@CurrentUser Optional<Long> userId) {
        if (userId.isPresent()) {
            return ApiResponse.ok(preferencesService.getForUser(userId.get()));
        }
        return ApiResponse.ok(preferencesService.getDefault());
    }

    // 简化写法
    @GetMapping("/widgets")
    public ApiResponse<Widgets> getWidgets(@CurrentUser Optional<Long> userId) {
        return ApiResponse.ok(widgetsService.getWidgets(userId.orElse(null)));
    }
}
```

### 与 @RequireAuth 的区别

| 注解 | 作用时机 | 适用场景 |
|------|---------|---------|
| `@RequireAuth` | 方法执行前，拦截器阶段 | 整个接口需要登录 |
| `@CurrentUser Long userId` | 参数解析阶段 | 需要使用 userId，未登录抛异常 |
| `@CurrentUser Optional<Long> userId` | 参数解析阶段 | 允许匿名访问，但已登录可获取 userId |

**组合使用示例：**

```java
// 方式一：只用 @CurrentUser
@GetMapping("/profile")
public ApiResponse<UserProfile> getProfile(@CurrentUser Long userId) {
    // 未登录会在参数解析时抛 NotLoginException
}

// 方式二：@RequireAuth + @CurrentUser
@RequireAuth
@GetMapping("/profile")
public ApiResponse<UserProfile> getProfile(@CurrentUser Long userId) {
    // 未登录会在拦截器阶段被拦截，不会到达参数解析
    // 这种写法语义更明确，推荐使用
}
```

## 边界场景与异常处理

| 场景 | 行为 |
|------|------|
| 已登录 + `@CurrentUser Long userId` | 返回 userId |
| 未登录 + `@CurrentUser Long userId` | 抛 `NotLoginException` → 401 |
| 已登录 + `@CurrentUser Optional<Long> userId` | 返回 `Optional.of(userId)` |
| 未登录 + `@CurrentUser Optional<Long> userId` | 返回 `Optional.empty()` |
| 参数类型为 `String`/其他类型 | `supportsParameter()` 返回 `false`，不解析 |
| 参数无 `@CurrentUser` 注解 | `supportsParameter()` 返回 `false`，不解析 |
| 参数为 `Optional<Long>` 但无 `@CurrentUser` | 不触发此 Resolver，由 Spring 处理 |

## 参考文档

- Epic Backlog: [00_epic_backlog.md](../00_epic_backlog.md)
- F03-03 SecurityContext: [01_requirement.md](../F03-03-SecurityContext/01_requirement.md)
- F03-07 AutoConfiguration: [01_requirement.md](../F03-07-AutoConfiguration/01_requirement.md)
- AI 协作 SOP: [AI协作开发SOP.md](../../../sop/AI协作开发SOP.md)
- Spring MVC 文档: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller-ann-arguments.html
- SKILL.md: [SKILL.md](../../../skills/SKILL.md)
