# Druid 数据源集成实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 cartisan-data-jpa 模块中集成 Druid 数据源，提供 SQL 监控、慢 SQL 记录、防火墙和连接池监控功能。

**Architecture:** 通过 `compileOnly` 引入 Druid Starter，创建自动配置类注册 StatFilter 和 WallFilter，业务通过 `spring.datasource.type` 显式切换到 Druid 时生效。

**Tech Stack:** Spring Boot 3.4.x, Druid Spring Boot 3 Starter 1.2.23, JUnit 5, AssertJ

---

## File Structure

```
cartisan-data-jpa/
├── build.gradle.kts                                    (修改 - 添加依赖)
├── src/main/java/com/cartisan/data/jpa/config/
│   ├── CartisanDataJpaAutoConfiguration.java          (已存在)
│   └── DruidAutoConfiguration.java                    (新建)
├── src/main/resources/META-INF/spring/
│   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports  (修改)
└── src/test/java/com/cartisan/data/jpa/config/
    └── DruidAutoConfigurationTest.java                (新建)
docs/guide/
    └── cartisan-boot-使用手册.md                       (修改)
```

---

## Task 1: 添加 Druid Starter 依赖

**Files:**
- Modify: `cartisan-data-jpa/build.gradle.kts`

- [ ] **Step 1: 编辑 build.gradle.kts 添加依赖**

在 `dependencies` 块中添加 Druid Starter（在 Lombok 配置之后）：

```kotlin
// Druid Spring Boot 3 Starter（可选依赖，业务需显式配置 spring.datasource.type 才生效）
compileOnly("com.alibaba:druid-spring-boot-3-starter:1.2.23")
```

- [ ] **Step 2: 验证 Gradle 语法**

Run: `./gradlew :cartisan-data-jpa:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/build.gradle.kts
git commit -m "feat(data-jpa): add Druid Starter as compileOnly dependency"
```

---

## Task 2: 创建 DruidAutoConfiguration 类

**Files:**
- Create: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/DruidAutoConfiguration.java`

- [ ] **Step 1: 创建 DruidAutoConfiguration.java**

```java
package com.cartisan.data.jpa.config;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Druid 数据源自动配置。
 *
 * <p>配置内容：
 * <ul>
 *   <li>StatFilter — SQL 监控和慢 SQL 记录</li>
 *   <li>WallFilter — SQL 防火墙，防止 SQL 注入</li>
 * </ul>
 *
 * <h3>装配条件</h3>
 * <ul>
 *   <li>classpath 中存在 {@code com.alibaba.druid.pool.DruidDataSource}</li>
 *   <li>配置了 {@code spring.datasource.type=com.alibaba.druid.pool.DruidDataSource}</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <p>业务项目在 {@code application.yml} 中配置：</p>
 * <pre>{@code
 * spring:
 *   datasource:
 *     type: com.alibaba.druid.pool.DruidDataSource
 *     druid:
 *       stat-view-servlet:
 *         enabled: true
 *         login-username: admin
 *         login-password: admin
 * }</pre>
 *
 * <p>监控页面访问地址：{@code http://localhost:8080/druid/index.html}</p>
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.alibaba.druid.pool.DruidDataSource")
@ConditionalOnProperty(
    name = "spring.datasource.type",
    havingValue = "com.alibaba.druid.pool.DruidDataSource"
)
public class DruidAutoConfiguration {

    /**
     * 配置 StatFilter（SQL 监控过滤器）。
     *
     * <p>默认启用，可通过 {@code spring.datasource.druid.filter.stat.enabled=false} 禁用。</p>
     *
     * <p>业务可通过配置覆盖慢 SQL 阈值等参数：</p>
     * <pre>{@code
     * spring:
     *   datasource:
     *     druid:
     *       filter:
     *         stat:
     *           log-slow-sql: true
     *           slow-sql-millis: 1000
     * }</pre>
     *
     * @return StatFilter Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "spring.datasource.druid.filter.stat",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public StatFilter statFilter() {
        return new StatFilter();
    }

    /**
     * 配置 WallConfig（防火墙配置）。
     *
     * <p>默认允许批量执行 SQL，可通过配置覆盖。</p>
     *
     * @return WallConfig Bean
     */
    @Bean
    public WallConfig wallConfig() {
        WallConfig config = new WallConfig();
        // 允许批量执行，兼容 JPA 批量操作
        config.setMultiStatementAllow(true);
        return config;
    }

    /**
     * 配置 WallFilter（SQL 防火墙过滤器）。
     *
     * <p>默认启用，可通过 {@code spring.datasource.druid.filter.wall.enabled=false} 禁用。</p>
     *
     * @param wallConfig WallConfig Bean
     * @return WallFilter Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "spring.datasource.druid.filter.wall",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public WallFilter wallFilter(WallConfig wallConfig) {
        WallFilter filter = new WallFilter();
        filter.setConfig(wallConfig);
        return filter;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :cartisan-data-jpa:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/DruidAutoConfiguration.java
git commit -m "feat(data-jpa): add DruidAutoConfiguration for StatFilter and WallFilter"
```

---

## Task 3: 注册自动配置

**Files:**
- Modify: `cartisan-data-jpa/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: 更新 AutoConfiguration.imports**

在文件末尾添加新行：

```
com.cartisan.data.jpa.config.CartisanDataJpaAutoConfiguration
com.cartisan.data.jpa.config.DruidAutoConfiguration
```

- [ ] **Step 2: 验证自动配置可加载**

Run: `./gradlew :cartisan-data-jpa:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
git commit -m "feat(data-jpa): register DruidAutoConfiguration"
```

---

## Task 4: 编写单元测试

**Files:**
- Create: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/config/DruidAutoConfigurationTest.java`
- Create: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/config/DruidTestApplication.java`

- [ ] **Step 4.1: 创建测试应用配置类**

创建 `DruidTestApplication.java`：

```java
package com.cartisan.data.jpa.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Druid 自动配置测试应用。
 */
@SpringBootApplication
public class DruidTestApplication {
}
```

- [ ] **Step 4.2: 编写单元测试**

创建 `DruidAutoConfigurationTest.java`：

```java
package com.cartisan.data.jpa.config;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DruidAutoConfiguration 单元测试。
 */
@DisplayName("DruidAutoConfiguration 测试")
class DruidAutoConfigurationTest {

    @Nested
    @SpringBootTest(classes = DruidTestApplication.class)
    @DisplayName("未配置 spring.datasource.type 时")
    class WithoutDruidTypeConfigured {

        @Autowired(required = false)
        private StatFilter statFilter;

        @Autowired(required = false)
        private WallFilter wallFilter;

        @Autowired(required = false)
        private WallConfig wallConfig;

        @Test
        @DisplayName("不应该创建 Druid 相关 Bean")
        void shouldNotCreateDruidBeans() {
            assertThat(statFilter).isNull();
            assertThat(wallFilter).isNull();
            assertThat(wallConfig).isNull();
        }
    }

    @Nested
    @SpringBootTest(classes = DruidTestApplication.class)
    @TestPropertySource(properties = {
        "spring.datasource.type=com.alibaba.druid.pool.DruidDataSource"
    })
    @DisplayName("配置 spring.datasource.type=DruidDataSource 时")
    class WithDruidTypeConfigured {

        @Autowired(required = false)
        private StatFilter statFilter;

        @Autowired(required = false)
        private WallFilter wallFilter;

        @Autowired(required = false)
        private WallConfig wallConfig;

        @Test
        @DisplayName("应该创建 StatFilter Bean")
        void shouldCreateStatFilter() {
            assertThat(statFilter).isNotNull();
        }

        @Test
        @DisplayName("应该创建 WallFilter Bean")
        void shouldCreateWallFilter() {
            assertThat(wallFilter).isNotNull();
        }

        @Test
        @DisplayName("应该创建 WallConfig Bean")
        void shouldCreateWallConfig() {
            assertThat(wallConfig).isNotNull();
        }

        @Test
        @DisplayName("WallConfig 应该允许批量执行")
        void wallConfigShouldAllowMultiStatement() {
            assertThat(wallConfig.isMultiStatementAllow()).isTrue();
        }
    }

    @Nested
    @SpringBootTest(classes = DruidTestApplication.class)
    @TestPropertySource(properties = {
        "spring.datasource.type=com.alibaba.druid.pool.DruidDataSource",
        "spring.datasource.druid.filter.stat.enabled=false"
    })
    @DisplayName("禁用 StatFilter 时")
    class WithStatFilterDisabled {

        @Autowired(required = false)
        private StatFilter statFilter;

        @Autowired(required = false)
        private WallFilter wallFilter;

        @Test
        @DisplayName("不应该创建 StatFilter，但应该创建 WallFilter")
        void shouldNotCreateStatFilter() {
            assertThat(statFilter).isNull();
            assertThat(wallFilter).isNotNull();
        }
    }

    @Nested
    @SpringBootTest(classes = DruidTestApplication.class)
    @TestPropertySource(properties = {
        "spring.datasource.type=com.alibaba.druid.pool.DruidDataSource",
        "spring.datasource.druid.filter.wall.enabled=false"
    })
    @DisplayName("禁用 WallFilter 时")
    class WithWallFilterDisabled {

        @Autowired(required = false)
        private StatFilter statFilter;

        @Autowired(required = false)
        private WallFilter wallFilter;

        @Autowired(required = false)
        private WallConfig wallConfig;

        @Test
        @DisplayName("不应该创建 WallFilter，但应该创建 StatFilter")
        void shouldNotCreateWallFilter() {
            assertThat(statFilter).isNotNull();
            // WallConfig 是独立 Bean，不受 enabled 控制
            assertThat(wallConfig).isNotNull();
            assertThat(wallFilter).isNull();
        }
    }
}
```

- [ ] **Step 4.3: 运行测试验证**

Run: `./gradlew :cartisan-data-jpa:test --tests DruidAutoConfigurationTest`
Expected: 所有测试通过

- [ ] **Step 4.4: 提交**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/config/
git commit -m "test(data-jpa): add DruidAutoConfiguration unit tests"
```

---

## Task 5: 更新使用手册

**Files:**
- Modify: `docs/guide/cartisan-boot-使用手册.md`

- [ ] **Step 5.1: 在 "1.4 cartisan-data-jpa 模块" 添加 Druid 说明**

在 `cartisan-data-jpa` 模块的能力表格中添加一行：

```markdown
| **Druid 集成** | 支持 Druid 数据源，提供 SQL 监控、慢 SQL 记录、防火墙功能 |
```

插入位置：在 `@Condition 注解` 行之后

- [ ] **Step 5.2: 在 "2.XX" 新增 Druid 配置章节**

在 `@Condition 注解详细说明` (5.1) 之后，添加新章节 `5.X Druid 数据源`：

```markdown
### 5.X Druid 数据源

`cartisan-data-jpa` 模块支持集成 Druid 数据源，提供 SQL 监控、慢 SQL 记录、防火墙等功能。

#### 5.X.1 启用方式

在业务项目 `application.yml` 中配置：

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: pass
```

#### 5.X.2 监控页面配置

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      stat-view-servlet:
        enabled: true
        login-username: admin
        login-password: admin
```

访问地址：`http://localhost:8080/druid/index.html`

#### 5.X.3 慢 SQL 记录配置

```yaml
spring:
  datasource:
    druid:
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 1000
```

#### 5.X.4 SQL 防火墙配置

```yaml
spring:
  datasource:
    druid:
      filter:
        wall:
          enabled: true
          config:
            multi-statement-allow: true
```

#### 5.X.5 注意事项

- 框架默认使用 HikariCP，需显式配置 `spring.datasource.type` 才切换到 Druid
- 生产环境建议配置监控页面登录密码
- `RequestLogFilter` 已排除 `/druid/*` 路径，监控页面访问不会被记录
```

- [ ] **Step 5.3: 在 "依赖说明" 章节添加 Druid 信息**

在 `5.4 cartisan-data-jpa` 的依赖说明中添加：

```markdown
```
api 依赖：
- cartisan-core
- cartisan-event

implementation 依赖：
- Spring Boot Starter Data JPA
- Hibernate Core（传递）

compileOnly 依赖：
- Druid Spring Boot 3 Starter 1.2.23（可选，业务需显式配置才生效）
```
```

- [ ] **Step 5.4: 验证文档格式**

Run: `grep -n "Druid" docs/guide/cartisan-boot-使用手册.md`
Expected: 能看到新增的 Druid 相关内容

- [ ] **Step 5.5: 提交**

```bash
git add docs/guide/cartisan-boot-使用手册.md
git commit -m "docs: add Druid integration usage guide"
```

---

## Task 6: 更新设计文档归档

**Files:**
- Create: `docs/guide/druid-数据源集成指南.md` (可选，从设计文档提取使用说明)
- Archive: 移动设计文档到归档

- [ ] **Step 6.1: 提取使用指南（可选）**

如果需要单独的使用指南，从设计文档中提取使用方式章节创建独立文档。

- [ ] **Step 6.2: 归档设计文档**

```bash
git mv docs/superpowers/specs/2026-03-27-druid-integration-design.md docs/specs/
git commit -m "docs: archive Druid integration design spec"
```

- [ ] **Step 6.3: 清理过程文档**

如果 brainstorming 过程中有临时文档，清理或归档。

---

## 验收标准

完成所有任务后，验证以下功能：

1. **默认行为**: 业务项目不配置 `spring.datasource.type` 时，使用 HikariCP
2. **Druid 切换**: 配置 `spring.datasource.type=com.alibaba.druid.pool.DruidDataSource` 后，使用 Druid
3. **监控页面**: 访问 `/druid/index.html` 可看到监控页面
4. **Filter 生效**: SQL 语句被 StatFilter 和 WallFilter 拦截处理

**手动验证命令**:

```bash
# 1. 编译所有模块
./gradlew compileJava

# 2. 运行 cartisan-data-jpa 测试
./gradlew :cartisan-data-jpa:test

# 3. 验证自动配置注册
cat cartisan-data-jpa/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 参考资料

- [Druid GitHub](https://github.com/alibaba/druid)
- [Druid Spring Boot Starter 文档](https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter)
- 设计文档: [docs/superpowers/specs/2026-03-27-druid-integration-design.md](../specs/2026-03-27-druid-integration-design.md)
- 框架使用手册: [docs/guide/cartisan-boot-使用手册.md](../../guide/cartisan-boot-使用手册.md)
