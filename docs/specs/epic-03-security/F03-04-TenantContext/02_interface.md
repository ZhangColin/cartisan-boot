# Feature: F03-04 TenantContext — 接口契约

## 类设计

### TenantContext 工具类

**包路径：** `com.cartisan.security.context`

**类声明（伪代码）：**
```java
public final class TenantContext {
    // 私有构造函数，防止实例化
    private TenantContext() { throw new UnsupportedOperationException(...) }

    // ScopedValue 存储键（package-private，供 Filter 调用）
    static final ScopedValue<Long> TENANT_ID = ...

    // 公共 API（业务代码调用）
    public static Long getCurrentTenantId()
    public static boolean hasTenant()
    public static Long requireTenant()

    // package-private API（Filter 调用）
    static void runWithTenant(Long tenantId, Runnable runnable)
}
```

**设计要点：**
- `final` 类，防止继承
- 私有构造函数抛出异常，防止反射实例化
- 无状态，所有方法都是静态方法

---

## 接口定义

### 公共 API（Public API）

#### getCurrentTenantId()

**签名：**
```java
public static Long getCurrentTenantId()
```

**描述：** 获取当前租户 ID

**返回值：**
| 情况 | 返回值 |
|------|--------|
| 有租户上下文 | 租户 ID（Long） |
| 无租户上下文 | `null` |

**异常：** 无

**前置条件：** 无

**后置条件：** 无副作用

---

#### hasTenant()

**签名：**
```java
public static boolean hasTenant()
```

**描述：** 判断当前请求是否有租户上下文

**返回值：**
| 情况 | 返回值 |
|------|--------|
| 有租户上下文 | `true` |
| 无租户上下文 | `false` |

**异常：** 无

**前置条件：** 无

**后置条件：** 无副作用

**实现伪代码：**
```java
return getCurrentTenantId() != null;
```

---

#### requireTenant()

**签名：**
```java
public static Long requireTenant()
```

**描述：** 获取当前租户 ID，若不存在则抛出异常

**返回值：**
| 情况 | 返回值 |
|------|--------|
| 有租户上下文 | 租户 ID（Long） |

**异常：**
| 异常类型 | 触发条件 |
|---------|---------|
| `IllegalStateException` | 无租户上下文 |

**错误消息：** `"No tenant context available"`

**前置条件：** 无

**后置条件：** 无副作用

**实现伪代码：**
```java
Long tenantId = getCurrentTenantId();
if (tenantId == null) {
    throw new IllegalStateException("No tenant context available");
}
return tenantId;
```

---

### Package-Private API

#### runWithTenant()

**签名：**
```java
static void runWithTenant(Long tenantId, Runnable runnable)
```

**描述：** 在指定租户上下文中执行任务

**可见性：** package-private（仅限 `com.cartisan.security.context` 包内调用）

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| tenantId | Long（可空） | 要绑定的租户 ID，null 表示不绑定 |
| runnable | Runnable | 要执行的任务 |

**异常：** 无（抛出的异常由调用方处理）

**行为：**
1. 若 `tenantId != null`：使用 `ScopedValue.where(TENANT_ID, tenantId).run(runnable)` 绑定租户上下文
2. 若 `tenantId == null`：直接执行 `runnable.run()`，不绑定任何值
3. 作用域结束时自动清理，无需手动 remove

**实现伪代码：**
```java
if (tenantId != null) {
    ScopedValue.where(TENANT_ID, tenantId).run(runnable);
} else {
    runnable.run();
}
```

**使用示例（Filter 中）：**
```java
// TenantContextFilter 中
Long tenantId = parseTenantId(request);  // 从 Header 或 Session 解析
TenantContext.runWithTenant(tenantId, () -> {
    filterChain.doFilter(request, response);
});
// 作用域结束，租户自动清理
```

---

## 存储机制

### ScopedValue 设计

**常量声明（package-private）：**
```java
static final ScopedValue<Long> TENANT_ID = ScopedValue.newInstance();
```

**读取操作：**
```java
// getCurrentTenantId() 实现
public static Long getCurrentTenantId() {
    return ScopedValue.getOrDefault(TENANT_ID, null);
}
```

**设计理由：**
- 使用 `getOrDefault(TENANT_ID, null)` 而非 `get(TENANT_ID)`
- 未绑定时 `get()` 会抛 `NoSuchElementException`，`getOrDefault()` 返回 null
- 与 "未设置时返回 null" 的语义一致

**作用域管理：**
- ScopedValue 的值在 `where(...).run(...)` 作用域内有效
- 作用域结束后自动清理，无需手动 remove
- 子任务（包括 Virtual Threads）自动继承父任务的租户上下文

---

## 核心流程

### 读取租户 ID 流程

```pseudocode
FUNCTION getCurrentTenantId()
    RETURN ScopedValue.getOrDefault(TENANT_ID, null)
END FUNCTION
```

### 绑定租户上下文流程（Filter 调用）

```pseudocode
FUNCTION runWithTenant(tenantId, runnable)
    IF tenantId IS NOT NULL THEN
        // 绑定租户 ID 到 ScopedValue，在作用域内执行
        ScopedValue.where(TENANT_ID, tenantId).run(runnable)
    ELSE
        // 不绑定，直接执行
        runnable.run()
    END IF
END FUNCTION
```

### Filter 集成流程（F03-05 实现）

```pseudocode
// TenantContextFilter.doFilter()
FUNCTION doFilter(request, response, chain)
    // 1. 尝试从 Header 解析租户 ID
    tenantId = request.getHeader("X-Tenant-Id")

    // 2. 若 Header 不存在，尝试从 Sa-Token Session 读取
    IF tenantId IS NULL AND StpUtil.isLogin() THEN
        tenantId = StpUtil.getSession().get("tenantId")
    END IF

    // 3. 绑定租户上下文，执行后续过滤器
    TenantContext.runWithTenant(tenantId, () -> {
        chain.doFilter(request, response)
    })
    // 4. 作用域结束，租户自动清理
END FUNCTION
```

---

## 技术要点

### 1. ScopedValue vs ThreadLocal

| 特性 | ScopedValue | ThreadLocal |
|------|-------------|-------------|
| Java 版本 | 21+ | 1.2+ |
| Virtual Threads 兼容 | ✅ 完美兼容 | ⚠️ 需要额外处理 |
| 清理方式 | 作用域结束自动清理 | 需要手动 remove() |
| 继承性 | 自动传递给子任务 | 需要 InheritableThreadLocal |

### 2. 为什么不需要 setCurrentTenantId() 和 clear()

Epic Backlog 原计划提供 `setCurrentTenantId()` 和 `clear()`，但 ScopedValue 的设计模式不同：

| 原设计 | 新设计 |
|--------|--------|
| `setCurrentTenantId(id)` + `clear()` | `runWithTenant(id, runnable)` |
| 手动管理生命周期 | 作用域自动管理 |
| 可能忘记 clear() | 无法忘记，作用域结束即清理 |

### 3. 异常处理策略

`requireTenant()` 使用 `IllegalStateException` 而非自定义异常：

| 方案 | 优点 | 缺点 |
|------|------|------|
| `IllegalStateException` | JDK 标准，语义清晰 | 无业务特定信息 |
| 自定义 `NoTenantException` | 可携带更多信息 | 引入新异常类型 |

**决策：** TenantContext 是基础设施，使用标准异常；业务层可自行包装。

---

## 与 SecurityContext 的对比

| 特性 | SecurityContext | TenantContext |
|------|----------------|---------------|
| 数据来源 | Sa-Token（StpUtil） | Header / Session |
| 存储机制 | 代理 Sa-Token Session | ScopedValue |
| 设置方式 | 无公共设置方法 | `runWithTenant()`（package-private） |
| 清理方式 | 无需清理（Sa-Token 管理） | 作用域结束自动清理 |
| 未设置返回值 | `null` | `null` |
| "必须有"方法 | 无 | `requireTenant()` |

---

## 依赖关系

### 当前 Feature 依赖

- F03-01（模块骨架）✅ 完成

### 当前 Feature 被依赖

- F03-05（TenantContextFilter）将使用 `runWithTenant()`

---

## 后续工作（不在本 Feature 范围）

1. **F03-05 TenantContextFilter**
   - 实现 Filter，从 Header 或 Session 解析租户 ID
   - 调用 `TenantContext.runWithTenant()` 绑定租户上下文

2. **F03-07 AutoConfiguration**
   - 自动注册 TenantContextFilter
   - 配置 Filter 拦截规则

---

## 参考

- Epic Backlog: [00_epic_backlog.md](../00_epic_backlog.md)
- Requirement Spec: [01_requirement.md](./01_requirement.md)
- SecurityContext: `com.cartisan.security.context.SecurityContext`
- SKILL.md: [docs/skills/SKILL.md](../../../../skills/SKILL.md)
