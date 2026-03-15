# Feature: F04-02 jOOQ 自动配置 — 实施计划

## 目标复述

在 cartisan-data-query 模块中添加 jOOQ 自动配置能力，使业务项目引入依赖后能够直接注入 `DSLContext` Bean 进行查询。

核心要点：
- 使用 `@AutoConfiguration` 注解 + `AutoConfiguration.imports` 注册
- 条件装配：`@ConditionalOnBean(DataSource)` + `@ConditionalOnMissingBean(DSLContext)`
- 显式配置 PostgreSQL 方言
- 支持可选的 SQL 日志开关（`cartisan.data-query.jooq.sql-logging`）

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `src/main/java/com/cartisan/data/query/config/JooqAutoConfiguration.java` | 主自动配置类 |
| 新增 | `src/main/java/com/cartisan/data/query/config/JooqProperties.java` | 配置属性类 |
| 新增 | `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 自动配置注册 |
| 修改 | `build.gradle.kts` | 添加 spring-boot-autoconfigure、configuration-processor、cartisan-test 依赖 |
| 新增 | `src/test/java/com/cartisan/data/query/config/JooqAutoConfigurationTest.java` | 集成测试 |

## 核心流程（伪代码）

```
应用启动
    │
    ▼
检查 @ConditionalOnBean(DataSource.class)
    │
    ├─ 无 DataSource ──→ 跳过自动配置
    │
    └─ 有 DataSource ──→ 检查 @ConditionalOnMissingBean(DSLContext.class)
                           │
                           ├─ 已有用户 DSLContext ──→ 跳过自动配置
                           │
                           └─ 无用户 DSLContext ──→ 创建 Bean
                                                   │
                                                   ├─ 读取 JooqProperties
                                                   ├─ 构建 Settings（sqlLogging ? executeLogging : skip）
                                                   ├─ 创建 DSLContext
                                                   │   └─ DSL.using(dataSource, POSTGRES, settings)
                                                   └─ 注册为 Spring Bean
```

## 原子任务清单

### Step 1: 更新 build.gradle.kts 依赖

- **文件**：`cartisan-data-query/build.gradle.kts`
- **内容**：添加 Spring Boot 自动配置相关依赖
- **验证**：`./gradlew :cartisan-data-query:dependencies` 中包含新依赖

```kotlin
dependencies {
    // ... 现有依赖 ...

    // Spring Boot AutoConfiguration 支持
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // 配置属性元数据处理器（IDE 自动补全提示）
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // 测试依赖
    testImplementation(project(":cartisan-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

---

### Step 2: 创建 JooqProperties 配置类

- **文件**：`src/main/java/com/cartisan/data/query/config/JooqProperties.java`
- **内容**：
  - `@ConfigurationProperties("cartisan.data-query.jooq")` 注解
  - `sqlLogging` 布尔字段（默认 false）
  - getter/setter 方法
  - 详细 JavaDoc
- **验证**：编译通过

```java
package com.cartisan.data.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * jOOQ 配置属性。
 *
 * <p>配置前缀：{@code cartisan.data-query.jooq}
 */
@ConfigurationProperties("cartisan.data-query.jooq")
public class JooqProperties {

    /**
     * 是否启用 SQL 执行日志。
     */
    private boolean sqlLogging = false;

    public boolean isSqlLogging() {
        return sqlLogging;
    }

    public void setSqlLogging(boolean sqlLogging) {
        this.sqlLogging = sqlLogging;
    }
}
```

---

### Step 3: 创建 JooqAutoConfiguration 自动配置类

- **文件**：`src/main/java/com/cartisan/data/query/config/JooqAutoConfiguration.java`
- **内容**：
  - `@AutoConfiguration` 注解
  - `@ConditionalOnBean(DataSource.class)`
  - `@ConditionalOnMissingBean(DSLContext.class)`
  - `@EnableConfigurationProperties(JooqProperties.class)`
  - `dslContext()` Bean 方法
  - `settings()` 私有辅助方法
- **注意**：实现时需确认 jOOQ 版本的 Settings API（Builder 或 withExecuteLogging）
- **验证**：编译通过

```java
package com.cartisan.data.query.config;

import org.jooq.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * cartisan-data-query 模块的 jOOQ 自动配置。
 */
@AutoConfiguration
@ConditionalOnBean(DataSource.class)
@ConditionalOnMissingBean(DSLContext.class)
@EnableConfigurationProperties(JooqProperties.class)
public class JooqAutoConfiguration {

    @Bean
    public DSLContext dslContext(DataSource dataSource, JooqProperties properties) {
        return DSL.using(
            dataSource,
            SQLDialect.POSTGRES,
            settings(properties)
        );
    }

    private Settings settings(JooqProperties properties) {
        // 实现时按 jOOQ 版本确认 API
        // 可能是 SettingsBuilder 或 new Settings().withExecuteLogging()
    }
}
```

---

### Step 4: 创建 AutoConfiguration.imports 注册文件

- **文件**：`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- **内容**：
```
com.cartisan.data.query.config.JooqAutoConfiguration
```
- **验证**：文件路径和格式正确

---

### Step 5: 编写集成测试

- **文件**：`src/test/java/com/cartisan/data/query/config/JooqAutoConfigurationTest.java`
- **内容**：
  - `@SpringBootTest`
  - `@Import(PostgresTestContainer.class)`
  - 两个测试方法：验证 Bean 创建、验证 sqlLogging 配置
- **验证**：编译通过

```java
package com.cartisan.data.query.config;

import com.cartisan.test.container.PostgresTestContainer;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgresTestContainer.class)
class JooqAutoConfigurationTest {

    @Autowired
    private DSLContext dslContext;

    @Test
    @DisplayName("given_postgresDataSource_when_autoConfig_then_dslContextBeanCreated")
    void given_postgresDataSource_when_autoConfig_then_dslContextBeanCreated() {
        assertThat(dslContext).isNotNull();
    }

    @Test
    @TestPropertySource(properties = "cartisan.data-query.jooq.sql-logging=true")
    @DisplayName("given_sqlLoggingEnabled_when_autoConfig_then_contextStartsSuccessfully")
    void given_sqlLoggingEnabled_when_autoConfig_then_contextStartsSuccessfully() {
        assertThat(dslContext).isNotNull();
    }
}
```

---

### Step 6: 全量验证

- **验证命令**：
```bash
./gradlew :cartisan-data-query:compileJava
./gradlew :cartisan-data-query:test
```
- **验收标准**：
  - 编译通过
  - 测试全部绿灯
  - 生成了 `spring-configuration-metadata.json`（configuration-processor 产物）

## 实施顺序

```
Step 1: 更新 build.gradle.kts ─┐
                          │
Step 2: JooqProperties ─────┼──→ Step 6: 全量验证
                          │
Step 3: JooqAutoConfiguration ─┤
                          │
Step 4: AutoConfiguration.imports ─┘
                          │
Step 5: 测试 ───────────────┘
```

## 代码量预估

| 文件 | 预估行数 |
|------|---------|
| JooqProperties.java | 30-40 |
| JooqAutoConfiguration.java | 50-70 |
| AutoConfiguration.imports | 1 |
| JooqAutoConfigurationTest.java | 30-40 |
| **总计** | **110-150 行** |

## 依赖模块

- `cartisan-test` — 提供 `PostgresTestContainer`
- `cartisan-web` — 复用 `PageResponse`（已有，F04-01 已配置）
