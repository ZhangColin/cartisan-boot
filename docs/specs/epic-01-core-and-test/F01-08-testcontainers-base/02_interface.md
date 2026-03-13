# Feature: F01-08 — 接口契约

> 版本：v0.1 | 日期：2026-03-13
> 状态：Phase 2 完成

---

## 1. 类/接口清单

| 文件 | 职责 |
|------|------|
| `PostgresTestContainer` | PostgreSQL 16 容器配置，`@ServiceConnection` 自动注入 DataSource |
| `RedisTestContainer` | Redis 7 容器配置，`@ServiceConnection` 自动注入 Redis 连接 |
| `IntegrationTestBase` | 启动容器 + 每个测试方法前清理数据 |
| `ApiTestBase` | 继承 IntegrationTestBase，叠加 MockMvc |

---

## 2. HTTP 接口

无（本 Feature 不提供 HTTP 端点）。

---

## 3. 领域接口描述（伪代码）

### 3.1 PostgresTestContainer

```java
@TestConfiguration(proxyBeanMethods = false)
public final class PostgresTestContainer {

    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    }
}
```

**职责**：
- 提供预配置的 PostgreSQL 16 容器
- 通过 `@ServiceConnection` 自动注入 Spring DataSource 属性
- `@TestConfiguration` 允许业务项目按需 `@Import`

---

### 3.2 RedisTestContainer

```java
@TestConfiguration(proxyBeanMethods = false)
public final class RedisTestContainer {

    @Bean
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis() {
        return new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    }
}
```

**职责**：
- 提供预配置的 Redis 7 容器
- 通过 `@ServiceConnection` 自动注入 Spring Redis 属性

---

### 3.3 IntegrationTestBase

```java
@SpringBootTest
@Import({PostgresTestContainer.class, RedisTestContainer.class})
public abstract class IntegrationTestBase {

    // PostgreSQL 清理 SQL（TRUNCATE ... CASCADE）
    private static final String TRUNCATE_ALL_SQL = """
            DO $$
            DECLARE
                tables TEXT;
            BEGIN
                SELECT string_agg(tablename, ', ') INTO tables
                FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename != 'flyway_schema_history';
            IF tables IS NOT NULL THEN
                EXECUTE 'TRUNCATE TABLE ' || tables || ' CASCADE';
            END IF;
            END $$
            """;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanData() {
        // 清理数据库
        if (jdbcTemplate != null) {
            jdbcTemplate.execute(TRUNCATE_ALL_SQL);
        }
        // 清理 Redis
        if (redisTemplate != null) {
            redisTemplate.getConnectionFactory()
                .getConnection().serverCommands().flushDb();
        }
    }
}
```

**职责**：
- 启动 PostgreSQL + Redis 容器
- 每个测试方法前执行 `cleanData()`
- `required = false` 允许只使用部分容器

**清理策略**：
- PostgreSQL：`TRUNCATE ... CASCADE`（自动处理外键顺序）
- Redis：`FLUSHDB`（清空当前数据库）

---

### 3.4 ApiTestBase

```java
@AutoConfigureMockMvc
public abstract class ApiTestBase extends IntegrationTestBase {

    @Autowired
    protected MockMvc mvc;
}
```

**职责**：
- 继承 `IntegrationTestBase`（获得容器 + 数据清理）
- 叠加 `@AutoConfigureMockMvc`（提供 MockMvc）
- 不封装 MockMvc API，保持薄基类

**使用方式**：
```java
class OrderControllerTest extends ApiTestBase {
    @Test
    void shouldCreateOrder() throws Exception {
        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").exists());
    }
}
```

---

## 4. 数据库变更

无（本 Feature 不创建表）。

---

## 5. 技术决策记录

### 决策 1：使用 @ServiceConnection 而非手动 @DynamicPropertySource

**背景**：
- 传统方式需要写 `@DynamicPropertySource` 手动注入 `spring.datasource.url`、`spring.redis.host` 等属性
- Spring Boot 3.1+ 提供 `@ServiceConnection` 自动识别容器类型并注入对应属性

**理由**：
- `@ServiceConnection` 是 Spring Boot 官方推荐方式
- 自动注入，减少样板代码
- 类型安全，容器变更时自动适配

### 决策 2：TRUNCATE ... CASCADE 清理策略

**背景**：
- 需要在每个测试方法前清理数据
- 表之间有外键约束，直接 truncate 会报错

**理由**：
- `TRUNCATE ... CASCADE` 一次性处理所有表，PostgreSQL 自动处理外键
- 不需要排序，不需要禁用约束
- 比逐表 delete 快得多

### 决策 3：ApiTestBase 不封装 MockMvc API

**背景**：
- 初步设计提供 `get/post/put/delete` 便捷方法
- 方法名与 `MockMvcRequestBuilders` 静态导入冲突（无限递归 bug）

**理由**：
- MockMvc 的 fluent API 已经足够清晰
- 便捷方法覆盖不了所有场景（查询参数、Authorization header、multipart 等）
- 保持薄基类，不膨胀

### 决策 4：不包含 Virtual Threads 验证测试

**背景**：
- SOP 提到"支持 Virtual Threads"

**理由**：
- Testcontainers 1.20+ 声明支持 Virtual Threads
- "不要测试别人的承诺" — 这是 Testcontainers 的契约
- 如有 bug，应报 issue 给 Testcontainers 项目

---

## 6. 依赖清单

| 库 | 版本 | 用途 |
|----|------|------|
| `org.testcontainers:testcontainers-bom` | 1.20.4 | BOM 管理版本 |
| `org.testcontainers:testcontainers` | 1.20.4 | GenericContainer（Redis 用） |
| `org.testcontainers:postgresql` | 1.20.4 | PostgreSQLContainer |
| `org.springframework.boot:spring-boot-testcontainers` | 3.4.0 | @ServiceConnection |
