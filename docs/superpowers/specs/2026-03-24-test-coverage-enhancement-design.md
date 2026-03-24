# 测试覆盖增强设计文档

> **版本**：v1.0 | **日期**：2026-03-24
> **目标**：将整体测试覆盖率从当前水平提升至 90%+，分支覆盖率至 85%+

---

## 一、现状分析

### 1.1 当前覆盖率

| 模块 | 指令覆盖 | 分支覆盖 | 状态 |
|------|----------|----------|------|
| cartisan-core | 99% | 88% | ✅ 优秀 |
| cartisan-event | 100% | n/a | ✅ 完美 |
| cartisan-security | 92% | 88% | ✅ 良好 |
| cartisan-ai | 89% | 67% | ⚠️ 需改进 |
| cartisan-web | 88% | 77% | ⚠️ 需改进 |
| cartisan-data-query | 57% | 90% | ❌ 最低 |
| cartisan-data-jpa | 67% | 60% | ❌ 最低 |

### 1.2 核心问题

1. **cartisan-data-jpa**（67%）：
   - `ConditionSpecifications` 11 种查询类型未全覆盖
   - `BaseRepositoryImpl` 事件发布、软删除逻辑分支缺失
   - `CartisanDataJpaAutoConfiguration` 0% 覆盖（正常，AutoConfiguration 通过集成测试验证）

2. **cartisan-data-query**（57%）：
   - `JooqTenantSupport` 租户上下文处理分支未覆盖
   - `JooqAutoConfiguration` 0% 覆盖（正常）

3. **cartisan-ai**（89% 指令 / 67% 分支）：
   - `SseHelper` 超时、异常、客户端断开等边界情况未覆盖

4. **cartisan-web**（88% 指令 / 77% 分支）：
   - `TreeNodeBuilder` 边界条件（空列表、循环引用等）未测试

---

## 二、设计方案

### 2.1 测试策略

遵循项目编码规范：
- 使用 AssertJ 断言，禁止无意义断言
- 测试命名：`shouldX_whenY` 风格
- 使用 `@TestConfiguration` 提供测试专用配置
- 集成测试使用 `Testcontainers` 或 `@SpringBootTest`

### 2.2 测试清单

#### P0 - 核心业务逻辑

| # | 测试类 | 模块 | 测试内容 |
|---|--------|------|----------|
| 1 | `ConditionSpecificationsTest` | cartisan-data-jpa | 11 种查询类型 + 嵌套路径 + 异常处理 |
| 2 | `BaseRepositoryImplEventPublishingTest` | cartisan-data-jpa | save() 事件发布逻辑 |
| 3 | `BaseRepositoryImplSoftDeleteTest` | cartisan-data-jpa | 软删除 markAsDeleted() 调用 |

#### P1 - 重要功能

| # | 测试类 | 模块 | 测试内容 |
|---|--------|------|----------|
| 4 | `JooqTenantSupportTest` | cartisan-data-query | 有/无租户上下文 |
| 5 | `SseHelperTest` | cartisan-ai | 超时、异常、客户端断开、usage 回调 |
| 6 | `TreeNodeBuilderTest` | cartisan-web | 空列表、单节点、多层级 |

#### P2 - 边界情况

| # | 测试类 | 模块 | 测试内容 |
|---|--------|------|----------|
| 7 | `ResubmitAspectTest` | cartisan-web | AOP 切面、Redis 锁 |
| 8 | `AutoResponseAdviceTest` | cartisan-web | 自动响应包装、String 特殊处理 |

---

## 三、测试设计细节

### 3.1 ConditionSpecificationsTest

**测试场景**：

| 场景 | 说明 |
|------|------|
| 相等性查询 | EQUAL、NOT_EQUAL |
| 大小比较 | GREATER、GREATER_EQUAL、LESS、LESS_EQUAL |
| 模糊查询 | INNER_LIKE、LEFT_LIKE、RIGHT_LIKE |
| 集合查询 | IN（Collection、Array） |
| 区间查询 | BETWEEN（List、Array、无效长度） |
| 多字段模糊 | blurry 属性（逗号分隔字段） |
| 嵌套路径 | `user.profile.name` 格式 |
| @Condition 注解 | fromAnnotation() 方法 |
| 异常处理 | 无效字段名、无效类型 |

**测试方式**：单元测试，使用 Mockito 模拟 `CriteriaBuilder` 和 `Root`

### 3.2 BaseRepositoryImplEventPublishingTest

**测试场景**：

| 场景 | 说明 |
|------|------|
| save() 新建实体 | 发布注册的事件 |
| save() 更新实体 | 清空旧事件，发布新事件 |
| save() 无事件 | 不发布 |

**测试方式**：集成测试，使用 `@DataJpaTest` + `TestEntityManager`

### 3.3 BaseRepositoryImplSoftDeleteTest

**测试场景**：

| 场景 | 说明 |
|------|------|
| delete(软删除实体) | 调用 markAsDeleted() + save() |
| delete(普通实体) | 物理删除 |
| deleteAll() | 混合处理 |

**测试方式**：集成测试，使用 `@DataJpaTest`

### 3.4 JooqTenantSupportTest

**测试场景**：

| 场景 | 说明 |
|------|------|
| 有租户上下文 | 返回 `tenantIdField.eq(tenantId)` |
| 无租户上下文 | 返回 `DSL.noCondition()` |

**测试方式**：单元测试，模拟 `TenantContext`

### 3.5 SseHelperTest

**测试场景**：

| 场景 | 说明 |
|------|------|
| 正常流式发送 | 事件逐个发送 |
| 超时处理 | onTimeout 回调 |
| 异常处理 | onError 回调 |
| 客户端断开 | doOnCancel 回调 |
| usage 回调 | 流结束时调用 |

**测试方式**：单元测试，使用 `StepVerifier` 测试 Reactor Flux

### 3.6 TreeNodeBuilderTest

**测试场景**：

| 场景 | 说明 |
|------|------|
| 空列表 | 返回空列表 |
| 单节点 | 返回单根节点 |
| 多层级 | 正确构建父子关系 |
| 循环引用 | 处理循环（或抛异常） |

**测试方式**：单元测试

---

## 四、不测试的内容

### 4.1 AutoConfiguration 类

以下类 0% 覆盖是**正常现象**，不需要单独测试：

- `CartisanDataJpaAutoConfiguration`
- `JooqAutoConfiguration`
- `CartisanWebAutoConfiguration`
- `CartisanSecurityAutoConfiguration`
- `CartisanEventAutoConfiguration`

**理由**：
- Spring Boot 的 `@AutoConfiguration` 通过 `@AutoConfigurationMetadata` 验证
- 功能在各模块的集成测试中已间接验证
- Spring Boot 本身会验证 Bean 是否正确加载

### 4.2 已有充分测试的功能

- `PageQuery`（100% 覆盖）
- `DomainEvent` / `DomainEventPublisher`（100% 覆盖）
- `SecurityContext` / `TenantContext`（92% 覆盖）
- `ChatMessage` / `ChatRequest` / `ChatResponse`（100% 覆盖）

---

## 五、验收标准

| 指标 | 目标 |
|------|------|
| cartisan-data-jpa 指令覆盖 | ≥ 85% |
| cartisan-data-query 指令覆盖 | ≥ 80% |
| cartisan-ai 分支覆盖 | ≥ 80% |
| cartisan-web 分支覆盖 | ≥ 85% |
| 整体指令覆盖 | ≥ 90% |

---

## 六、实施计划

执行 `writing-plans` 技能创建详细实现计划。
