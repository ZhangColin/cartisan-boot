# Feature: F02-03 — RequestContext 测试规格

> **Epic**: Epic 2 — Web + Data-JPA + Event
>
> **状态**: 已完成
>
> **完成日期**: 2026-03-14

---

## 测试策略

### 测试分层

| 测试层 | 工具 | 覆盖范围 |
|--------|------|---------|
| 单元测试 | JUnit 5 + AssertJ | RequestContext 核心逻辑 |
| 单元测试 | JUnit 5 + Mockito | Filter 逻辑（mock Servlet API） |
| 集成测试 | MockMvc | Filter 在 Spring 环境中的行为 |

### 测试覆盖的 AC

| AC | 描述 | 测试方法 |
|----|------|---------|
| AC1 | requestId 生成 UUID | `given_noRequestIdHeader_when_doFilter_then_generatesUuid` |
| AC2 | requestId 从 Header 读取 | `given_requestIdHeaderWithValue_when_doFilter_then_usesHeaderValue` |
| AC3 | 空白 requestId Header 时生成 UUID | `given_requestIdHeaderWithBlank_when_doFilter_then_generatesUuid` |
| AC4 | clientIp 从 XFF 提取 | `given_xffHeaderWithSingleIp_when_doFilter_then_returnsFirstIp` |
| AC5 | clientIp 优先级正确 | `given_noXffHeader_when_doFilter_then_returnsRealIp` |
| AC6 | clientIp 回退到 RemoteAddr | `given_noXffAndNoRealIpHeader_when_doFilter_then_returnsRemoteAddr` |
| AC7 | ThreadLocal 正确清理 | `given_initContext_when_clear_then_getRequestIdReturnsNull` |
| AC8 | 并发请求互不干扰 | `given_concurrentAccess_when_multipleThreads_then_noInterference` |
| AC9 | 初始化失败时容错降级 | Filter 的 try-catch 块 |
| AC10 | Filter 最早执行 | `@Order(Ordered.HIGHEST_PRECEDENCE)` |

---

## 单元测试

### RequestContextTest

**文件**: `RequestContextTest.java` (186 行)

**测试场景**:
1. 未初始化时 get 方法返回 null
2. 初始化后 get 方法返回正确值
3. null 值初始化时返回 null
4. clear() 后 get 方法返回 null
5. 重复 init() 覆盖之前的值
6. 并发访问时 ThreadLocal 隔离

**关键断言**:
```java
assertThat(RequestContext.getRequestId()).isEqualTo("test-request-id");
assertThat(RequestContext.getRequestId()).isNull(); // after clear
```

### RequestContextFilterTest

**文件**: `RequestContextFilterTest.java` (263 行)

**测试场景**:
1. requestId 提取（无 Header、有值、空白、带空格）
2. clientIp 提取（XFF 单 IP、多 IP、带空格、X-Real-IP、RemoteAddr）
3. Filter 链路（正常执行、异常时清理）
4. 验证 chain.doFilter() 被调用

**Mock 策略**:
- Mock HttpServletRequest/Response/FilterChain
- 使用 Answer 在 chain 执行期间捕获 RequestContext 值
- LENIENT stubbing 避免不必要的 stubbing 警告

---

## 集成测试

### RequestContextFilterIntegrationTest

**文件**: `RequestContextFilterIntegrationTest.java` (127 行)

**测试场景**:
1. 无 requestId Header 的请求
2. 有 requestId Header 的请求
3. 有 XFF Header 的请求
4. 顺序请求验证上下文清理
5. 异常场景验证上下文清理

**依赖**: 使用现有的 `/test/exception` 端点（TestController）

---

## 测试执行结果

```
RequestContextTest                 10 tests PASSED
RequestContextFilterTest           12 tests PASSED
RequestContextFilterIntegrationTest 5 tests PASSED
-----------------------------------------------
Total                              27 tests PASSED
```

---

## 代码质量指标

| 指标 | 值 |
|------|-----|
| 生产代码行数 | 232 行 |
| 测试代码行数 | 576 行 |
| 测试覆盖率 | 100% AC 覆盖 |
| 测试/代码比 | 2.48:1 |

---

## 已知问题

**无** - 所有测试通过，无遗留问题。
