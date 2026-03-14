# Feature: F02-03 — RequestContext 实施计划

> **注意**: 本文档定义原子任务清单。执行顺序必须严格遵守。

---

## 目标复述

为 cartisan-web 模块添加请求上下文能力：
1. 实现 `RequestContext` 类，基于 ThreadLocal 存储 requestId 和 clientIp
2. 实现 `RequestContextFilter`，在每个请求生命周期中初始化和清理上下文
3. requestId 支持链路追踪（Header 读取或 UUID 生成）
4. clientIp 支持代理场景（XFF → X-Real-IP → RemoteAddr）
5. 容错设计：初始化失败不影响业务

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-web/src/main/java/com/cartisan/web/context/RequestContext.java` | 请求上下文类（final） |
| 新增 | `cartisan-web/src/main/java/com/cartisan/web/context/RequestContextFilter.java` | Filter 实现 |
| 新增 | `cartisan-web/src/main/java/com/cartisan/web/context/package-info.java` | 包文档（可选） |
| 新增 | `cartisan-web/src/test/java/com/cartisan/web/context/RequestContextTest.java` | 单元测试 |
| 新增 | `cartisan-web/src/test/java/com/cartisan/web/context/RequestContextFilterTest.java` | Filter 单元测试 |
| 新增 | `cartisan-web/src/test/java/com/cartisan/web/context/RequestContextFilterIntegrationTest.java` | 集成测试 |

---

## 核心流程（伪代码）

### RequestContext 初始化流程

```
Filter.doFilterInternal():
  try:
    requestId = extractRequestId()     // X-Request-Id 或 UUID
    clientIp = extractClientIp()       // XFF → X-Real-IP → RemoteAddr
    RequestContext.init(requestId, clientIp)
    chain.doFilter()
  finally:
    RequestContext.clear()
```

### requestId 提取

```
extractRequestId(request):
  header = request.getHeader("X-Request-Id")
  if (header != null && header.trim().length() > 0):
    return header.trim()
  return UUID.randomUUID().toString()
```

### clientIp 提取

```
extractClientIp(request):
  // 1. X-Forwarded-For
  xff = request.getHeader("X-Forwarded-For")
  if (xff != null && xff.trim().length() > 0):
    return xff.split(",")[0].trim()

  // 2. X-Real-IP
  realIp = request.getHeader("X-Real-IP")
  if (realIp != null && realIp.trim().length() > 0):
    return realIp.trim()

  // 3. RemoteAddr
  return request.getRemoteAddr()
```

---

## 原子任务清单

### Step 1: 契约代码化

**目标**: 将 02_interface.md 中的接口描述转为 Java 源代码骨架

- **文件**: `cartisan-web/src/main/java/com/cartisan/web/context/RequestContext.java`
- **内容**:
  - 类声明（final）
  - ThreadLocal 字段
  - requestId / clientIp 字段
  - 私有构造函数
  - 静态方法签名（getRequestId, getClientIp, init, clear）
  - JavaDoc（含前置/后置条件）
- **验证**: `./gradlew :cartisan-web:compileJava` 通过

**预估代码量**: 40-60 行

---

### Step 2: RequestContext 单元测试（红灯）

- **文件**: `cartisan-web/src/test/java/com/cartisan/web/context/RequestContextTest.java`
- **内容**:
  - 测试命名遵循 `given_*_when_*_then_*` 格式
  - 覆盖场景：
    - `given_initContext_when_getRequestId_then_returnsInitializedValue`
    - `given_noContext_when_getRequestId_then_returnsNull`
    - `given_initContext_when_clear_then_getReturnsNull`
    - `given_multipleThreads_when_concurrentAccess_then_noInterference`
- **验证**: 编译通过 + 测试全红（实现类只有空实现）

**预估代码量**: 60-80 行

---

### Step 3: RequestContext 实现（绿灯）

- **文件**: `cartisan-web/src/main/java/com/cartisan/web/context/RequestContext.java`
- **内容**:
  - 实现 `getRequestId()`: 返回 CONTEXT.get().requestId（null 安全）
  - 实现 `getClientIp()`: 返回 CONTEXT.get().clientIp（null 安全）
  - 实现 `init()`: 创建实例并存入 ThreadLocal
  - 实现 `clear()`: ThreadLocal.remove()
- **验证**: `./gradlew :cartisan-web:test` 测试全绿

**预估代码量**: 30-50 行

---

### Step 4: RequestContextFilter 契约代码化

- **文件**: `cartisan-web/src/main/java/com/cartisan/web/context/RequestContextFilter.java`
- **内容**:
  - 类声明（extends OncePerRequestFilter）
  - @Component 和 @Order 注解
  - 常量定义（HEADER_*）
  - doFilterInternal 方法签名
  - private 方法签名（extractRequestId, extractClientIp）
  - JavaDoc
- **验证**: `./gradlew :cartisan-web:compileJava` 通过

**预估代码量**: 50-70 行

---

### Step 5: RequestContextFilter 单元测试（红灯）

- **文件**: `cartisan-web/src/test/java/com/cartisan/web/context/RequestContextFilterTest.java`
- **内容**:
  - Mock HttpServletRequest/Response/FilterChain
  - 覆盖场景：
    - `given_noRequestIdHeader_when_extractRequestId_then_generatesUuid`
    - `given_requestIdHeader_when_extractRequestId_then_returnsHeaderValue`
    - `given_blankRequestIdHeader_when_extractRequestId_then_generatesUuid`
    - `given_xffHeader_when_extractClientIp_then_returnsFirstIp`
    - `given_noProxyHeaders_when_extractClientIp_then_returnsRemoteAddr`
    - `given_filterExecutes_when_clear_then_contextCleared`
- **验证**: 编译通过 + 测试全红

**预估代码量**: 100-130 行

---

### Step 6: RequestContextFilter 实现（绿灯）

- **文件**: `cartisan-web/src/main/java/com/cartisan/web/context/RequestContextFilter.java`
- **内容**:
  - 实现 `doFilterInternal()`: try-finally 结构
  - 实现 `extractRequestId()`: Header → UUID 逻辑
  - 实现 `extractClientIp()`: XFF → X-Real-IP → RemoteAddr
  - 添加异常处理和日志
- **验证**: `./gradlew :cartisan-web:test` 单元测试全绿

**预估代码量**: 80-120 行

---

### Step 7: 集成测试

- **文件**: `cartisan-web/src/test/java/com/cartisan/web/context/RequestContextFilterIntegrationTest.java`
- **内容**:
  - 使用 MockMvc 验证 Filter 链路
  - 覆盖场景：
    - 正常请求后能获取到 requestId
    - 多并发请求互不干扰
    - 异常场景下请求仍成功返回
- **验证**: `./gradlew :cartisan-web:test` 全绿

**预估代码量**: 80-100 行

---

### Step 8: 全量验证与 ArchUnit

- **执行**:
  ```bash
  ./gradlew :cartisan-web:check
  ```
- **验证**:
  - 所有测试通过
  - ArchUnit 规则通过（如有的话）
  - 代码无 warning

---

## 任务依赖关系

```
Step 1 ───→ Step 2 ───→ Step 3
                    │
                    ▼
Step 4 ───→ Step 5 ───→ Step 6 ───→ Step 7 ───→ Step 8
```

- Step 1-3: RequestContext 的 TDD 循环
- Step 4-6: RequestContextFilter 的 TDD 循环
- Step 7: 集成验证
- Step 8: 全量验证

---

## 预估总代码量

| 类型 | 行数 |
|------|------|
| 生产代码 | 150-230 行 |
| 测试代码 | 240-310 行 |
| **合计** | **390-540 行** |

---

## 执行注意事项

1. **严格遵守顺序**：必须先写测试（红灯），再写实现（绿灯）
2. **原子任务边界**：每个 Step 提交一次，便于回滚
3. **测试命名**：遵循 `given_*_when_*_then_*` 格式
4. **null 安全**：所有 getter 必须处理 null 情况
5. **清理验证**：务必验证 ThreadLocal 被正确清理
