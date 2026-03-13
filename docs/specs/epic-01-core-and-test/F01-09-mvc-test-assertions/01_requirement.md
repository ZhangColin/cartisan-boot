# Feature: F01-09 — MVC 测试断言辅助类

> Epic: 01-core-and-test
> 版本：v0.1 | 日期：2026-03-13
> 状态：Phase 1 需求澄清

---

## 背景

F01-08 已完成 `IntegrationTestBase` 和 `ApiTestBase` 基类，提供了 Testcontainers 容器和 MockMvc 注入能力。但业务项目在编写 Controller 层测试时，仍需重复编写针对 `ApiResponse<T>` 格式的断言代码。

cartisan-boot 约定所有 API 返回统一的 `ApiResponse<T>` 格式（code、message、data、requestId）。每个测试都需要断言这些字段，产生样板代码。

**目标：** 提供测试辅助类，消除 API 测试中的重复断言代码。

---

## 目标

1. 新增 `ApiTestAssertions` 辅助类，提供响应断言和请求辅助方法
2. 保持 `cartisan-test` 对 `cartisan-web` 的零依赖，使用 JsonPath 动态解析
3. 提供语义化的断言方法（如 `assertNotFound()`）提升测试可读性

---

## 范围

### 包含（In Scope）

| 类别 | 内容 |
|------|------|
| **响应断言** | `assertOk()`, `assertError(int)`, `assertData(path, value)` |
| **语义缩写** | `assertNotFound()`, `assertBadRequest()`, `assertForbidden()` |
| **JSON 序列化** | `toJson(Object)` 简化请求体构造 |
| **请求辅助** | `withToken(String)`, `withTenantId(long)` 返回 RequestPostProcessor |
| **单元测试** | 验证所有断言方法正常工作 |

### 不包含（Out of Scope）

| 内容 | 原因 |
|------|------|
| 请求方法封装（get/post/put/delete） | MockMvc fluent API 已足够简洁 |
| JSON 反序列化断言 | 打断流式调用，JsonPath 已够用 |
| 通用 Header 辅助 | MockMvc 原生 `.header(name, value)` 无需再封装 |
| `ApiTestBase` 修改 | 保持薄基类设计，不改动 |

---

## 验收标准（Acceptance Criteria）

### AC1: 响应断言方法
- [ ] `assertOk(ResultActions)` 断言 HTTP 200 且 `$.code == 200`
- [ ] `assertError(ResultActions, int)` 断言 `$.code == 指定值`
- [ ] `assertData(ResultActions, String, Object)` 断言 `$.data.{path} == value`
- [ ] 所有断言方法返回 `ResultActions`，支持流式调用

### AC2: 语义缩写方法
- [ ] `assertNotFound()` 等价于 `assertError(404)`
- [ ] `assertBadRequest()` 等价于 `assertError(400)`
- [ ] `assertForbidden()` 等价于 `assertError(403)`

### AC3: 请求辅助方法
- [ ] `toJson(Object)` 使用 Jackson 序列化为 JSON 字符串
- [ ] `withToken(String)` 添加 `Authorization: Bearer {token}` Header
- [ ] `withTenantId(long)` 添加 `X-Tenant-Id: {id}` Header

### AC4: 依赖约束
- [ ] `ApiTestAssertions` 不依赖 `cartisan-web` 模块
- [ ] 使用 JsonPath 动态解析，不依赖 `ApiResponse` 类

### AC5: 单元测试
- [ ] 每个公开方法都有对应测试
- [ ] 使用 MockMvc 验证断言行为
- [ ] 测试覆盖正常场景和边界场景

---

## 约束

### 技术约束
- 使用 JsonPath 解析响应（`org.springframework.test.web.servlet.ResultActions` 的 `jsonPath`）
- Jackson `ObjectMapper` 用于 JSON 序列化
- 工具类模式：private constructor，静态方法

### 架构约束
- 包路径：`com.cartisan.test.base`
- 依赖：仅依赖 `cartisan-core` 和 Spring Boot Test

### 设计原则
- 零依赖 `cartisan-web`，使用 JsonPath 动态解析
- 保持流式调用，所有断言方法返回 `ResultActions`

---

## 使用示例

```java
class OrderControllerTest extends ApiTestBase {

    @Test
    void shouldGetOrderById() throws Exception {
        mvc.perform(get("/api/orders/1"))
            .andExpect(status().isOk())
            .andDo(ApiTestAssertions::assertOk)
            .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        mvc.perform(get("/api/orders/999"))
            .andExpect(status().isOk())
            .andDo(r -> ApiTestAssertions.assertNotFound(r));
    }

    @Test
    void shouldCreateOrder() throws Exception {
        String json = ApiTestAssertions.toJson(new CreateOrderRequest("O001"));
        mvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(json)
                .with(ApiTestAssertions.withToken("test-token"))
                .with(ApiTestAssertions.withTenantId(1L)))
            .andExpect(status().isOk())
            .andDo(ApiTestAssertions::assertOk);
    }
}
```

---

## 参考资料

- 设计文档：`docs/cartisan-boot-设计文档.md` 第 4.4 节（cartisan-security）定义了 `X-Tenant-Id` Header
- 设计文档：`docs/cartisan-boot-设计文档.md` 第 4.2 节（cartisan-web）定义了 `ApiResponse<T>` 格式
- Epic Backlog：`docs/specs/epic-01-core-and-test/00_epic_backlog.md` F01-09
