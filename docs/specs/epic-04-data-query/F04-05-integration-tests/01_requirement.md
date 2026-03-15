# Feature: jOOQ 集成测试

## 背景

cartisan-data-query 模块已完成 F04-01（PageQuery）、F04-02（JooqAutoConfiguration）、F04-03（JooqTenantSupport），但缺少端到端的集成测试验证。

当前状态：
- F04-02 的 `JooqAutoConfigurationTest` 验证了 Bean 创建和方言配置
- F04-03 的 `JooqTenantSupportTest` 只验证了单元行为
- 缺少 `DSLContext` + 真实数据库 + `JooqTenantSupport` 的完整链路测试

## 目标

验证 jOOQ 查询基础设施的端到端集成：
- DSLContext 能正确连接数据库并执行查询
- JooqTenantSupport 与真实数据库配合能正确过滤租户数据
- 分页参数（limit/offset）能正确传递到 SQL

## 范围

### 包含（In Scope）

- **cartisan-security 修改**：新增 `TenantTestSupport` 测试工具类
- **cartisan-data-query 集成测试**：
  - DSLContext 简单查询验证（无表查询）
  - 租户过滤验证（临时表 + DSL.tableField）
  - 分页参数执行验证（可选）

### 不包含（Out of Scope）

- jOOQ 代码生成的验证（留给业务项目）
- 业务表的查询（框架不包含业务表）
- JPA/jOOQ CQRS 共存的完整验证（不涉及事务边界、领域事件）

### 设计约束

- **不依赖生成代码**：使用 `DSL.tableField` 构造字段引用，不使用业务表
- **临时表清理**：测试后清理临时表，避免影响其他测试
- **测试命名**：遵循 `given_{条件}_when_{操作}_then_{预期结果}` 规范

## 验收标准（Acceptance Criteria）

- **AC1**：`TenantTestSupport` 类添加到 cartisan-security，提供 `runWithTenant(Long, Runnable)` 方法
- **AC2**：集成测试 `JooqIntegrationTest` 创建，继承 `IntegrationTestBase`
- **AC3**：`given_dslContextAutoConfigured_whenExecuteSimpleQuery_thenReturnsResult` 测试通过
- **AC4**：`given_tenantContextAndTableWithTenantId_whenQueryWithEqTenantId_thenFiltersByTenant` 测试通过
- **AC5**：临时表在测试后正确清理，不影响其他测试
- **AC6**（可选）：`given_pageQuery_whenQueryWithLimitOffset_thenExecutesSuccessfully` 测试通过

## 约束

- **测试隔离**：每个测试方法前后数据独立，使用 `IntegrationTestBase` 自动清理
- **容器依赖**：测试需要 Docker 环境（PostgreSQL 容器）
- **测试包**：集成测试放在 `src/test/java`，不放入主源码

## 交付物

### cartisan-security

| 文件 | 说明 |
|------|------|
| `src/main/java/com/cartisan/security/test/TenantTestSupport.java` | 测试工具类，提供 `runWithTenant` 静态方法 |

### cartisan-data-query

| 文件 | 说明 |
|------|------|
| `src/test/java/com/cartisan/data/query/JooqIntegrationTest.java` | 集成测试类 |
| `src/test/java/.../F04-05-integration-tests/01_requirement.md` | 本需求规格 |
