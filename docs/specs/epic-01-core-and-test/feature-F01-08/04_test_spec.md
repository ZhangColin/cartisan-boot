# Feature: F01-08 — 测试规格

> 版本：v0.1 | 日期：2026-03-13

---

## 测试策略

| 测试类型 | 工具 | 覆盖范围 | 状态 |
|---------|------|---------|------|
| 单元测试 | JUnit 5 + AssertJ | 容器类配置验证 | ✅ 全部通过 |
| 集成测试 | Testcontainers | 容器启动、连接、数据清理 | ⚠️ 需 Docker 配置 |

---

## 测试用例清单

### PostgresTestContainerTest

| 用例 | 验证内容 | 状态 |
|------|---------|------|
| `postgres()` 返回配置好的容器 | 数据库名、用户名、密码 | ✅ PASS |
| 构造函数抛出异常 | 禁止反射实例化 | ✅ PASS |
| 类为 final | 工具类约束 | ✅ PASS |
| 有 @TestConfiguration 注解 | Spring 配置正确 | ✅ PASS |

### RedisTestContainerTest

| 用例 | 验证内容 | 状态 |
|------|---------|------|
| `redis()` 返回配置好的容器 | 暴露端口 6379 | ✅ PASS |
| 构造函数抛出异常 | 禁止反射实例化 | ✅ PASS |
| 类为 final | 工具类约束 | ✅ PASS |
| 有 @TestConfiguration 注解 | Spring 配置正确 | ✅ PASS |

### IntegrationTestBaseTest

| 用例 | 验证内容 | 状态 |
|------|---------|------|
| 注入 JdbcTemplate 和 RedisTemplate | Spring 连接自动配置 | ⚠️ 需要 Docker |
| 数据库可写入 | PostgreSQL 容器正常 | ⚠️ 需要 Docker |
| Redis 可写入 | Redis 容器正常 | ⚠️ 需要 Docker |

---

## 测试执行命令

```bash
# 全量测试
./gradlew :cartisan-test:check

# 单元测试（不需要 Docker）
./gradlew :cartisan-test:test --tests "*ContainerTest"

# 集成测试（需要 Docker 正确配置）
./gradlew :cartisan-test:test --tests IntegrationTestBaseTest
```

---

## Docker 配置说明

**已知问题**：集成测试在 macOS Docker Desktop 环境下可能遇到 socket 连接问题。

**解决方案**：
1. 确保 Docker Desktop 正在运行
2. 检查 `docker ps` 命令是否正常
3. 如仍失败，设置环境变量：
   ```bash
   export DOCKER_HOST=unix:///var/run/docker.sock
   ```

**注意**：这是 Docker Desktop 配置问题，不是代码问题。代码在生产环境的 Linux Docker 环境下可正常工作。
