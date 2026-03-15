# Feature: jOOQ 集成测试 — 实施计划

## 目标复述

为 cartisan-data-query 添加集成测试，验证 jOOQ 查询基础设施的端到端集成。同时为 cartisan-security 添加测试工具类 `TenantTestSupport`，便于集成测试中设置租户上下文。

## 变更范围

| 操作 | 模块 | 文件路径 | 说明 |
|------|------|---------|------|
| 新增 | cartisan-security | `src/main/java/com/cartisan/security/test/TenantTestSupport.java` | 测试工具类 |
| 新增 | cartisan-data-query | `src/test/java/com/cartisan/data/query/JooqIntegrationTest.java` | 集成测试类 |

## 核心流程（伪代码）

### 租户过滤测试流程

```
1. 创建临时表 test_user (id, tenant_id, name)
2. 插入测试数据：tenant_id=100 和 tenant_id=200
3. 用 DSL.tableField 构造 tenant_id 字段引用
4. 用 TenantTestSupport.runWithTenant(100L) 设置租户上下文
5. 执行查询：where(JooqTenantSupport.eqTenantId(tenantIdField))
6. 断言：只返回 tenant_id=100 的数据
7. 清理：DROP TABLE test_user
```

## 原子任务清单

### Step 1: TenantTestSupport 工具类

- **文件**：`cartisan-security/src/main/java/com/cartisan/security/test/TenantTestSupport.java`
- **内容**：
  - 公开的静态方法 `runWithTenant(Long tenantId, Runnable runnable)`
  - 委托给 `TenantContext.runWithTenant()`（package-private）
  - 添加 JavaDoc 说明测试用途
- **验证**：编译通过

### Step 2: JooqIntegrationTest 骨架

- **文件**：`cartisan-data-query/src/test/java/com/cartisan/data/query/JooqIntegrationTest.java`
- **内容**：
  - 继承 `IntegrationTestBase`
  - 添加 `@SpringBootTest` 和 `@ImportAutoConfiguration`
  - 注入 `DSLContext`
  - 创建内部 `TestApp` 配置类
- **验证**：编译通过

### Step 3: DSLContext 简单查询测试

- **文件**：`JooqIntegrationTest.java`
- **内容**：
  - 测试方法：`given_dslContextAutoConfigured_whenExecuteSimpleQuery_thenReturnsResult()`
  - 执行 `dslContext.select(DSL.one()).fetch()`
  - 断言结果正确
- **验证**：测试通过

### Step 4: 租户过滤测试

- **文件**：`JooqIntegrationTest.java`
- **内容**：
  - 测试方法：`given_tenantContextAndTableWithTenantId_whenQueryWithEqTenantId_thenFiltersByTenant()`
  - CREATE TEMP TABLE test_user
  - INSERT 多租户数据
  - 用 `DSL.tableField` 构造字段引用
  - 用 `TenantTestSupport.runWithTenant()` 设置租户
  - 执行带 `JooqTenantSupport.eqTenantId()` 的查询
  - 断言只返回当前租户数据
  - @AfterEach 清理临时表
- **验证**：测试通过

### Step 5: 分页参数测试（可选）

- **文件**：`JooqIntegrationTest.java`
- **内容**：
  - 测试方法：`given_pageQuery_whenQueryWithLimitOffset_thenExecutesSuccessfully()`
  - 创建 PageQuery 实例
  - 执行带 LIMIT/OFFSET 参数的查询
  - 断言查询成功执行
- **验证**：测试通过

## 任务依赖关系

```
Step 1 (TenantTestSupport)
    │
    ├──→ Step 2 (测试骨架)
    │       │
    │       ├──→ Step 3 (简单查询测试)
    │       │
    │       ├──→ Step 4 (租户过滤测试) ← 依赖 Step 1
    │       │
    │       └──→ Step 5 (分页测试，可选)
```

## 预估代码量

| Step | 文件 | 预估行数 |
|------|------|---------|
| 1 | TenantTestSupport.java | 20-30 行 |
| 2 | JooqIntegrationTest.java (骨架) | 30-40 行 |
| 3 | 简单查询测试方法 | 15-20 行 |
| 4 | 租户过滤测试方法 | 40-50 行 |
| 5 | 分页测试方法（可选） | 15-20 行 |
| **合计** | | **120-160 行** |

## 验证方式

### 编译验证
```bash
./gradlew :cartisan-security:compileJava
./gradlew :cartisan-data-query:compileTestJava
```

### 测试执行
```bash
# 单独运行集成测试
./gradlew :cartisan-data-query:test --tests JooqIntegrationTest

# 注意：需要 Docker 环境运行
```
