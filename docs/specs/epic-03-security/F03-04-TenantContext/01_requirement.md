# Feature: F03-04 TenantContext — 多租户上下文

## 背景

在多租户 SaaS 系统中，每个请求都需要关联到特定的租户（Tenant）。当前用户上下文（SecurityContext）提供了用户信息，但缺少租户信息的上下文管理。

TenantContext 需要提供：
1. 在请求进入时解析并存储租户 ID
2. 在业务代码中读取当前租户 ID
3. 兼容 Java 21 Virtual Threads 环境

## 目标

- 提供租户 ID 的存取 API，与 SecurityContext 风格一致
- 使用 ScopedValue 实现，确保 Virtual Threads 兼容性
- 支持两种数据来源：Header（`X-Tenant-Id`）优先于 Sa-Token Session

## 范围

### 包含（In Scope）

1. **TenantContext 工具类**
   - `getCurrentTenantId()` — 获取当前租户 ID，无则返回 null
   - `hasTenant()` — 判断是否有租户上下文
   - `requireTenant()` — 获取租户 ID，无则抛异常
   - `runWithTenant()` — package-private，供 Filter 绑定租户上下文

2. **存储机制**
   - 使用 Java 21+ ScopedValue 存储
   - 支持在 Virtual Threads 中正确传递租户上下文
   - 作用域结束自动清理，无需手动 remove

3. **单元测试**
   - 覆盖所有公共 API
   - 测试无租户、有租户、null 租户、作用域清理场景

### 不包含（Out of Scope）

- **Filter 实现**（F03-05 TenantContextFilter）
- **租户鉴权逻辑**（只存取租户 ID，不验证权限）
- **自动配置**（F03-07 AutoConfiguration）

## 验收标准（Acceptance Criteria）

### AC1: 无租户上下文时返回 null

**Given** 当前请求未设置租户上下文
**When** 调用 `getCurrentTenantId()`
**Then** 返回 `null`

### AC2: 有租户上下文时返回租户 ID

**Given** 当前请求已设置租户 ID 为 `123L`
**When** 调用 `getCurrentTenantId()`
**Then** 返回 `123L`

### AC3: hasTenant 正确反映租户状态

**Given** 当前请求有/无租户上下文
**When** 调用 `hasTenant()`
**Then** 有租户返回 `true`，无租户返回 `false`

### AC4: requireTenant 在无租户时抛异常

**Given** 当前请求未设置租户上下文
**When** 调用 `requireTenant()`
**Then** 抛出 `IllegalStateException`

### AC5: requireTenant 在有租户时返回租户 ID

**Given** 当前请求已设置租户 ID 为 `456L`
**When** 调用 `requireTenant()`
**Then** 返回 `456L`

### AC6: 作用域结束后租户自动清理

**Given** 在 `runWithTenant(111L, ...)` 作用域内
**When** 作用域结束后调用 `getCurrentTenantId()`
**Then** 返回 `null`

### AC7: 支持 null 租户（显式清除）

**Given** 使用 `runWithTenant(null, ...)` 绑定 null
**When** 在作用域内调用 `getCurrentTenantId()`
**Then** 返回 `null`

## 约束

### 技术约束

- **Java 版本**：Java 21+（使用 ScopedValue）
- **存储机制**：ScopedValue，不使用 ThreadLocal
- **可见性**：`runWithTenant()` 为 package-private，只允许 Filter 调用
- **类设计**：final 类 + 私有构造函数，防止实例化

### API 风格约束

- 与 `SecurityContext` 保持一致的风格
- 未设置时返回 `null`，不使用魔法数字
- `requireTenant()` 使用 `IllegalStateException`，不引入自定义异常

### 测试约束

- 测试类放在 `com.cartisan.security.context` 包下
- 使用 `given-when-then` 命名规范
- 测试必须覆盖所有公共方法

## 设计决策

### D1: 数据来源优先级 — Header > Session

**理由**：
- 服务间调用通过 Header 显式传租户
- 管理员可通过 Header 切换到其他租户查看
- 登录用户 Session 中的租户作为默认值

### D2: 使用 ScopedValue 而非 ThreadLocal

**理由**：
- 项目已使用 Java 21，无老版本兼容需求
- ScopedValue 与 Virtual Threads 完美配合
- 作用域结束自动清理，无需手动 remove

### D3: 使用 runWithTenant 替代 setCurrentTenantId + clear

**理由**：
- ScopedValue 的设计模式是"在作用域中执行"
- `where(key, value).run(action)` 自动管理生命周期
- 避免忘记调用 `clear()` 导致的内存泄漏

### D4: requireTenant 抛出 IllegalStateException

**理由**：
- JDK 标准异常，语义清晰
- TenantContext 是基础设施，不应引入业务异常
- 业务层可自行包装为特定异常类型

## 依赖

### 模块内依赖

- 无（TenantContext 是独立的工具类）

### 模块外依赖

- `cartisan-security` 模块已存在
- F03-01 模块骨架已完成

### 后续依赖

- F03-05 TenantContextFilter 将使用 `runWithTenant()` 绑定租户上下文

## 参考

- Epic Backlog: [00_epic_backlog.md](../00_epic_backlog.md)
- SecurityContext: `com.cartisan.security.context.SecurityContext`
- SKILL.md: [docs/skills/SKILL.md](../../../../skills/SKILL.md)
