# Feature: F03-05 TenantContextFilter — 实施计划

## 目标复述

实现 HTTP Filter，从请求中解析租户 ID（Header > Session 优先级），通过 `TenantContext.runWithTenant()` 绑定租户上下文，请求结束后自动清理。兼容 Java 21+ Virtual Threads 环境。

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/context/TenantContextFilter.java` | Filter 实现类 |
| 新增 | `cartisan-security/src/test/java/com/cartisan/security/context/TenantContextFilterTest.java` | 单元测试 |

## 核心流程

```pseudocode
FUNCTION doFilter(request, response, chain)
    httpRequest = CAST request TO HttpServletRequest
    tenantId = resolveTenantId(httpRequest)
    TenantContext.runWithTenant(tenantId, () -> {
        chain.doFilter(request, response)
    })
END FUNCTION

FUNCTION resolveTenantId(request)
    tenantId = parseTenantIdFromHeader(request)
    IF tenantId IS NOT NULL THEN RETURN tenantId
    RETURN parseTenantIdFromSession()
END FUNCTION
```

## 原子任务清单

### Step 1: 生成 Filter 实现类（骨架）

- **文件**：`cartisan-security/src/main/java/com/cartisan/security/context/TenantContextFilter.java`
- **内容**：
  - 类声明：`@Component("cartisanTenantContextFilter")` + `@Order` + `final`
  - 实现 `Filter` 接口
  - 日志记录器
  - 空方法：`doFilter()`、`resolveTenantId()`、`parseTenantIdFromHeader()`、`parseTenantIdFromSession()`
- **验证**：编译通过

### Step 2: 编写单元测试（红灯）

- **文件**：`cartisan-security/src/test/java/com/cartisan/security/context/TenantContextFilterTest.java`
- **内容**：基于 AC 编写测试，覆盖：
  - AC1: Header 存在且有效 → 返回租户 ID
  - AC2: Header 不存在，Session 有租户 → 从 Session 读取
  - AC3: 都不存在 → 返回 null
  - AC4: Header 和 Session 都存在 → Header 优先
  - AC5: Header 为空字符串 → 忽略，继续从 Session 读取
  - AC6: Header 格式错误 → 记录 WARN，忽略，继续从 Session 读取
  - AC7: 请求异常 → 上下文仍被清理（由 ScopedValue 保证）
  - AC8: Filter 不抛异常
  - AC9: Virtual Threads 兼容（由 ScopedValue 保证）
  - AC10-11: 日志验证
  - AC12: 执行顺序（通过 `@Order` 注解验证）
- **验证**：编译通过 + 测试全红

### Step 3: 实现 parseTenantIdFromHeader() 方法

- **文件**：`TenantContextFilter.java`
- **内容**：
  - 读取 `X-Tenant-Id` Header
  - 空白检查
  - `Long.parseLong()` 解析
  - 解析成功：DEBUG 日志 + 返回
  - 解析失败：WARN 日志 + 返回 null
- **验证**：相关测试通过

### Step 4: 实现 parseTenantIdFromSession() 方法

- **文件**：`TenantContextFilter.java`
- **内容**：
  - 检查 `StpUtil.isLogin()`
  - 读取 `StpUtil.getSession().get("tenantId")`
  - 转换为 Long
  - 解析成功：DEBUG 日志 + 返回
  - 解析失败：WARN 日志 + 返回 null
- **验证**：相关测试通过

### Step 5: 实现 resolveTenantId() 方法

- **文件**：`TenantContextFilter.java`
- **内容**：
  - 调用 `parseTenantIdFromHeader()`
  - 若返回 null，调用 `parseTenantIdFromSession()`
  - 返回最终结果
- **验证**：相关测试通过

### Step 6: 实现 doFilter() 方法

- **文件**：`TenantContextFilter.java`
- **内容**：
  - 强转 `ServletRequest` 为 `HttpServletRequest`
  - 调用 `resolveTenantId()`
  - 调用 `TenantContext.runWithTenant(tenantId, runnable)`
  - 在 runnable 中调用 `chain.doFilter()`
- **验证**：测试全绿

### Step 7: 验证 ArchUnit 规则

- **命令**：`./gradlew :cartisan-security:test`
- **验证**：ArchUnit 规则通过（如有）

### Step 8: 更新进度标记

- **文件**：本文档
- **内容**：标记各 Step 为 ✅

## 测试策略

### 测试命名规范

遵循 SKILL.md TEST-002：`given_{条件}_when_{操作}_then_{预期结果}`

### 测试场景覆盖

| AC | 测试方法 |
|----|---------|
| AC1 | `given_validHeader_when_resolveTenantId_then_returnTenantId()` |
| AC2 | `given_noHeaderButLoggedIn_when_resolveTenantId_then_returnFromSession()` |
| AC3 | `given_noHeaderAndNotLoggedIn_when_resolveTenantId_then_returnNull()` |
| AC4 | `given_bothHeaderAndSession_when_resolveTenantId_then_headerPriority()` |
| AC5 | `given_emptyHeader_when_resolveTenantId_then_ignoreAndContinue()` |
| AC6 | `given_invalidHeaderFormat_when_resolveTenantId_then_logWarnAndIgnore()` |
| AC7 | 由 ScopedValue 机制保证，无需单独测试 |
| AC8 | `given_anyScenario_when_doFilter_then_noException()` |
| AC9 | 由 ScopedValue 机制保证，无需单独测试 |
| AC10-11 | 通过 Mockito verify 日志调用 |
| AC12 | `given_filterClass_when_checkOrder_then_hasOrderAnnotation()` |

### Mock 策略

- `HttpServletRequest`：Mock Header 读取
- `StpUtil`：Mock `isLogin()` 和 `getSession()`（使用 Mockito 静态 Mock）
- `TenantContext`：使用真实实现（已在 F03-04 实现）

## 风险评估

| 风险 | 缓解措施 |
|------|---------|
| Sa-Token 静态方法难以 Mock | 使用 Mockito 的 `mockStatic()` |
| 日志验证复杂 | 使用 Mockito ArgumentCaptor 或简化验证 |
| ScopedValue 在测试中绑定问题 | 每个测试后检查 `TenantContext.getCurrentTenantId() == null` |

## 预估工作量

| Step | 预估行数 |
|------|---------|
| Step 1 | 20 行 |
| Step 2 | 100 行 |
| Step 3 | 20 行 |
| Step 4 | 20 行 |
| Step 5 | 10 行 |
| Step 6 | 15 行 |
| Step 7 | 0 行（验证） |
| Step 8 | 0 行（文档） |
| **总计** | **~185 行** |

符合复杂度 M 级别（80-200 行）。

## 参考

- Requirement Spec: [01_requirement.md](./01_requirement.md)
- Interface Spec: [02_interface.md](./02_interface.md)
- F03-04 Interface: [../F03-04-TenantContext/02_interface.md](../F03-04-TenantContext/02_interface.md)
- SKILL.md: [docs/skills/SKILL.md](../../../../skills/SKILL.md)
