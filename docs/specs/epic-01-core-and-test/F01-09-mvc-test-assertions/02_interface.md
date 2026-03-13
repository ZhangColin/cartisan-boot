# Feature: F01-09 — MVC 测试断言辅助类 — 接口契约

> Epic: 01-core-and-test
> 版本：v0.1 | 日期：2026-03-13
> 状态：Phase 2 接口设计

---

## 1. 类设计

### 1.1 ApiTestAssertions

**职责：** 提供 MockMvc 测试的断言和请求辅助方法

**类型：** Final 工具类（private constructor，静态方法）

**包路径：** `com.cartisan.test.base`

### 1.2 依赖关系

```
ApiTestAssertions
    └── Spring Test: MockMvc, ResultActions, RequestPostProcessor
    └── Spring Test: JsonPath (MockMvc 的 jsonPath() 方法)
    └── Jackson: ObjectMapper (JSON 序列化)
    └── cartisan-core: 无需依赖
    └── cartisan-web: 零依赖（关键约束）
```

---

## 2. 方法签名描述（伪代码）

### 2.1 响应断言方法

#### assertOk() → ResultMatcher

**描述：** 断言响应为成功状态

**前置条件：** 无

**后置条件：**
- 验证 HTTP status 为 200
- 验证 JSON 响应中 `$.code == 200`

**异常：** 无

**伪代码：**
```
assertOk():
    return ResultMatcher {
        status().isOk().match(result)
        jsonPath("$.code").value(200).match(result)
    }
```

**设计说明：** 返回 `ResultMatcher` 而非 `ResultActions`，更符合 MockMvc 的 `andExpect()` 习惯用法。

---

#### assertError(int code) → ResultMatcher

**描述：** 断言响应包含指定的错误码

**前置条件：** 无

**后置条件：**
- 验证 JSON 响应中 `$.code == code`

**异常：** 无

**伪代码：**
```
assertError(code):
    return ResultMatcher {
        jsonPath("$.code").value(code).match(result)
    }
```

---

#### assertData(String path, Object value) → ResultMatcher

**描述：** 断言响应 data 字段中指定路径的值

**前置条件：**
- `path` 为有效的 JsonPath 表达式（不含 `$.data.` 前缀）
- `value` 非空

**后置条件：**
- 验证 JSON 响应中 `$.data.{path} == value`

**异常：** 无

**伪代码：**
```
assertData(path, value):
    fullPath = "$.data." + path
    return ResultMatcher {
        jsonPath(fullPath).value(value).match(result)
    }
```

---

### 2.2 语义缩写方法

#### assertNotFound() → ResultMatcher

**描述：** 断言响应为 404 错误

**实现：** 返回 `assertError(404)`

---

#### assertBadRequest() → ResultMatcher

**描述：** 断言响应为 400 错误

**实现：** 返回 `assertError(400)`

---

#### assertForbidden() → ResultMatcher

**描述：** 断言响应为 403 错误

**实现：** 返回 `assertError(403)`

---

### 2.3 请求辅助方法

#### toJson(Object body) → String

**描述：** 将对象序列化为 JSON 字符串

**前置条件：**
- `body` 可序列化（Jackson 支持）

**后置条件：**
- 返回 JSON 格式字符串

**异常：**
- `JsonProcessingException` — 序列化失败

**伪代码：**
```
toJson(body):
    return objectMapper.writeValueAsString(body)
```

---

#### withToken(String token) → RequestPostProcessor

**描述：** 创建添加 Bearer Token 的请求处理器

**前置条件：**
- `token` 非空

**后置条件：**
- 返回的 RequestPostProcessor 会在请求中添加 `Authorization: Bearer {token}` Header

**异常：** 无

**伪代码：**
```
withToken(token):
    return request -> {
        request.addHeader("Authorization", "Bearer " + token)
        return request
    }
```

---

#### withTenantId(long tenantId) → RequestPostProcessor

**描述：** 创建添加租户 ID 的请求处理器

**前置条件：**
- `tenantId > 0`

**后置条件：**
- 返回的 RequestPostProcessor 会在请求中添加 `X-Tenant-Id: {tenantId}` Header

**异常：** 无

**伪代码：**
```
withTenantId(tenantId):
    return request -> {
        request.addHeader("X-Tenant-Id", String.valueOf(tenantId))
        return request
    }
```

---

## 3. 数据结构

### 3.1 ApiResponse JSON 格式（用于断言参考）

虽然不依赖 `ApiResponse` 类，但断言基于以下 JSON 结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 响应码，200 表示成功 |
| message | String | 响应消息 |
| data | T | 响应数据，可为对象或数组 |
| requestId | String | 全链路追踪 ID |

**示例响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "Order 1"
  },
  "requestId": "req-123"
}
```

---

## 4. 常量定义

| 常量 | 值 | 说明 |
|------|-----|------|
| AUTHORIZATION_HEADER | "Authorization" | Bearer Token Header 名称 |
| TENANT_ID_HEADER | "X-Tenant-Id" | 租户 ID Header 名称（来自设计文档 4.4 节） |
| BEARER_PREFIX | "Bearer " | Bearer Token 前缀 |
| CODE_PATH | "$.code" | JsonPath：响应码 |
| DATA_PATH_PREFIX | "$.data." | JsonPath：data 字段前缀 |

---

## 5. 异常处理

| 场景 | 处理方式 |
|------|---------|
| toJson 序列化失败 | 抛出 JsonProcessingException（unchecked，由测试处理） |
| assertXxx 断言失败 | 抛出 AssertionError（MockMvc 原生行为） |
| 传入 null 参数 | 由 MockMvc 或 Jackson 自然抛出 NPE，无需额外校验 |

---

## 6. 设计决策

### 决策 1：零依赖 cartisan-web

**选择：** 使用 JsonPath 动态解析，不依赖 `ApiResponse` 类

**理由：**
- `cartisan-test` 只依赖 `cartisan-core`，不依赖 `cartisan-web`
- 测试关心的是 JSON 结构，而非 Java 类型
- 模块间依赖更清晰

**备选方案（未采纳）：**
- 在 test 模块定义 ApiResponse 接口 → 违反 DRY，存在两个定义
- 先实现 cartisan-web → 改变 Epic 依赖顺序

---

### 决策 2：工具类模式

**选择：** Private constructor + 静态方法

**理由：**
- 无状态工具，无需实例化
- 使用简单：`ApiTestAssertions.assertOk(result)`
- 符合 Java 工具类惯例

---

### 决策 3：断言方法返回 ResultActions

**选择：** 所有断言方法返回传入的 `ResultActions`

**理由：**
- 保持流式调用：`.andDo(assertOk).andExpect(...)`
- 与 MockMvc 原生风格一致

---

## 7. 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-test/src/main/java/com/cartisan/test/base/ApiTestAssertions.java` | 工具类 |
| 新增 | `cartisan-test/src/test/java/com/cartisan/test/base/ApiTestAssertionsTest.java` | 单元测试 |
| 修改 | `cartisan-test/src/main/java/com/cartisan/test/base/package-info.java` | 添加类描述 |
| 不变 | `cartisan-test/src/main/java/com/cartisan/test/base/ApiTestBase.java` | 保持薄基类 |

---

## 8. 测试策略

### 8.1 单元测试：ApiTestAssertionsTest

**测试方法覆盖：**

| 方法 | 测试场景 |
|------|---------|
| assertOk | 成功响应（code=200） |
| assertError | 错误响应（code=404, 400, 403 等） |
| assertData | data 字段简单值、嵌套值 |
| assertNotFound | 语义缩写正确委托给 assertError(404) |
| assertBadRequest | 语义缩写正确委托给 assertError(400) |
| assertForbidden | 语义缩写正确委托给 assertError(403) |
| toJson | 正常对象序列化、异常对象序列化 |
| withToken | Header 正确添加 |
| withTenantId | Header 正确添加 |

**测试方式：**
- 使用 MockMvc 的 `perform()` 执行模拟请求
- 使用 MockMvc 的 `andExpect()` 验证断言方法效果
- 需要一个测试 Controller 返回固定 JSON

### 8.2 集成测试：ApiTestBaseIntegrationTest

**验证：** `ApiTestBase` 继承链正常工作，`ApiTestAssertions` 可用

---

## 9. 参考资料

- 设计文档 4.2 节：`ApiResponse<T>` 格式定义
- 设计文档 4.4 节：`X-Tenant-Id` Header 定义
- Spring Test 文档：MockMvc、ResultActions、RequestPostProcessor
- Jackson 文档：ObjectMapper 序列化
