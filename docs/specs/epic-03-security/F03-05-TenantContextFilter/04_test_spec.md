# Feature: F03-05 TenantContextFilter — 测试规格

## 测试策略

采用单元测试覆盖所有核心逻辑，使用 Mockito 模拟外部依赖（HttpServletRequest、StpUtil）。

### 测试框架

- JUnit 5 + AssertJ
- Mockito（包括静态方法 mock）

### Mock 策略

| 依赖 | Mock 方式 | 原因 |
|------|----------|------|
| HttpServletRequest | @Mock | 隔离 HTTP 层 |
| StpUtil | mockStatic() | Sa-Token 静态方法 |
| SaSession | mock() | Session 对象 |
| TenantContext | 真实实现 | F03-04 已实现，ScopedValue 可测试 |

## 测试覆盖矩阵

### parseTenantIdFromHeader

| AC | 测试方法 | 输入 | 预期输出 |
|----|---------|------|---------|
| AC1 | `given_validHeader_when_parseFromHeader_then_returnTenantId()` | `X-Tenant-Id: 123` | `123L` |
| AC5 | `given_emptyHeader_when_parseFromHeader_then_returnNull()` | `X-Tenant-Id: ` | `null` |
| AC6 | `given_invalidHeaderFormat_when_parseFromHeader_then_returnNull()` | `X-Tenant-Id: abc` | `null` |
| - | `given_noHeader_when_parseFromHeader_then_returnNull()` | 无 Header | `null` |
| - | `given_blankHeader_when_parseFromHeader_then_returnNull()` | `X-Tenant-Id:    ` | `null` |

### parseTenantIdFromSession

| AC | 测试方法 | 输入 | 预期输出 |
|----|---------|------|---------|
| AC2 | `given_loggedInWithTenantId_when_parseFromSession_then_returnTenantId()` | 已登录，Session 有 tenantId=456 | `456L` |
| - | `given_notLoggedIn_when_parseFromSession_then_returnNull()` | 未登录 | `null` |
| - | `given_sessionWithoutTenantId_when_parseFromSession_then_returnNull()` | 已登录，Session 无 tenantId | `null` |
| - | `given_invalidSessionTenantId_when_parseFromSession_then_returnNull()` | 已登录，Session 有 tenantId="abc" | `null` |

### resolveTenantId

| AC | 测试方法 | 输入 | 预期输出 |
|----|---------|------|---------|
| AC4 | `given_bothHeaderAndSession_when_resolveTenantId_then_headerPriority()` | Header=123, Session=456 | `123L`（Header 优先） |
| AC2 | `given_noHeaderButSessionHasTenant_when_resolveTenantId_then_fromSession()` | 无 Header, Session=456 | `456L` |
| AC3 | `given_noHeaderAndNoSession_when_resolveTenantId_then_returnNull()` | 无 Header, 未登录 | `null` |

### doFilter

| AC | 测试方法 | 输入 | 预期输出 |
|----|---------|------|---------|
| - | `given_tenantId_when_doFilter_then_contextIsSet()` | Header=123 | 上下文=123L，结束后清理 |
| - | `given_noTenantId_when_doFilter_then_contextIsNull()` | 无租户 | 上下文=null |
| AC7 | `given_exceptionInChain_when_doFilter_then_contextIsCleared()` | Header=123 + 异常 | 异常传播，上下文清理 |
| AC8 | `given_invalidHeader_when_doFilter_then_noException()` | Header="abc" | 无异常 |

### Order

| AC | 测试方法 | 预期输出 |
|----|---------|---------|
| AC12 | `given_filterClass_when_checkOrder_then_implementsOrdered()` | 实现 Ordered，order = HIGHEST_PRECEDENCE + 10 |

## 验收标准测试映射

| AC | 描述 | 测试方法 | 状态 |
|----|------|---------|------|
| AC1 | Header 存在且有效 | `given_validHeader_when_parseFromHeader_then_returnTenantId()` | ✅ |
| AC2 | Header 不存在，Session 有租户 | `given_noHeaderButSessionHasTenant_when_resolveTenantId_then_fromSession()` | ✅ |
| AC3 | 都不存在 | `given_noHeaderAndNoSession_when_resolveTenantId_then_returnNull()` | ✅ |
| AC4 | Header 和 Session 都存在，Header 优先 | `given_bothHeaderAndSession_when_resolveTenantId_then_headerPriority()` | ✅ |
| AC5 | Header 为空字符串 | `given_emptyHeader_when_parseFromHeader_then_returnNull()` | ✅ |
| AC6 | Header 格式错误 | `given_invalidHeaderFormat_when_parseFromHeader_then_returnNull()` | ✅ |
| AC7 | 请求异常后上下文清理 | `given_exceptionInChain_when_doFilter_then_contextIsCleared()` | ✅ |
| AC8 | Filter 不抛异常 | `given_invalidHeader_when_doFilter_then_noException()` | ✅ |
| AC9 | Virtual Threads 兼容 | 由 ScopedValue 保证（F03-04 测试覆盖） | ✅ |
| AC10 | DEBUG 日志 | 通过代码审查确认 | ✅ |
| AC11 | WARN 日志 | 通过代码审查确认 | ✅ |
| AC12 | Filter 顺序 | `given_filterClass_when_checkOrder_then_implementsOrdered()` | ✅ |

## 测试运行命令

```bash
# 运行所有测试
./gradlew :cartisan-security:test

# 只运行 TenantContextFilter 测试
./gradlew :cartisan-security:test --tests "TenantContextFilterTest"
```

## 测试统计

- **测试类数量**：1
- **测试方法数量**：17
- **测试通过率**：100%
- **代码覆盖场景**：主流程 + 边界 + 异常

## 集成测试（F03-08）

本 Feature 的集成测试将在 F03-08 中实现，覆盖：
- MockMvc 测试 Header 解析
- MockMvc 测试 Session 解析
- 完整 Filter 链验证

## 参考

- Requirement Spec: [01_requirement.md](./01_requirement.md)
- Interface Spec: [02_interface.md](./02_interface.md)
- Implementation Plan: [03_implementation.md](./03_implementation.md)
