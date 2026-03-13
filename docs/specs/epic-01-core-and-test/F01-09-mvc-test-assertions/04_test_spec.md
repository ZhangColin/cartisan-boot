# Feature: F01-09 — MVC 测试断言辅助类 — 测试规格与归档

> Epic: 01-core-and-test
> 版本：v0.1 | 日期：2026-03-14
> 状态：Phase 5 审查与归档

---

## 测试策略

### 测试分层

| 测试层 | 工具 | 覆盖内容 |
|--------|------|---------|
| 单元测试 | JUnit 5 + AssertJ | ApiTestAssertions 所有公开方法 |
| MockMvc 测试 | MockMvc + @WebMvcTest | 断言方法与真实 MockMvc 集成 |

### 测试覆盖

| 方法 | 测试方法 | 场景 |
|------|---------|------|
| assertOk() | shouldAssertOk_whenResponseIsSuccess | HTTP 200 + code=200 |
| assertError(int) | shouldAssertError_whenResponseHasErrorCode | 指定错误码 |
| assertData(String, Object) | shouldAssertData_whenDataFieldMatches | 简单值 |
| | shouldAssertData_whenDataFieldIsString | 字符串值 |
| | shouldAssertData_whenNestedPath | 嵌套路径 |
| assertNotFound() | shouldAssertNotFound_whenCodeIs404 | 语义缩写 404 |
| assertBadRequest() | shouldAssertBadRequest_whenCodeIs400 | 语义缩写 400 |
| assertForbidden() | shouldAssertForbidden_whenCodeIs403 | 语义缩写 403 |
| toJson(Object) | shouldSerializeObjectToJson | 对象序列化为 JSON |
| withToken(String) | shouldAddAuthorizationHeader_whenWithToken | Header 添加验证 |
| withTenantId(long) | shouldAddTenantIdHeader_whenWithTenantId | Header 添加验证 |

### 测试工具

| 组件 | 用途 |
|------|------|
| TestController | Mock Controller 返回固定 JSON |
| TestConfiguration | 测试配置类，排除不需要的自动配置 |
| TestDto | 测试用 DTO |

---

## 测试用例清单

### TC1: assertOk 断言成功响应
**前置条件：** MockMvc 已注入
**输入：** GET /test/success
**预期：** 断言通过（HTTP 200 + code=200）

### TC2: assertError 断言错误码
**前置条件：** MockMvc 已注入
**输入：** GET /test/error, code=500
**预期：** 断言通过（code=500）

### TC3: assertData 断言数据字段
**前置条件：** MockMvc 已注入
**输入：** GET /test/success, path="id", value=1
**预期：** 断言通过（$.data.id == 1）

### TC4: assertNotFound 语义缩写
**前置条件：** MockMvc 已注入
**输入：** GET /test/notFound
**预期：** 断言通过（code=404）

### TC5: assertBadRequest 语义缩写
**前置条件：** MockMvc 已注入
**输入：** GET /test/badRequest
**预期：** 断言通过（code=400）

### TC6: assertForbidden 语义缩写
**前置条件：** MockMvc 已注入
**输入：** GET /test/forbidden
**预期：** 断言通过（code=403）

### TC7: toJson JSON 序列化
**前置条件：** TestDto 对象已创建
**输入：** TestDto("test-name", 123)
**预期：** 返回 {"name":"test-name","value":123}

### TC8: withToken 添加 Authorization Header
**前置条件：** token 字符串已提供
**输入：** token="test-token-123"
**预期：** 返回非空 RequestPostProcessor

### TC9: withTenantId 添加 X-Tenant-Id Header
**前置条件：** tenantId 已提供
**输入：** tenantId=42
**预期：** 返回非空 RequestPostProcessor

---

## 验收标准验证

| AC | 验证方法 | 结果 |
|----|---------|------|
| AC1: 响应断言方法 | 单元测试覆盖所有方法 | ✅ PASS |
| AC2: 语义缩写方法 | 3 个语义方法有独立测试 | ✅ PASS |
| AC3: 请求辅助方法 | toJson / withToken / withTenantId 有测试 | ✅ PASS |
| AC4: 依赖约束 | 无 cartisan-web 导入，build.gradle.kts 无该依赖 | ✅ PASS |
| AC5: 单元测试 | 11 个测试方法，全部通过 | ✅ PASS |

---

## 设计决策记录

### 决策 1: 使用 ResultMatcher 而非 ResultActions

**原规格设计：** 方法接受 `ResultActions` 参数并返回 `ResultActions`

**实际实现：** 方法返回 `ResultMatcher`，用于 `.andExpect()`

**理由：**
- `ResultMatcher` 更符合 MockMvc 的习惯用法
- 使用 `.andExpect(ApiTestAssertions.assertOk())` 比 `.andDo(ApiTestAssertions::assertOk)` 更清晰
- 与 MockMvc 的 `MockMvcResultMatchers` 风格一致

**影响：** 接口规格文档已更新以反映实际设计

---

## 已知限制

### 1. ObjectMapper 使用默认配置

**描述：** `ObjectMapper` 使用默认构造函数，未配置日期序列化等选项

**影响：** 如果测试需要序列化日期类型，可能需要额外处理

**缓解措施：** 未来可添加显式配置，当前按需扩展

---

## 代码质量指标

| 指标 | 值 |
|------|-----|
| 测试覆盖方法数 | 9 个公开方法 |
| 测试用例数 | 11 个 |
| 测试通过率 | 100% |
| 代码审查评分 | 85/100 |
| ArchUnit 违规 | 0 |

---

## 后续改进建议

1. **添加错误场景测试** - 测试序列化失败、断言失败等场景
2. **ObjectMapper 配置** - 添加常用配置（NON_NULL、日期格式等）
3. **TestController DTO 化** - 使用 record 代替 Map 提高类型安全

---

## 变更日志

| 日期 | 变更内容 |
|------|---------|
| 2026-03-14 | Phase 5 完成，生成测试规格文档 |
