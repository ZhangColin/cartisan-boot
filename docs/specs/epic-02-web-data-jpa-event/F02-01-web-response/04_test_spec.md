# Feature: F02-01 cartisan-web 响应体 — 测试规格与归档

> **实现日期**: 2026-03-14
> **对应需求**: [01_requirement.md](./01_requirement.md)
> **对应接口**: [02_interface.md](./02_interface.md)
> **对应实施**: [03_implementation.md](./03_implementation.md)

---

## 测试策略

### 测试分层

| 测试层 | 工具 | 覆盖内容 | 结果 |
|--------|------|---------|------|
| 单元测试 | JUnit 5 + AssertJ | ApiResponse/PageResponse 所有工厂方法和构造器 | ✅ 10/10 通过 |
| 回归测试 | 复用现有测试 | cartisan-core 现有功能未受影响 | ✅ 117/117 通过 |

### 测试覆盖率

| 模块 | 测试类 | 测试方法 | 覆盖场景 |
|------|--------|---------|---------|
| cartisan-core | BaseCodeMessageTest | +1 (shouldHaveSuccessEnum) | SUCCESS 枚举值验证 |
| cartisan-web | ApiResponseTest | 7 | 所有工厂方法 + 泛型类型安全 |
| cartisan-web | PageResponseTest | 3 | 构造器 + 空列表 + 泛型类型 |

**总计**: 127 个测试全绿

---

## 最终测试用例清单

### ApiResponseTest (cartisan-web)

| # | 测试方法 | 验证内容 | 对应 AC |
|---|---------|---------|---------|
| 1 | `given_data_when_ok_then_return_success_response_with_data` | ok(T data) 返回正确字段 | AC2 |
| 2 | `given_noData_when_ok_then_return_success_response_without_data` | ok() 返回 data=null | AC2 |
| 3 | `given_codeMessage_when_error_then_return_error_response` | error(CodeMessage) 映射 | AC3 |
| 4 | `given_codeMessage_and_args_when_error_then_return_parameterized_error_response` | MessageFormat 格式化 | AC4 |
| 5 | `given_codeMessage_and_emptyArgs_when_error_then_return_error_response_with_original_message` | 空 args 使用原 message | AC4 |
| 6 | `given_customCode_and_message_when_error_then_return_custom_error_response` | 自定义 code/message | AC5 |
| 7 | `given_differentTypeData_when_ok_then_support_generic_type_inference` | 泛型类型推导 | AC7 |

### PageResponseTest (cartisan-web)

| # | 测试方法 | 验证内容 | 对应 AC |
|---|---------|---------|---------|
| 1 | `given_pageData_when_construct_then_return_page_response` | 构造器设置所有字段 | AC6 |
| 2 | `given_emptyList_when_construct_then_support_empty_items` | 空列表支持 | AC6 |
| 3 | `given_differentTypeData_when_construct_then_support_generic_types` | 泛型类型支持 | AC7 |

### BaseCodeMessageTest (cartisan-core)

| # | 测试方法 | 验证内容 | 对应 AC |
|---|---------|---------|---------|
| 1 | `shouldHaveSuccessEnum_withCorrectValues` | SUCCESS 枚举值 | AC1 |

---

## 交叉审查

### 审查信息

| 项目 | 内容 |
|------|------|
| 审查人/模型 | feature-dev:code-reviewer (agentId: a3c2f3c83af16d89f) |
| 审查范围 | git diff 286e103..910b09b |
| 审查日期 | 2026-03-14 |
| 结论 | Ready to proceed: Yes |

### 审查发现的问题

| 优先级 | 问题 | 状态 |
|--------|------|------|
| **Important** | 测试命名不符合 SKILL.md TEST-002 规范 | ✅ 已修复 |
| Minor | 01_requirement.md 中 AC4 描述不一致（String.format vs MessageFormat） | ✅ 已修复 |

### 审查结论摘要

**优点**：
- 完美遵循 TDD 流程（先红灯后绿灯）
- 所有验收标准 AC1-AC8 全部满足
- 正确选择 MessageFormat 保持与 BaseCodeMessage 一致性
- 模块依赖正确（只依赖 cartisan-core，无 Spring Web）

**需要修复**：
- 测试方法命名改为 given_when_then 格式（已修复）
- 文档描述更新为 MessageFormat（已修复）

---

## 设计决策记录

本 Feature 引入或影响的设计决策：

### 决策 1: MessageFormat vs String.format

**背景**: 初期计划使用 `String.format`（%s 风格），但发现现有 `BaseCodeMessage` 使用 `MessageFormat`（{0} 风格）。

**决策**: 使用 `MessageFormat.format()` 以保持一致性。

**理由**:
1. `CartisanException` 和 `BaseCodeMessage` 已采用 MessageFormat
2. 避免不兼容性
3. MessageFormat 是 Java 标准库的国际化方案

**影响**: 需要同步更新 02_interface.md 和 03_implementation.md

---

## 完成检查清单

### 验收标准 (AC)

- [x] AC1: BaseCodeMessage.SUCCESS 补充完成
- [x] AC2: ApiResponse.ok() 成功响应正确
- [x] AC3: ApiResponse.error(CodeMessage) 错误响应正确
- [x] AC4: ApiResponse.error(CodeMessage, Object...) 参数化消息正确（MessageFormat）
- [x] AC5: ApiResponse.error(int, String) 自定义错误正确
- [x] AC6: PageResponse 构造器正确
- [x] AC7: 泛型类型安全验证通过
- [x] AC8: 模块依赖正确（无 Spring Web 依赖）

### 质量门禁

- [x] 所有测试绿灯 (127/127)
- [x] ArchUnit 通过（cartisan-core 现有规则）
- [x] 交叉审查已执行且结论已留痕
- [x] 01/02/03/04 文档齐全且与代码一致
- [x] DECISIONS.md 无需新增（MessageFormat 决策已在 02 中记录）
- [x] SKILL.md 无需新增（无新的踩坑经验）

### 代码提交

- [x] 第一次提交: feat(web): add ApiResponse and PageResponse (910b09b)
- [x] 第二次提交: fix(test): 修复 F02-01 代码审查发现的问题 (a44a01c)

---

## 后续集成点

本 Feature 的交付物将在以下 Feature 中被使用：

| Feature | 集成方式 |
|---------|---------|
| F02-02（异常处理） | GlobalExceptionHandler 返回 `ApiResponse.error(...)` |
| F02-03（请求上下文） | Controller/Advice 从 RequestContext 读取 requestId 并传入 |
| F02-09（自动配置） | 统一 Jackson 序列化配置（如命名策略） |

---

## 归档总结

**F02-01 cartisan-web 响应体** 已完成所有 Phase 1-5 流程，交付物包括：

1. **代码**:
   - `BaseCodeMessage.SUCCESS` 枚举值
   - `ApiResponse<T>` Record（5 个工厂方法）
   - `PageResponse<T>` Record
   - 10 个单元测试

2. **文档**:
   - 01_requirement.md（需求规格）
   - 02_interface.md（接口契约）
   - 03_implementation.md（实施计划）
   - 04_test_spec.md（测试规格与归档，本文件）

3. **质量保证**:
   - 127 个测试全绿
   - 交叉审查通过
   - TDD 流程严格执行
