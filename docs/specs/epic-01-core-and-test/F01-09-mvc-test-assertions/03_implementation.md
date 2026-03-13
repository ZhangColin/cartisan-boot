# Feature: F01-09 — MVC 测试断言辅助类 — 实施计划

> Epic: 01-core-and-test
> 版本：v0.1 | 日期：2026-03-13
> 状态：Phase 3 实施计划

---

## 目标复述

新增 `ApiTestAssertions` 工具类，为 MockMvc API 测试提供：
1. 响应断言方法（`assertOk`, `assertError`, `assertData`）及语义缩写
2. JSON 序列化辅助（`toJson`）
3. 请求 Header 辅助（`withToken`, `withTenantId`）

保持 `cartisan-test` 对 `cartisan-web` 零依赖，使用 JsonPath 动态解析 `ApiResponse<T>` 格式的 JSON 响应。

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-test/src/main/java/com/cartisan/test/base/ApiTestAssertions.java` | 工具类实现 |
| 新增 | `cartisan-test/src/test/java/com/cartisan/test/base/ApiTestAssertionsTest.java` | 单元测试 |
| 新增 | `cartisan-test/src/test/java/com/cartisan/test/base/TestController.java` | 测试用 Mock Controller |
| 修改 | `cartisan-test/src/main/java/com/cartisan/test/base/package-info.java` | 添加类描述 |
| 不变 | `cartisan-test/src/main/java/com/cartisan/test/base/ApiTestBase.java` | 保持薄基类 |

---

## 核心流程（伪代码）

```
1. 创建 ApiTestAssertions 类
   - 定义 private constructor
   - 创建静态 ObjectMapper 实例
   - 定义常量（Header 名称、JsonPath 前缀）

2. 实现响应断言方法
   - assertOk(result):
       result.andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
       return result

   - assertError(result, code):
       result.andExpect(jsonPath("$.code").value(code))
       return result

   - assertData(result, path, value):
       result.andExpect(jsonPath("$.data." + path).value(value))
       return result

3. 实现语义缩写方法
   - assertNotFound(result) = assertError(result, 404)
   - assertBadRequest(result) = assertError(result, 400)
   - assertForbidden(result) = assertError(result, 403)

4. 实现请求辅助方法
   - toJson(body) = objectMapper.writeValueAsString(body)

   - withToken(token) = RequestPostProcessor:
       request.addHeader("Authorization", "Bearer " + token)

   - withTenantId(id) = RequestPostProcessor:
       request.addHeader("X-Tenant-Id", String.valueOf(id))

5. 编写单元测试
   - 创建 TestController 返回固定 JSON
   - 验证每个断言方法效果
   - 验证 Header 正确添加
```

---

## 原子任务清单

### Step 1: 创建 ApiTestAssertions 类骨架

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/base/ApiTestAssertions.java`
- **内容**:
  - 定义 final 类，private constructor
  - 创建 `private static final ObjectMapper OBJECT_MAPPER`
  - 定义常量：`AUTHORIZATION_HEADER`, `TENANT_ID_HEADER`, `BEARER_PREFIX`
- **验证**: 编译通过

---

### Step 2: 编写测试用 TestController

- **文件**: `cartisan-test/src/test/java/com/cartisan/test/base/TestController.java`
- **内容**:
  - 创建 Mock Controller 返回固定 JSON
  - Endpoint: `GET /test/success` → `{"code":200,"data":{"id":1}}`
  - Endpoint: `GET /test/notFound` → `{"code":404}`
  - Endpoint: `GET /test/badRequest` → `{"code":400}`
- **验证**: 编译通过

---

### Step 3: 编写响应断言测试（红灯）

- **文件**: `cartisan-test/src/test/java/com/cartisan/test/base/ApiTestAssertionsTest.java`
- **内容**:
  - `shouldAssertOk_whenResponseIsSuccess()` — 验证 assertOk
  - `shouldAssertError_whenResponseHasErrorCode()` — 验证 assertError
  - `shouldAssertData_whenDataFieldMatches()` — 验证 assertData
  - `shouldAssertNotFound_whenCodeIs404()` — 验证 assertNotFound
  - `shouldAssertBadRequest_whenCodeIs400()` — 验证 assertBadRequest
  - `shouldAssertForbidden_whenCodeIs403()` — 验证 assertForbidden
- **验证**: 编译通过，测试全红（方法不存在）

---

### Step 4: 实现响应断言方法（绿灯）

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/base/ApiTestAssertions.java`
- **内容**:
  - `public static ResultActions assertOk(ResultActions result)`
  - `public static ResultActions assertError(ResultActions result, int code)`
  - `public static ResultActions assertData(ResultActions result, String path, Object value)`
  - `public static ResultActions assertNotFound(ResultActions result)`
  - `public static ResultActions assertBadRequest(ResultActions result)`
  - `public static ResultActions assertForbidden(ResultActions result)`
- **验证**: 编译通过 + Step 3 测试全绿 + ArchUnit 通过

---

### Step 5: 编写请求辅助方法测试（红灯）

- **文件**: 扩展 `ApiTestAssertionsTest.java`
- **内容**:
  - `shouldSerializeObjectToJson()` — 验证 toJson
  - `shouldAddAuthorizationHeader_whenWithToken()` — 验证 withToken
  - `shouldAddTenantIdHeader_whenWithTenantId()` — 验证 withTenantId
- **验证**: 编译通过，测试全红

---

### Step 6: 实现请求辅助方法（绿灯）

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/base/ApiTestAssertions.java`
- **内容**:
  - `public static String toJson(Object body)`
  - `public static RequestPostProcessor withToken(String token)`
  - `public static RequestPostProcessor withTenantId(long tenantId)`
- **验证**: 编译通过 + Step 5 测试全绿 + ArchUnit 通过

---

### Step 7: 更新 package-info.java

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/base/package-info.java`
- **内容**: 添加 `ApiTestAssertions` 类描述
- **验证**: 编译通过

---

### Step 8: 全量验证

- **命令**:
  - `./gradlew :cartisan-test:compileJava`
  - `./gradlew :cartisan-test:test`
  - `./gradlew :cartisan-test:check`
- **验证**: 全部通过

---

## 任务规模评估

| Step | 预估代码行数 | 预估时间 |
|------|-------------|---------|
| Step 1 | ~30 行 | 5 分钟 |
| Step 2 | ~40 行 | 10 分钟 |
| Step 3 | ~80 行 | 15 分钟 |
| Step 4 | ~60 行 | 15 分钟 |
| Step 5 | ~40 行 | 10 分钟 |
| Step 6 | ~30 行 | 10 分钟 |
| Step 7 | ~5 行 | 2 分钟 |
| Step 8 | 验证 | 5 分钟 |
| **总计** | **~285 行** | **~72 分钟** |

---

## AC 映射

| AC | 对应 Step |
|----|----------|
| AC1: 响应断言方法 | Step 3, Step 4 |
| AC2: 语义缩写方法 | Step 3, Step 4 |
| AC3: 请求辅助方法 | Step 5, Step 6 |
| AC4: 依赖约束 | 无需验证（设计保证） |
| AC5: 单元测试 | Step 2, Step 3, Step 5 |

---

## 参考资料

- 需求文档: `01_requirement.md`
- 接口设计: `02_interface.md`
- Jackson ObjectMapper 文档
- Spring Test MockMvc 文档
