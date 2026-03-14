# Feature: F03-05 TenantContextFilter — 接口契约

## 类设计

### TenantContextFilter 过滤器类

**包路径：** `com.cartisan.security.context`

**类声明（伪代码）：**
```java
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Component("cartisanTenantContextFilter")
public final class TenantContextFilter implements Filter {
    // 私有构造函数（Spring 通过 @Component 实例化）
    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    // 公共方法
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException;

    // 私有辅助方法
    private Long resolveTenantId(HttpServletRequest request);
    private Long parseTenantIdFromHeader(HttpServletRequest request);
    private Long parseTenantIdFromSession();
}
```

**设计要点：**
- `final` 类，防止继承
- `@Component` 注解，Spring 自动扫描注册
- `@Order` 确保优先执行（在 SecurityInterceptor 之前）
- Bean 名称显式指定为 `cartisanTenantContextFilter`，避免与其他 Filter 冲突
- 实现 `jakarta.servlet.Filter` 接口

---

## 接口定义

### 公共 API（Public API）

#### doFilter()

**签名：**
```java
@Override
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
```

**描述：** 过滤器入口方法，解析租户 ID 并绑定到 TenantContext

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| request | ServletRequest | HTTP 请求（需强转为 HttpServletRequest） |
| response | ServletResponse | HTTP 响应 |
| chain | FilterChain | 过滤器链 |

**异常：**
| 异常类型 | 触发条件 |
|---------|---------|
| IOException | I/O 错误 |
| ServletException | Servlet 错误 |

**行为：**
1. 调用 `resolveTenantId()` 解析租户 ID
2. 调用 `TenantContext.runWithTenant(tenantId, runnable)` 绑定租户上下文
3. 执行后续过滤器链
4. 作用域结束，租户上下文自动清理（ScopedValue 机制）

**前置条件：** 无

**后置条件：** 租户上下文已清理

---

### 私有辅助方法（Private Helper Methods）

#### resolveTenantId()

**签名：**
```java
private Long resolveTenantId(HttpServletRequest request)
```

**描述：** 解析租户 ID，按 Header > Session 优先级

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| request | HttpServletRequest | HTTP 请求 |

**返回值：**
| 情况 | 返回值 |
|------|--------|
| Header 存在且有效 | 解析的租户 ID |
| Header 无效，Session 有租户 | 从 Session 读取的租户 ID |
| 都不存在 | `null` |

**解析逻辑：**
1. 调用 `parseTenantIdFromHeader(request)` 尝试从 Header 解析
2. 若返回 `null`，调用 `parseTenantIdFromSession()` 从 Session 读取
3. 返回最终结果

---

#### parseTenantIdFromHeader()

**签名：**
```java
private Long parseTenantIdFromHeader(HttpServletRequest request)
```

**描述：** 从 `X-Tenant-Id` Header 解析租户 ID

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| request | HttpServletRequest | HTTP 请求 |

**返回值：**
| 情况 | 返回值 |
|------|--------|
| Header 存在且格式正确 | 解析的租户 ID |
| Header 不存在 | `null` |
| Header 为空字符串 | `null` |
| Header 格式错误 | `null`（记录 WARN 日志） |

**异常：** 无（格式错误时记录日志并返回 null）

**行为：**
1. 读取 `X-Tenant-Id` Header
2. 若为 `null` 或空白，返回 `null`
3. 尝试 `Long.parseLong()` 解析
4. 解析失败：记录 WARN 日志，返回 `null`
5. 解析成功：记录 DEBUG 日志，返回租户 ID

---

#### parseTenantIdFromSession()

**签名：**
```java
private Long parseTenantIdFromSession()
```

**描述：** 从 Sa-Token Session 读取租户 ID

**参数：** 无

**返回值：**
| 情况 | 返回值 |
|------|--------|
| 用户已登录且 Session 有 tenantId | 解析的租户 ID |
| 用户未登录 | `null` |
| Session 中无 tenantId | `null` |

**异常：** 无（解析失败时返回 null）

**行为：**
1. 检查 `StpUtil.isLogin()`
2. 若未登录，返回 `null`
3. 调用 `StpUtil.getSession().get("tenantId")` 读取
4. 若为 `null`，返回 `null`
5. 尝试转为 Long：`Long.parseLong(sessionTenantId.toString())`
6. 解析成功：记录 DEBUG 日志，返回租户 ID
7. 解析失败：记录 WARN 日志，返回 `null`

---

## 核心流程

### doFilter 主流程

```pseudocode
FUNCTION doFilter(request, response, chain)
    httpServletRequest = CAST request TO HttpServletRequest

    // 解析租户 ID
    tenantId = resolveTenantId(httpRequest)

    // 使用 ScopedValue 绑定租户上下文，执行后续过滤器
    TenantContext.runWithTenant(tenantId, () -> {
        chain.doFilter(request, response)
    })
    // 作用域结束，租户上下文自动清理
END FUNCTION
```

### resolveTenantId 解析流程

```pseudocode
FUNCTION resolveTenantId(request)
    // 1. 优先从 Header 解析
    tenantId = parseTenantIdFromHeader(request)
    IF tenantId IS NOT NULL THEN
        RETURN tenantId
    END IF

    // 2. Header 不存在或无效，从 Session 读取
    tenantId = parseTenantIdFromSession()
    RETURN tenantId  // 可能为 null
END FUNCTION
```

### parseTenantIdFromHeader 解析流程

```pseudocode
FUNCTION parseTenantIdFromHeader(request)
    headerValue = request.getHeader("X-Tenant-Id")

    // 空白检查
    IF headerValue IS NULL OR headerValue.isBlank() THEN
        RETURN NULL
    END IF

    // 解析数字
    TRY
        tenantId = Long.parseLong(headerValue.trim())
        LOG DEBUG "Resolved tenantId: {} from header", tenantId
        RETURN tenantId
    CATCH NumberFormatException
        LOG WARN "Invalid X-Tenant-Id header value: {}", headerValue
        RETURN NULL
    END TRY
END FUNCTION
```

### parseTenantIdFromSession 解析流程

```pseudocode
FUNCTION parseTenantIdFromSession()
    // 检查是否登录
    IF NOT StpUtil.isLogin() THEN
        RETURN NULL
    END IF

    // 读取 Session 中的 tenantId
    sessionTenantId = StpUtil.getSession().get("tenantId")
    IF sessionTenantId IS NULL THEN
        RETURN NULL
    END IF

    // 转换为 Long
    TRY
        tenantId = Long.parseLong(sessionTenantId.toString())
        LOG DEBUG "Resolved tenantId: {} from session", tenantId
        RETURN tenantId
    CATCH NumberFormatException
        LOG WARN "Invalid tenantId in session: {}", sessionTenantId
        RETURN NULL
    END TRY
END FUNCTION
```

---

## 与 F03-04 的集成

### TenantContext 接口调用

TenantContextFilter 调用 F03-04 提供的 package-private 方法：

```java
// F03-04 TenantContext 提供
static void runWithTenant(Long tenantId, Runnable runnable)

// F03-05 TenantContextFilter 调用
TenantContext.runWithTenant(tenantId, () -> {
    filterChain.doFilter(request, response);
});
```

**关键点：**
- Filter 和 TenantContext 在同一个包（`com.cartisan.security.context`）
- 可以访问 package-private 的 `runWithTenant()` 方法
- 使用 ScopedValue 作用域模式，无需手动清理

---

## 日志规范

### 日志级别

| 场景 | 级别 | 消息格式 |
|------|------|---------|
| Header 解析成功 | DEBUG | `Resolved tenantId: {} from header` |
| Session 解析成功 | DEBUG | `Resolved tenantId: {} from session` |
| Header 格式错误 | WARN | `Invalid X-Tenant-Id header value: {}` |
| Session 格式错误 | WARN | `Invalid tenantId in session: {}` |

### 日志配置建议

生产环境通常配置为 INFO 级别，DEBUG 日志不会输出，不影响性能。

---

## 依赖关系

### 当前 Feature 依赖

| 依赖 | 状态 | 说明 |
|------|------|------|
| F03-01（模块骨架） | ✅ 完成 | cartisan-security 模块已创建 |
| F03-04（TenantContext） | ✅ 完成 | 提供 `runWithTenant()` 方法 |

### 当前 Feature 被依赖

| 被依赖 | 说明 |
|--------|------|
| F03-07（AutoConfiguration） | 自动注册 Filter |
| F03-08（集成测试） | 验证 Filter 行为 |

---

## 技术要点

### 1. 为什么使用 @Component 而非 @Configuration + @Bean

| 方案 | 优点 | 缺点 |
|------|------|------|
| `@Component` | 简单，自动扫描注册 | 无法条件装配 |
| `@Configuration + @Bean` | 支持条件装配 | 需要额外配置类 |

**决策：** 使用 `@Component` + `@Order`，简单直接。条件装配由 F03-07 AutoConfiguration 统一处理（可在此处添加 `@ConditionalOnClass` 等）。

### 2. 为什么 Bean 名称显式指定

避免与其他可能存在的 `tenantContextFilter` Bean 冲突，使用模块前缀 `cartisanTenantContextFilter`。

参考 SKILL.md TOOL-007：`@Component` 默认 bean 名称可能与自动配置冲突。

### 3. 异常处理策略

Filter 不应该抛出未捕获的异常：
- Header/Session 解析失败：记录日志，返回 `null`
- 租户 ID 为 `null` 时仍正常执行：`runWithTenant(null, runnable)` 直接执行任务
- 后续过滤器链异常正常传播（由 GlobalExceptionHandler 处理）

### 4. Virtual Threads 兼容

通过 F03-04 的 `ScopedValue` 机制自动兼容 Virtual Threads，无需额外处理。

---

## 后续工作（不在本 Feature 范围）

1. **F03-07 AutoConfiguration**
   - 条件装配：`@ConditionalOnClass(StpUtil.class)`
   - Filter 注册顺序优化

2. **F03-08 集成测试**
   - MockMvc 测试 Header 解析
   - MockMvc 测试 Session 解析
   - 测试 Filter 执行顺序

---

## 参考

- Epic Backlog: [00_epic_backlog.md](../00_epic_backlog.md)
- Requirement Spec: [01_requirement.md](./01_requirement.md)
- F03-04 Interface: [../F03-04-TenantContext/02_interface.md](../F03-04-TenantContext/02_interface.md)
- SKILL.md: [docs/skills/SKILL.md](../../../../skills/SKILL.md)
- Sa-Token 文档: https://sa-token.cc/doc.html#/
