# Feature: F01-08 — 测试规格

> 版本：v0.2 | 日期：2026-03-14

---

## 测试策略

| 测试类型 | 工具 | 覆盖范围 | 状态 |
|---------|------|---------|------|
| 单元测试 | JUnit 5 + AssertJ | 容器类配置验证 | ✅ 全部通过 |
| 集成测试 | Testcontainers | 容器启动、连接、数据清理 | ✅ 全部通过 |

---

## 测试用例清单

### PostgresTestContainerTest

| 用例 | 验证内容 | 状态 |
|------|---------|------|
| `postgres()` 返回配置好的容器 | 数据库名、用户名、密码 | ✅ PASS |
| 有 @TestConfiguration 注解 | Spring 配置正确 | ✅ PASS |

> **注意**：已移除"构造函数抛出异常"和"类为 final"测试。@TestConfiguration 类不能使用工具类模式（私有构造函数 + final），因为 Spring 需要实例化配置类。详见 SKILL.md TOOL-004。

### RedisTestContainerTest

| 用例 | 验证内容 | 状态 |
|------|---------|------|
| `redis()` 返回配置好的容器 | 暴露端口 6379 | ✅ PASS |
| 有 @TestConfiguration 注解 | Spring 配置正确 | ✅ PASS |

### IntegrationTestBaseTest

| 用例 | 验证内容 | 状态 |
|------|---------|------|
| 注入 JdbcTemplate 和 RedisTemplate | Spring 连接自动配置 | ✅ PASS |
| 数据库可写入 | PostgreSQL 容器正常 | ✅ PASS |
| Redis 可写入 | Redis 容器正常 | ✅ PASS |

### DataCleanupTest

| 用例 | 验证内容 | 状态 |
|------|---------|------|
| 测试间数据隔离 | @BeforeEach 清理生效 | ✅ PASS |
| PostgreSQL 清理 | TRUNCATE ... CASCADE 生效 | ✅ PASS |
| Redis 清理 | FLUSHDB 生效 | ✅ PASS |

### ApiTestBaseTest

| 用例 | 验证内容 | 状态 |
|------|---------|------|
| MockMvc 注入 | @AutoConfigureMockMvc 生效 | ✅ PASS |
| HTTP 请求可发送 | MockMvc 正常工作 | ✅ PASS |
| 继承关系 | 继承 IntegrationTestBase | ✅ PASS |

---

## 测试执行命令

```bash
# 全量测试
./gradlew :cartisan-test:check

# 单元测试（不需要 Docker）
./gradlew :cartisan-test:test --tests "*ContainerTest"

# 集成测试（需要 Docker）
./gradlew :cartisan-test:test --tests IntegrationTestBaseTest
```

---

## Docker 配置说明

**版本要求**：Testcontainers 1.21.4+ 兼容 Docker Engine 29 / Docker Desktop 4.59+。

**验证 Docker 环境**：
```bash
docker ps
```

**集成测试验证**：
```bash
# 查看容器启动日志
./gradlew :cartisan-test:test --info | grep "Container is started"
# 应输出：Container postgres:16-alpine started in PT<X>s
```

**已知问题**：Testcontainers 1.20.x 及以下版本与 Docker Engine 29 不兼容，会报 "Could not find a valid Docker environment" 错误。解决方案：升级到 Testcontainers 1.21.4+。详见 ADR-029。
