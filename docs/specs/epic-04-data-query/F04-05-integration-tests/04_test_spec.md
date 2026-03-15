# Feature: jOOQ 集成测试 — 测试规格

## 测试策略

### 测试范围

| 测试项 | 目的 | 方式 |
|--------|------|------|
| DSLContext 集成 | 验证自动配置、DataSource、方言 | 真实数据库 + Testcontainers |
| 租户过滤 | 验证 JooqTenantSupport + DSLContext + 数据库 | 临时表 + TenantTestSupport |
| 分页参数 | 验证 LIMIT/OFFSET 参数传递 | 原生 SQL 查询 |

### 测试分层

| 层级 | 测试类 | 验证内容 |
|------|--------|---------|
| 单元测试 | JooqTenantSupportTest | 租户条件生成逻辑（mock） |
| 集成测试 | JooqIntegrationTest | 端到端查询 + 租户过滤 + 分页 |
| 架构测试 | CartisanArchRulesTest（复用） | 分层依赖、命名规范 |

## 测试用例清单

### JooqIntegrationTest

#### 1. DSLContext 可用性

**用例ID:** TC01
**方法:** `given_dslContextAutoConfigured_whenExecuteSimpleQuery_thenReturnsResult`

| 场景 | 输入 | 预期输出 |
|------|------|---------|
| DSLContext 已自动配置 | `dslContext.select(DSL.one()).fetch()` | 返回结果集，值为 1 |

#### 2. 租户过滤

**用例ID:** TC02
**方法:** `given_tenantContextAndTableWithTenantId_whenQueryWithEqTenantId_thenFiltersByTenant`

| 场景 | 输入 | 预期输出 |
|------|------|---------|
| 临时表有 2 条数据（tenant_id=100, 200），租户上下文=100 | `TenantTestSupport.runWithTenant(100L, query)` | 只返回 tenant_id=100 的数据 |

**数据准备:**
```sql
CREATE TEMP TABLE test_user (id BIGINT, tenant_id BIGINT, name VARCHAR);
INSERT INTO test_user VALUES (1, 100, 'Alice'), (2, 200, 'Bob');
```

**清理:** `DROP TABLE IF EXISTS test_user`

#### 3. 分页参数执行

**用例ID:** TC03
**方法:** `given_pageQuery_whenQueryWithLimitOffset_thenExecutesSuccessfully`

| 场景 | 输入 | 预期输出 |
|------|------|---------|
| PageQuery(2, 10) → offset=10 | `SELECT 1 LIMIT 10 OFFSET 10` | 查询成功执行，不报错 |

### JooqTenantSupportTest（单元测试）

| 用例ID | 方法 | 验证内容 |
|--------|------|---------|
| TC04 | `givenNoTenantContext_whenEqTenantId_thenReturnsNoCondition` | 无租户时返回 noCondition |
| TC05 | `givenNullField_whenEqTenantId_thenThrowsNullPointerException` | null 参数抛出 NPE |

## 测试执行结果

| 测试类 | 用例数 | 通过 | 失败 | 跳过 |
|--------|--------|------|------|------|
| JooqIntegrationTest | 3 | 3 | 0 | 0 |
| JooqTenantSupportTest | 2 | 2 | 0 | 0 |
| **合计** | **5** | **5** | **0** | **0** |

## 技术决策记录

### API 变更：JooqTenantSupport 参数类型

**日期:** 2026-03-15
**变更:** `eqTenantId()` 参数从 `TableField<?, Long>` 改为 `Field<Long>`

**原因:**
- 原设计使用 `TableField<?, Long>`，适用于生成的代码
- 集成测试中动态创建的字段是 `Field<Long>` 类型
- `TableField` 继承 `Field`，改为 `Field<Long>` 向后兼容

**影响:**
- 业务代码使用生成的 `USER.TENANT_ID`（`TableField`）不受影响
- 动态字段场景（如测试、动态查询）现在也可以使用

### 新增：TenantTestSupport 工具类

**位置:** `cartisan-security/src/main/java/com/cartisan/security/context/TenantTestSupport.java`

**用途:** 为集成测试提供设置租户上下文的便捷方法

**设计:**
- 公开静态方法 `runWithTenant(Long tenantId, Runnable runnable)`
- 委托给 `TenantContext.runWithTenant()`（package-private）
- 与 `TenantContext` 同包，可访问 package-private 方法

## 交叉审查

**审查人:** Claude (编码模型)
**审查范围:** Spec + 代码变更
**结论:** 通过

**审查要点:**
- ✅ 实现符合接口契约
- ✅ 错误处理完整（null 检查）
- ✅ 测试覆盖充分（单元 + 集成）
- ✅ 无并发问题（ScopedValue 隔离）
- ✅ 临时表正确清理
