# Feature: F03-03 SecurityContext

> **Epic**: E03 Security
> **复杂度**: S（Small，30-60 行）
> **依赖**: F03-01（模块骨架）
> **预估工时**: 0.5d

---

## 背景

`SecurityInterceptor`（F03-02）已实现声明式鉴权，但业务代码中仍有场景需要主动获取当前用户信息：
- Service 层记录操作日志（需要 userId/username）
- 领域逻辑中判断当前用户权限
- Filter/Interceptor 中获取租户 ID 等上下文信息

直接使用 Sa-Token 的 `StpUtil` 会导致业务代码与 Sa-Token 强耦合。需要一层薄抽象来隐藏实现细节。

## 目标

创建 `SecurityContext` 工具类，作为 Sa-Token `StpUtil` 的只读薄抽象层：

1. **隐藏 Sa-Token 依赖**：业务代码使用 `SecurityContext`，不直接依赖 `StpUtil`
2. **语义清晰**：方法命名表达业务语义（`getCurrentUserId` vs `getLoginId`）
3. **易于测试**：支持 `MockedStatic` mock，与 `SecurityInterceptor` 测试风格一致

## 范围

### 包含（In Scope）

- `SecurityContext` 工具类（`com.cartisan.security.context`）
- 完整的单元测试（覆盖所有方法 + 边界场景）
- JavaDoc 文档（含使用示例）

### 不包含（Out of Scope）

- 无配置变更（无 application.yml）
- 无数据库变更
- 无 AutoConfiguration（F03-07 统一处理）
- 不提供 `setCurrentUser()` 等写操作（仅读操作）

## 验收标准（Acceptance Criteria）

| ID | 验收标准 | 测试方法命名 |
|----|---------|-------------|
| AC1 | `getCurrentUserId()` 已登录时返回用户 ID | `given_userLoggedIn_when_getCurrentUserId_then_returnUserId` |
| AC2 | `getCurrentUserId()` 未登录时返回 null | `given_userNotLoggedIn_when_getCurrentUserId_then_returnNull` |
| AC3 | `getCurrentUsername()` 已登录时返回用户名 | `given_userLoggedIn_when_getCurrentUsername_then_returnUsername` |
| AC4 | `getCurrentUsername()` 未登录时返回 null | `given_userNotLoggedIn_when_getCurrentUsername_then_returnNull` |
| AC5 | `hasRole("admin")` 有角色时返回 true | `given_userHasRole_when_hasRole_then_returnTrue` |
| AC6 | `hasRole("admin")` 无角色或未登录时返回 false | `given_userHasNoRole_when_hasRole_then_returnFalse` |
| AC7 | `hasPermission("user:create")` 有权限时返回 true | `given_userHasPermission_when_hasPermission_then_returnTrue` |
| AC8 | `hasPermission("user:create")` 无权限或未登录时返回 false | `given_userHasNoPermission_when_hasPermission_then_returnFalse` |
| AC9 | `isAuthenticated()` 已登录时返回 true | `given_userLoggedIn_when_isAuthenticated_then_returnTrue` |
| AC10 | `isAuthenticated()` 未登录时返回 false | `given_userNotLoggedIn_when_isAuthenticated_then_returnFalse` |
| AC11 | 工具类不可实例化 | `given_reflectionInstantiate_when_throwUnsupportedOperationException` |

## 约束

### 功能约束

| 约束项 | 说明 |
|--------|------|
| **未登录行为** | `getCurrentUserId()` / `getCurrentUsername()` 返回 `null` |
| **权限检查行为** | `hasRole()` / `hasPermission()` 未登录时返回 `false`（直接代理 Sa-Token） |
| **类设计** | `final class` + 私有构造函数抛出 `UnsupportedOperationException` |
| **依赖范围** | 只依赖 `StpUtil`，不引入其他 Sa-Token 类 |

### 非功能约束

| 约束项 | 说明 |
|--------|------|
| **线程安全** | 无状态（数据来自 `StpUtil`，Sa-Token 保证线程安全） |
| **性能** | 静态方法直接调用，无额外开销 |
| **测试覆盖** | 单元测试覆盖率 ≥ 80% |

## 使用示例

### 推荐用法：先检查登录

```java
if (SecurityContext.isAuthenticated()) {
    Long userId = SecurityContext.getCurrentUserId();
    String username = SecurityContext.getCurrentUsername();
    // 使用 userId/username...
}
```

### 备选用法：null 检查

```java
Long userId = SecurityContext.getCurrentUserId();
if (userId != null) {
    // 使用 userId...
}
```

### 权限判断

```java
if (SecurityContext.hasRole("admin")) {
    // 管理员逻辑...
}

if (SecurityContext.hasPermission("user:create")) {
    // 有创建用户权限...
}
```

## 边界场景与异常处理

| 场景 | 行为 |
|------|------|
| 用户未登录调用 `getCurrentUserId()` | 返回 `null`（不抛异常） |
| 用户未登录调用 `getCurrentUsername()` | 返回 `null`（不抛异常） |
| 用户未登录调用 `hasRole()` | 返回 `false`（不抛异常） |
| 用户未登录调用 `hasPermission()` | 返回 `false`（不抛异常） |
| 用户未登录调用 `isAuthenticated()` | 返回 `false` |
| 尝试反射实例化 | 抛出 `UnsupportedOperationException` |

## 参考文档

- Epic Backlog: [00_epic_backlog.md](../00_epic_backlog.md)
- AI 协作 SOP: [AI协作开发SOP.md](../../../sop/AI协作开发SOP.md)
- Sa-Token 文档: https://sa-token.cc/doc.html#/use/id-source
- SKILL.md: [SKILL.md](../../../skills/SKILL.md)
