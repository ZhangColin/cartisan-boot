# Feature: F03-02 权限注解 + MVC 拦截器

## 背景

cartisan-security 模块需要提供声明式的权限控制能力。业务代码通过注解（`@RequireAuth`、`@RequireRole`、`@RequirePermission`）描述接口的鉴权要求，由 MVC 拦截器统一处理，避免在每个 Controller 方法中重复编写鉴权逻辑。

当前 Sa-Token 提供了 `StpUtil.checkXxx()` API，但直接在业务代码中调用会导致：
1. 鉴权逻辑分散在各个 Controller
2. 难以统一维护和替换底层安全实现
3. 与业务代码耦合，不利于测试

## 目标

- 提供声明式权限注解，业务代码通过注解描述鉴权要求
- 通过 MVC 拦截器统一处理鉴权逻辑
- 鉴权失败时返回标准化的 `ApiResponse` 错误响应（401/403）
- 保持 cartisan-web 与安全实现解耦

## 范围

### 包含（In Scope）

1. **三个权限注解**：
   - `@RequireAuth`：要求用户登录
   - `@RequireRole({"admin", "super"})`：要求用户拥有指定角色之一（OR 逻辑）
   - `@RequirePermission({"user:create", "user:update"})`：要求用户拥有指定权限之一（OR 逻辑）

2. **MVC 拦截器**（`SecurityInterceptor`）：
   - 从 `HandlerMethod` 读取注解
   - 支持类级别和方法级别注解，方法注解优先
   - 调用 Sa-Token 的 `StpUtil.checkXxx()` 执行鉴权
   - 非 `HandlerMethod` 直接放行

3. **异常处理器**（`SecurityExceptionHandler`）：
   - 位于 cartisan-security 模块内部
   - 捕获 Sa-Token 异常并转为 `ApiResponse` 格式
   - 401：未登录（`NotLoginException`）
   - 403：无权限（`NotRoleException`、`NotPermissionException`）

4. **单元测试**：
   - 拦截器逻辑测试
   - 异常处理器测试
   - 覆盖率 ≥ 80%

### 不包含（Out of Scope）

- **拦截器注册**：由 F03-07（自动配置）负责将 `SecurityInterceptor` 注册到 Spring MVC
- **登录/登出逻辑**：由 F03-06（AuthenticationService）负责
- **用户上下文**：由 F03-03（SecurityContext）负责
- **多租户上下文**：由 F03-04/F03-05 负责

## 验收标准（Acceptance Criteria）

- **AC1**：`@RequireAuth` 注解可作用于类和方法，未登录时返回 401
- **AC2**：`@RequireRole({"admin"})` 注解生效，无角色时返回 403
- **AC3**：`@RequirePermission({"user:create"})` 注解生效，无权限时返回 403
- **AC4**：方法注解优先于类注解（方法有注解时忽略类注解）
- **AC5**：`@RequireRole` 和 `@RequirePermission` 支持多值 OR 逻辑
- **AC6**：无任何鉴权注解的接口直接放行，不调用 `StpUtil`
- **AC7**：非 `HandlerMethod`（静态资源等）直接放行
- **AC8**：异常处理器将 Sa-Token 异常转为 `ApiResponse` 格式

## 约束

- **架构约束**：
  - 注解放置于 `com.cartisan.security.annotation` 包
  - 拦截器和异常处理器放置于 `com.cartisan.security.config` 包
  - cartisan-web 不依赖 Sa-Token

- **技术约束**：
  - 使用 Java 21
  - 使用 Spring MVC `HandlerInterceptor`
  - 使用 Sa-Token 1.45+ 的 `StpUtil` API
  - 测试使用 Mockito 3.4+ 的 `mockStatic` 静态 Mock

- **性能约束**：
  - 拦截器执行时间 < 5ms（鉴权操作本身）

## 依赖

- **前置依赖**：F03-01（cartisan-security 模块骨架）已完成
- **后续依赖**：F03-07 将注册本拦截器到 Spring MVC

## 设计决策（来自 Phase 1 澄清）

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 异常处理归属 | cartisan-security 内部实现 `SecurityExceptionHandler` | 保持 cartisan-web 与安全实现解耦 |
| 注解作用目标 | 类级别 + 方法级别，方法注解优先 | 减少重复，符合常见用法 |
| 多值支持 | `String[]` 类型，OR 逻辑 | 与 Sa-Token API 对齐，实现简单 |
| 拦截器注册方式 | 只实现拦截器 Bean，注册留给 F03-07 | 职责分离，F03-02 只做鉴权逻辑 |
| 实现模式 | 直接代理模式（方案 A） | 与当前规模匹配，符合 YAGNI |
