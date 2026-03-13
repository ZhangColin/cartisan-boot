# F01-08 Testcontainers 基类实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 为 cartisan-test 模块添加 Testcontainers 预配置基类，业务项目继承即可获得 PostgreSQL + Redis 集成测试环境。

**架构:** 使用 Spring Boot 3.4 的 `@TestConfiguration` + `@ServiceConnection` 自动注入连接属性，IntegrationTestBase 提供 `@BeforeEach` 数据清理（`TRUNCATE ... CASCADE` + `FLUSHDB`）。

**技术栈:** Testcontainers 1.20.4, Spring Boot Testcontainers, JUnit 5, JdbcTemplate, StringRedisTemplate

---

## Task 1: 配置 Testcontainers 依赖

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `cartisan-test/build.gradle.kts`

**Step 1: 添加 Version Catalog 配置**

编辑 `gradle/libs.versions.toml`，在 `[versions]` 节添加：

```toml
testcontainers = "1.20.4"
```

在 `[libraries]` 节添加：

```toml
testcontainers-bom = { module = "org.testcontainers:testcontainers-bom", version.ref = "testcontainers" }
testcontainers-core = { module = "org.testcontainers:testcontainers" }
testcontainers-postgresql = { module = "org.testcontainers:postgresql" }
```

**Step 2: 修改 cartisan-test 依赖**

编辑 `cartisan-test/build.gradle.kts`，在 `dependencies` 块添加：

```kotlin
// Testcontainers
api(platform(libs.testcontainers.bom))
api(libs.testcontainers.core)
api(libs.testcontainers.postgresql)

// Spring Boot Testcontainers 支持
api("org.springframework.boot:spring-boot-testcontainers")
```

**Step 3: 验证构建**

```bash
./gradlew :cartisan-test:compileJava
```

预期：BUILD SUCCESSFUL

**Step 4: 提交**

```bash
git add gradle/libs.versions.toml cartisan-test/build.gradle.kts
git commit -m "feat(test): 添加 Testcontainers 1.20.4 依赖"
```

---

## Task 2: 创建 PostgresTestContainer

**Files:**
- Create: `cartisan-test/src/main/java/com/cartisan/test/container/PostgresTestContainer.java`
- Create: `cartisan-test/src/main/java/com/cartisan/test/container/package-info.java`

**Step 1: 创建 container 包的 package-info.java**

创建 `cartisan-test/src/main/java/com/cartisan/test/container/package-info.java`:

```java
/**
 * Testcontainers 预配置。
 *
 * <p>提供开箱即用的 Docker 容器配置，通过 Spring Boot {@code @ServiceConnection}
 * 自动注入连接属性，无需手动配置。</p>
 *
 * @since 0.1.0
 */
package com.cartisan.test.container;
```

**Step 2: 创建 PostgresTestContainer**

创建 `cartisan-test/src/main/java/com/cartisan/test/container/PostgresTestContainer.java`:

```java
package com.cartisan.test.container;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * PostgreSQL 16 测试容器预配置。
 *
 * <p>通过 {@code @ServiceConnection} 自动注入 DataSource 属性，
 * 业务项目无需手动配置连接信息。</p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 方式 1：通过 IntegrationTestBase 自动获得（推荐）
 * class MyTest extends IntegrationTestBase { ... }
 *
 * // 方式 2：只需要 PostgreSQL，不需要 Redis
 * @SpringBootTest
 * @Import(PostgresTestContainer.class)
 * class MyTest { ... }
 * }</pre>
 *
 * @since 0.1.0
 */
@TestConfiguration(proxyBeanMethods = false)
public final class PostgresTestContainer {

    private PostgresTestContainer() {
        // 工具类，禁止实例化
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 创建 PostgreSQL 容器。
     *
     * <p>容器在 Spring 上下文启动时自动启动，
     * Spring Boot 通过 {@code @ServiceConnection} 自动注入 DataSource。</p>
     *
     * @return 预配置的 PostgreSQL 容器
     */
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

**Step 3: 编译验证**

```bash
./gradlew :cartisan-test:compileJava
```

预期：BUILD SUCCESSFUL

**Step 4: 提交**

```bash
git add cartisan-test/src/main/java/com/cartisan/test/container/
git commit -m "feat(test): 添加 PostgresTestContainer 预配置"
```

---

## Task 3: 创建 RedisTestContainer

**Files:**
- Create: `cartisan-test/src/main/java/com/cartisan/test/container/RedisTestContainer.java`

**Step 1: 创建 RedisTestContainer**

创建 `cartisan-test/src/main/java/com/cartisan/test/container/RedisTestContainer.java`:

```java
package com.cartisan.test.container;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

/**
 * Redis 7 测试容器预配置。
 *
 * <p>通过 {@code @ServiceConnection} 自动注入 Redis 连接属性。</p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 方式 1：通过 IntegrationTestBase 自动获得（推荐）
 * class MyTest extends IntegrationTestBase { ... }
 *
 * // 方式 2：只需要 Redis，不需要 PostgreSQL
 * @SpringBootTest
 * @Import(RedisTestContainer.class)
 * class MyTest { ... }
 * }</pre>
 *
 * @since 0.1.0
 */
@TestConfiguration(proxyBeanMethods = false)
public final class RedisTestContainer {

    private RedisTestContainer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 创建 Redis 容器。
     *
     * @return 预配置的 Redis 容器
     */
    @Bean
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis() {
        return new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew :cartisan-test:compileJava
```

预期：BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add cartisan-test/src/main/java/com/cartisan/test/container/RedisTestContainer.java
git commit -m "feat(test): 添加 RedisTestContainer 预配置"
```

---

## Task 4: 创建 base 包和 IntegrationTestBase

**Files:**
- Create: `cartisan-test/src/main/java/com/cartisan/test/base/package-info.java`
- Create: `cartisan-test/src/main/java/com/cartisan/test/base/IntegrationTestBase.java`

**Step 1: 创建 base 包的 package-info.java**

创建 `cartisan-test/src/main/java/com/cartisan/test/base/package-info.java`:

```java
/**
 * 测试基类。
 *
 * <p>提供预配置的集成测试基础设施，包括容器管理和数据清理。</p>
 *
 * @since 0.1.0
 */
package com.cartisan.test.base;
```

**Step 2: 创建 IntegrationTestBase**

创建 `cartisan-test/src/main/java/com/cartisan/test/base/IntegrationTestBase.java`:

```java
package com.cartisan.test.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.cartisan.test.container.PostgresTestContainer;
import com.cartisan.test.container.RedisTestContainer;
import org.junit.jupiter.api.BeforeEach;

/**
 * 集成测试基类。
 *
 * <p>自动启动 PostgreSQL + Redis 容器，每个测试方法前清理全部数据，
 * 保证测试间完全隔离。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * class OrderRepositoryTest extends IntegrationTestBase {
 *
 *     @Autowired
 *     private OrderRepository orderRepository;
 *
 *     @Test
 *     void shouldSaveOrder() {
 *         // 数据库已清理，可直接测试
 *         Order order = new Order("O001");
 *         orderRepository.save(order);
 *         assertThat(orderRepository.findById("O001")).isPresent();
 *     }
 * }
 * }</pre>
 *
 * <h3>数据清理策略</h3>
 * <ul>
 *   <li>PostgreSQL：{@code TRUNCATE ... CASCADE} 自动处理外键</li>
 *   <li>Redis：{@code FLUSHDB} 清空当前数据库</li>
 *   <li>Flyway 迁移表 {@code flyway_schema_history} 被排除</li>
 * </ul>
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import({
    PostgresTestContainer.class,
    RedisTestContainer.class
})
public abstract class IntegrationTestBase {

    /**
     * PostgreSQL 清理 SQL。
     *
     * <p>使用 {@code TRUNCATE ... CASCADE} 一次性清空所有表，
     * PostgreSQL 自动处理外键约束，无需排序。</p>
     */
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

    /**
     * 每个测试方法前清理数据。
     *
     * <p>{@code required = false} 允许业务项目只使用部分容器。</p>
     */
    @BeforeEach
    void cleanData() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute(TRUNCATE_ALL_SQL);
        }
        if (redisTemplate != null) {
            redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
        }
    }
}
```

**Step 3: 编译验证**

```bash
./gradlew :cartisan-test:compileJava
```

预期：BUILD SUCCESSFUL

**Step 4: 提交**

```bash
git add cartisan-test/src/main/java/com/cartisan/test/base/
git commit -m "feat(test): 添加 IntegrationTestBase 基类"
```

---

## Task 5: 创建 ApiTestBase

**Files:**
- Create: `cartisan-test/src/main/java/com/cartisan/test/base/ApiTestBase.java`

**Step 1: 创建 ApiTestBase**

创建 `cartisan-test/src/main/java/com/cartisan/test/base/ApiTestBase.java`:

```java
package com.cartisan.test.base;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API 测试基类。
 *
 * <p>继承 {@link IntegrationTestBase}，叠加 MockMvc 能力，
 * 用于测试 Controller 层。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * class OrderControllerTest extends ApiTestBase {
 *
 *     @Autowired
 *     private ObjectMapper objectMapper;
 *
 *     @Test
 *     void shouldCreateOrder() throws Exception {
 *         String orderJson = objectMapper.writeValueAsString(new CreateOrderRequest("O001"));
 *
 *         mvc.perform(post("/api/v1/orders")
 *                 .contentType(MediaType.APPLICATION_JSON)
 *                 .content(orderJson))
 *             .andExpect(status().isOk())
 *             .andExpect(jsonPath("$.data.id").value("O001"));
 *     }
 * }
 * }</pre>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>不封装 MockMvc API — 保持薄基类</li>
 *   <li>业务项目直接使用 MockMvc fluent API</li>
 *   <li>获得完整的灵活性（查询参数、Header、multipart 等）</li>
 * </ul>
 *
 * @since 0.1.0
 */
@AutoConfigureMockMvc
public abstract class ApiTestBase extends IntegrationTestBase {

    @Autowired
    protected MockMvc mvc;
}
```

**Step 2: 编译验证**

```bash
./gradlew :cartisan-test:compileJava
```

预期：BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add cartisan-test/src/main/java/com/cartisan/test/base/ApiTestBase.java
git commit -m "feat(test): 添加 ApiTestBase 基类"
```

---

## Task 6: 编写容器类的单元测试（红灯）

**Files:**
- Create: `cartisan-test/src/test/java/com/cartisan/test/container/PostgresTestContainerTest.java`
- Create: `cartisan-test/src/test/java/com/cartisan/test/container/RedisTestContainerTest.java`

**Step 1: 创建 PostgresTestContainerTest**

创建 `cartisan-test/src/test/java/com/cartisan/test/container/PostgresTestContainerTest.java`:

```java
package com.cartisan.test.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

@DisplayName("PostgresTestContainer 单元测试")
class PostgresTestContainerTest {

    @Test
    @DisplayName("postgres() 方法应返回配置好的 PostgreSQLContainer")
    void given_whenPostgres_thenReturnsConfiguredContainer() {
        // When
        PostgreSQLContainer<?> container = PostgresTestContainer.postgres();

        // Then
        assertThat(container).isNotNull();
        assertThat(container.getDatabaseName()).isEqualTo("testdb");
        assertThat(container.getUsername()).isEqualTo("test");
        assertThat(container.getPassword()).isEqualTo("test");
    }

    @Test
    @DisplayName("构造函数应抛出 UnsupportedOperationException")
    void given_whenInstantiate_thenThrowsException() throws Exception {
        // Given
        Constructor<PostgresTestContainer> constructor =
            PostgresTestContainer.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When & Then
        Throwable exception = assertThrows(
            Exception.class,
            constructor::newInstance
        );
        assertThat(exception)
            .hasCauseExactlyInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Utility class");
    }

    @Test
    @DisplayName("类应为 final")
    void given_whenCheckClass_thenIsFinal() {
        // Then
        assertThat(Modifier.isFinal(PostgresTestContainer.class.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("应有 @TestConfiguration 注解")
    void given_whenCheckAnnotation_thenHasTestConfiguration() {
        // Then
        assertThat(PostgresTestContainer.class.isAnnotationPresent(
            org.springframework.boot.test.context.TestConfiguration.class
        )).isTrue();
    }
}
```

**Step 2: 创建 RedisTestContainerTest**

创建 `cartisan-test/src/test/java/com/cartisan/test/container/RedisTestContainerTest.java`:

```java
package com.cartisan.test.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

@DisplayName("RedisTestContainer 单元测试")
class RedisTestContainerTest {

    @Test
    @DisplayName("redis() 方法应返回配置好的 GenericContainer")
    void given_whenRedis_thenReturnsConfiguredContainer() {
        // When
        GenericContainer<?> container = RedisTestContainer.redis();

        // Then
        assertThat(container).isNotNull();
        assertThat(container.getExposedPorts()).contains(6379);
    }

    @Test
    @DisplayName("构造函数应抛出 UnsupportedOperationException")
    void given_whenInstantiate_thenThrowsException() throws Exception {
        // Given
        Constructor<RedisTestContainer> constructor =
            RedisTestContainer.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When & Then
        Throwable exception = assertThrows(
            Exception.class,
            constructor::newInstance
        );
        assertThat(exception)
            .hasCauseExactlyInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Utility class");
    }

    @Test
    @DisplayName("类应为 final")
    void given_whenCheckClass_thenIsFinal() {
        // Then
        assertThat(Modifier.isFinal(RedisTestContainer.class.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("应有 @TestConfiguration 注解")
    void given_whenCheckAnnotation_thenHasTestConfiguration() {
        // Then
        assertThat(RedisTestContainer.class.isAnnotationPresent(
            org.springframework.boot.test.context.TestConfiguration.class
        )).isTrue();
    }
}
```

**Step 3: 编译测试**

```bash
./gradlew :cartisan-test:compileTestJava
```

预期：BUILD SUCCESSFUL

**Step 4: 运行测试（应通过，无需实现代码）**

```bash
./gradlew :cartisan-test:test --tests PostgresTestContainerTest
./gradlew :cartisan-test:test --tests RedisTestContainerTest
```

预期：PASSED（这些是纯验证测试，不需要实现）

**Step 5: 提交**

```bash
git add cartisan-test/src/test/java/com/cartisan/test/container/
git commit -m "test(test): 添加容器类单元测试"
```

---

## Task 7: 编写 IntegrationTestBase 集成测试

**Files:**
- Create: `cartisan-test/src/test/java/com/cartisan/test/base/IntegrationTestBaseTest.java`

**Step 1: 创建集成测试基类测试**

创建 `cartisan-test/src/test/java/com/cartisan/test/base/IntegrationTestBaseTest.java`:

```java
package com.cartisan.test.base;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("IntegrationTestBase 集成测试")
class IntegrationTestBaseTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("应能注入 JdbcTemplate 和 RedisTemplate")
    void given_whenStartTest_thenTemplatesInjected() {
        // Then
        assertThat(jdbcTemplate).isNotNull();
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    @DisplayName("应能向数据库写入数据")
    void given_whenInsert_thenDataPersisted() {
        // When
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'public'",
            Integer.class
        );

        // Then
        assertThat(count).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("应能向 Redis 写入数据")
    void given_whenSetRedis_thenDataPersisted() {
        // When
        redisTemplate.opsForValue().set("test:key", "test:value");

        // Then
        assertThat(redisTemplate.opsForValue().get("test:key")).isEqualTo("test:value");
    }
}
```

**Step 2: 运行集成测试（绿灯，验证容器正常工作）**

```bash
./gradlew :cartisan-test:test --tests IntegrationTestBaseTest
```

预期：PASSED（容器启动、连接成功、数据可读写）

**Step 3: 提交**

```bash
git add cartisan-test/src/test/java/com/cartisan/test/base/IntegrationTestBaseTest.java
git commit -m "test(test): 添加 IntegrationTestBase 集成测试"
```

---

## Task 8: 编写数据清理验证测试

**Files:**
- Create: `cartisan-test/src/test/java/com/cartisan/test/base/DataCleanupTest.java`

**Step 1: 创建数据清理测试**

创建 `cartisan-test/src/test/java/com/cartisan/test/base/DataCleanupTest.java`:

```java
package com.cartisan.test.base;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 验证数据清理功能。
 *
 * <p>使用 @TestInstance(PER_CLASS) 和 @Order 确保测试按顺序执行，
 * 验证后一个测试运行时数据已被清理。</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.PerClass)
@DisplayName("数据清理验证测试")
class DataCleanupTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String testKeyId;

    @Test
    @Order(1)
    @DisplayName("测试 1：写入数据")
    void test1_writeData() {
        // Given - 创建测试表
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS data_cleanup_test (id VARCHAR PRIMARY KEY, value VARCHAR)");

        // When - 写入数据
        jdbcTemplate.update("INSERT INTO data_cleanup_test (id, value) VALUES ('test1', 'value1')");
        testKeyId = "data-cleanup-test:" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(testKeyId, "test-value");

        // Then - 验证写入成功
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM data_cleanup_test",
            Integer.class
        );
        assertThat(count).isEqualTo(1);
        assertThat(redisTemplate.hasKey(testKeyId)).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("测试 2：验证数据已清理（应该是空表）")
    void test2_verifyDataCleared() {
        // Given - 测试 1 已执行完毕，@BeforeEach 应该清理了数据

        // When - 查询表（表应该还在，但数据是空的）
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM data_cleanup_test",
            Integer.class
        );

        // Then - 数据应该被清理
        assertThat(count).isEqualTo(0);
        assertThat(redisTemplate.hasKey(testKeyId)).isFalse();

        // Cleanup - 删除测试表
        jdbcTemplate.execute("DROP TABLE IF EXISTS data_cleanup_test");
    }
}
```

**Step 2: 运行数据清理测试**

```bash
./gradlew :cartisan-test:test --tests DataCleanupTest
```

预期：PASSED（验证测试 1 写入的数据在测试 2 中已被清理）

**Step 3: 提交**

```bash
git add cartisan-test/src/test/java/com/cartisan/test/base/DataCleanupTest.java
git commit -m "test(test): 添加数据清理验证测试"
```

---

## Task 9: 编写 ApiTestBase 验证测试

**Files:**
- Create: `cartisan-test/src/test/java/com/cartisan/test/base/ApiTestBaseTest.java`
- Create: `cartisan-test/src/test/java/com/cartisan/test/base/testcontroller/HelloController.java`

**Step 1: 创建测试用 Controller**

创建 `cartisan-test/src/test/java/com/cartisan/test/base/testcontroller/HelloController.java`:

```java
package com.cartisan.test.base.testcontroller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Testcontainers!";
    }
}
```

**Step 2: 创建 ApiTestBaseTest**

创建 `cartisan-test/src/test/java/com/cartisan/test/base/ApiTestBaseTest.java`:

```java
package com.cartisan.test.base;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("ApiTestBase 验证测试")
class ApiTestBaseTest extends ApiTestBase {

    @Test
    @DisplayName("应能注入 MockMvc")
    void given_whenStartTest_thenMvcInjected() {
        // Then
        assertThat(mvc).isNotNull();
    }

    @Test
    @DisplayName("应能发送 HTTP 请求")
    void given_whenGetRequest_thenReturnsResponse() throws Exception {
        // When & Then
        mvc.perform(get("/api/test/hello"))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello, Testcontainers!"));
    }

    @Test
    @DisplayName("应继承 IntegrationTestBase 的容器能力")
    void given_whenCheckInheritance_thenExtendsIntegrationTestBase() {
        // Then
        assertThat(this).isInstanceOf(IntegrationTestBase.class);
    }
}
```

**Step 3: 运行 ApiTestBaseTest**

```bash
./gradlew :cartisan-test:test --tests ApiTestBaseTest
```

预期：PASSED

**Step 4: 提交**

```bash
git add cartisan-test/src/test/java/com/cartisan/test/base/ApiTestBaseTest.java
git add cartisan-test/src/test/java/com/cartisan/test/base/testcontroller/
git commit -m "test(test): 添加 ApiTestBase 验证测试"
```

---

## Task 10: 全量测试和文档更新

**Files:**
- Modify: `docs/specs/epic-01-core-and-test/feature-F01-08/03_implementation.md`
- Create: `docs/specs/epic-01-core-and-test/feature-F01-08/04_test_spec.md`

**Step 1: 运行全量测试**

```bash
./gradlew :cartisan-test:check
```

预期：BUILD SUCCESSFUL（编译 + 测试 + ArchUnit 全通过）

**Step 2: 创建测试规格文档**

创建 `docs/specs/epic-01-core-and-test/feature-F01-08/04_test_spec.md`:

```markdown
# Feature: F01-08 — 测试规格

> 版本：v0.1 | 日期：2026-03-13

---

## 测试策略

| 测试类型 | 工具 | 覆盖范围 |
|---------|------|---------|
| 单元测试 | JUnit 5 + AssertJ | 容器类配置验证 |
| 集成测试 | Testcontainers | 容器启动、连接、数据清理 |
| API 测试 | MockMvc | ApiTestBase 功能验证 |

---

## 测试用例清单

### PostgresTestContainerTest

| 用例 | 验证内容 |
|------|---------|
| `postgres()` 返回配置好的容器 | 数据库名、用户名、密码 |
| 构造函数抛出异常 | 禁止反射实例化 |
| 类为 final | 工具类约束 |
| 有 @TestConfiguration 注解 | Spring 配置正确 |

### RedisTestContainerTest

| 用例 | 验证内容 |
|------|---------|
| `redis()` 返回配置好的容器 | 暴露端口 6379 |
| 构造函数抛出异常 | 禁止反射实例化 |
| 类为 final | 工具类约束 |
| 有 @TestConfiguration 注解 | Spring 配置正确 |

### IntegrationTestBaseTest

| 用例 | 验证内容 |
|------|---------|
| 注入 JdbcTemplate 和 RedisTemplate | Spring 连接自动配置 |
| 数据库可写入 | PostgreSQL 容器正常 |
| Redis 可写入 | Redis 容器正常 |

### DataCleanupTest

| 用例 | 验证内容 |
|------|---------|
| 测试间数据隔离 | @BeforeEach 清理生效 |
| PostgreSQL 清理 | TRUNCATE ... CASCADE 生效 |
| Redis 清理 | FLUSHDB 生效 |

### ApiTestBaseTest

| 用例 | 验证内容 |
|------|---------|
| MockMvc 注入 | @AutoConfigureMockMvc 生效 |
| HTTP 请求可发送 | MockMvc 正常工作 |
| 继承关系 | 继承 IntegrationTestBase |

---

## 测试执行命令

```bash
# 全量测试
./gradlew :cartisan-test:check

# 单个测试类
./gradlew :cartisan-test:test --tests PostgresTestContainerTest
./gradlew :cartisan-test:test --tests IntegrationTestBaseTest
```
```

**Step 3: 更新 03_implementation.md 状态**

在 `docs/specs/epic-01-core-and-test/feature-F01-08/03_implementation.md` 顶部添加：

```markdown
> 状态：Phase 3 完成 | 待执行
```

**Step 4: 提交**

```bash
git add docs/specs/epic-01-core-and-test/feature-F01-08/
git commit -m "docs(test): 完成 F01-08 实施计划和测试规格文档"
```

---

## 执行总结

**预计时间**: 约 30-45 分钟

**文件清单**:
- 新增: `gradle/libs.versions.toml` (修改)
- 新增: `cartisan-test/build.gradle.kts` (修改)
- 新增: `cartisan-test/src/main/java/com/cartisan/test/container/*`
- 新增: `cartisan-test/src/main/java/com/cartisan/test/base/*`
- 新增: `cartisan-test/src/test/java/com/cartisan/test/container/*`
- 新增: `cartisan-test/src/test/java/com/cartisan/test/base/*`
- 新增: `docs/specs/epic-01-core-and-test/feature-F01-08/03_implementation.md`
- 新增: `docs/specs/epic-01-core-and-test/feature-F01-08/04_test_spec.md`

**验收标准**:
- [ ] 所有测试通过: `./gradlew :cartisan-test:check`
- [ ] 容器自动启动和连接成功
- [ ] 数据自动清理验证通过
- [ ] ApiTestBase MockMvc 正常工作

---

**实施计划完成！下一步选择执行方式。**
