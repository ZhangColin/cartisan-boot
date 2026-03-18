# Feature: F01-08 — cartisan-test Testcontainers 基类

> 版本：v0.2 | 日期：2026-03-19
> 状态：**已废弃（Removed）**

---

> **决策记录（2026-03-19）**
>
> 本 Feature 已整体移除。理由：
> - Testcontainers 与本地 Docker 环境存在兼容问题，维护成本高
> - 框架层（cartisan-boot）本身不需要集成测试，只需验证纯逻辑
> - 集成测试属于业务项目的责任，由业务项目自行选择方案（H2、外部服务等）
>
> **变更范围：**
> - 删除：`PostgresTestContainer`、`RedisTestContainer`、`IntegrationTestBase`
> - 删除：`cartisan-data-jpa` 中依赖真实数据库的 `AuditingIntegrationTest`
> - 删除：`cartisan-data-query` 中的 `JooqIntegrationTest`
> - 删除：`cartisan-test` 模块的所有 Testcontainers 依赖
> - 保留：ArchUnit 规则、`ApiTestBase`、`FixtureBuilder` 等纯工具类

---

## 背景

业务项目的集成测试需要真实的数据库和 Redis 环境。传统方式依赖本地安装的 PostgreSQL/Redis，导致：
- 环境不一致（本地 Mac vs CI Linux）
- 测试数据污染（多个测试共享同一个数据库）
- CI 配置复杂（需要预装服务）

Testcontainers 通过 Docker 容器提供隔离的测试环境，但每个业务项目都要重复配置容器定义、数据清理、属性注入等逻辑。

---

## 目标

提供预配置的 Testcontainers 基础设施，业务项目继承即可用：

1. **PostgresTestContainer** — PostgreSQL 16 容器预配置
2. **RedisTestContainer** — Redis 7 容器预配置
3. **IntegrationTestBase** — 启动容器 + 自动清理数据
4. **ApiTestBase** — 继承 IntegrationTestBase + MockMvc

---

## 范围

### 包含（In Scope）

- PostgreSQL 16 容器配置（`@ServiceConnection` 自动注入）
- Redis 7 容器配置（`GenericContainer` + `@ServiceConnection`）
- 每个测试方法前自动清理数据（`TRUNCATE ... CASCADE` + `flushDb`）
- MockMvc 集成（`@AutoConfigureMockMvc`）
- Spring Boot 3.4 兼容（使用 `@TestConfiguration` + `@ServiceConnection`）

### 不包含（Out of Scope）

- 数据库 Schema 管理（由业务项目配置 Flyway 或 `ddl-auto`）
- Virtual Threads 兼容性验证（Testcontainers 1.20+ 原生支持，无需测试）
- 其他中间件容器（MongoDB、Kafka 等按需添加）

---

## 验收标准（Acceptance Criteria）

### AC1: 容器自动启动和配置

**Given** 业务项目继承 `IntegrationTestBase`
**When** 运行测试
**Then** PostgreSQL 和 Redis 容器自动启动
**And** Spring DataSource 自动指向容器数据库
**And** Spring Redis 连接自动指向容器 Redis

### AC2: 数据自动清理

**Given** 测试方法 A 向数据库写入数据
**When** 测试方法 A 执行完毕
**Then** 测试方法 B 开始时数据库为空
**And** Redis 也为空

### AC3: API 测试支持

**Given** 业务项目继承 `ApiTestBase`
**When** 使用 `mvc.perform()` 发送 HTTP 请求
**Then** 请求能到达 Controller
**And** 数据库和 Redis 可用

### AC4: 独立容器使用

**Given** 业务项目只需要 Redis，不需要 PostgreSQL
**When** 使用 `@Import(RedisTestContainer.class)`
**Then** 只有 Redis 容器启动
**And** Spring Redis 连接自动配置

### AC5: 容器端口动态分配

**Given** 多个测试并行运行
**When** 容器启动
**Then** 端口动态分配，避免冲突

---

## 约束

| 维度 | 约束 |
|------|------|
| 性能 | 每个测试类启动一次容器（`@Testcontainers` 默认行为） |
| 安全 | 容器仅供测试使用，不暴露到外网 |
| 兼容 | Spring Boot 3.4 + Java 21 |
| 依赖 | Testcontainers 1.20.4 + Spring Boot Testcontainers |

---

## 参考资料

- [Testcontainers 官方文档](https://testcontainers.com/)
- [Spring Boot @ServiceConnection 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.testcontainers)
- 设计文档 4.8 节
- SKILL.md 规则 TOOL-001、TEST-001
