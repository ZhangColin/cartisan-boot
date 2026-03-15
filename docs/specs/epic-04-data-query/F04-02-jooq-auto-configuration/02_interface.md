# Feature: F04-02 jOOQ 自动配置 — 接口契约

## 一、类/接口职责

### 1.1 JooqProperties

**职责：** 封装 jOOQ 配置属性，通过 `@ConfigurationProperties` 绑定外部配置。

**包路径：** `com.cartisan.data.query.config`

**注解：** `@ConfigurationProperties("cartisan.data-query.jooq")`

**属性：**
| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| sqlLogging | boolean | false | 是否启用 SQL 执行日志 |

---

### 1.2 JooqAutoConfiguration

**职责：** Spring Boot 自动配置类，创建 `DSLContext` Bean。

**包路径：** `com.cartisan.data.query.config`

**注解：**
- `@AutoConfiguration` — Spring Boot 3.x 自动配置
- `@ConditionalOnBean(DataSource.class)` — 有 DataSource 才生效
- `@ConditionalOnMissingBean(DSLContext.class)` — 允许用户覆盖
- `@EnableConfigurationProperties(JooqProperties.class)` — 启用配置属性

**方法：**
| 方法 | 返回类型 | 说明 |
|------|---------|------|
| dslContext(DataSource, JooqProperties) | DSLContext | 创建 jOOQ 核心 API 入口 |
| settings(JooqProperties) | Settings | 构建 jOOQ 配置（私有辅助方法） |

---

### 1.3 自动配置注册

**文件路径：** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**内容：**
```
com.cartisan.data.query.config.JooqAutoConfiguration
```

---

## 二、领域接口描述（伪代码）

### 2.1 JooqProperties 接口描述

```java
// 配置属性：cartisan.data-query.jooq
// 前置条件：无
// 后置条件：sqlLogging 属性可被 Spring Boot 绑定

configurationProperties(prefix = "cartisan.data-query.jooq")
class JooqProperties {
    field sqlLogging: boolean = false

    getSqlLogging(): boolean
    setSqlLogging(boolean): void
}
```

### 2.2 JooqAutoConfiguration 接口描述

```java
// 自动配置类
// 前置条件：
//   - Spring 容器中存在 DataSource Bean
//   - 不存在用户自定义的 DSLContext Bean
// 后置条件：
//   - 注册 DSLContext 单例 Bean
//   - 使用项目已有 DataSource
//   - 方言固定为 PostgreSQL
//   - 根据 JooqProperties.sqlLogging 决定是否打印 SQL

@AutoConfiguration
@ConditionalOnBean(DataSource.class)
@ConditionalOnMissingBean(DSLContext.class)
@EnableConfigurationProperties(JooqProperties.class)
class JooqAutoConfiguration {

    // 创建 DSLContext Bean
    // 参数：dataSource - 项目已有的 DataSource
    //      properties - 配置属性
    // 返回：配置好的 DSLContext 实例
    bean dslContext(dataSource: DataSource, properties: JooqProperties): DSLContext {
        return DSL.using(
            dataSource,
            SQLDialect.POSTGRES,
            settings(properties)
        )
    }

    // 构建 jOOQ Settings（私有方法）
    // 参数：properties - 配置属性
    // 返回：jOOQ Settings 实例
    private settings(properties: JooqProperties): Settings {
        builder = new SettingsBuilder()
        if properties.isSqlLogging() {
            builder.executeLogging(true)
        }
        return builder.build()
    }
}
```

---

## 三、配置属性接口

### 3.1 application.yml 配置示例

```yaml
cartisan:
  data-query:
    jooq:
      # 是否启用 SQL 执行日志（默认 false）
      sql-logging: true
```

### 3.2 配置元数据（IDE 提示）

通过 `spring-boot-configuration-processor` 自动生成，包含：
- 属性名：`cartisan.data-query.jooq.sql-logging`
- 类型：`java.lang.Boolean`
- 默认值：`false`
- 描述：是否启用 SQL 执行日志

---

## 四、核心流程（伪代码）

```
应用启动
    │
    ▼
检查 @ConditionalOnBean(DataSource)
    │
    ├─ 无 DataSource ──→ 跳过自动配置，不创建 DSLContext
    │
    └─ 有 DataSource ──→ 检查 @ConditionalOnMissingBean(DSLContext)
                           │
                           ├─ 已有用户 DSLContext ──→ 跳过自动配置
                           │
                           └─ 无用户 DSLContext ──→ 执行创建
                                                   │
                                                   ├─ 读取 JooqProperties
                                                   ├─ 构建 Settings
                                                   │   └─ sqlLogging=true → executeLogging(true)
                                                   ├─ 创建 DSLContext
                                                   │   └─ DSL.using(dataSource, POSTGRES, settings)
                                                   └─ 注册为 Spring Bean
```

---

## 五、数据库变更

**无数据库变更。** 本 Feature 仅配置 jOOQ，不涉及数据库表结构或迁移脚本。

---

## 六、备选方案及取舍

### 6.1 方案选择：为什么选显式方言配置

| 方案 | 描述 | 优点 | 缺点 | 选择 |
|------|------|------|------|------|
| A | 显式配置 `SQLDialect.POSTGRES` | 行为明确，不依赖自动检测 | 仅支持 PostgreSQL | ✅ 采用 |
| B | 使用 jOOQ 默认自动检测 | 支持多数据库 | 可能因环境差异导致不一致 | ❌ |
| C | 支持配置属性覆盖方言 | 灵活支持多库 | 增加配置复杂度（YAGNI） | ❌ |

**结论：** 采用方案 A。项目技术栈已选定 PostgreSQL 16+，框架层显式配置意图明确，行为可预期。

### 6.2 条件装配：为什么组合使用两个条件注解

| 条件 | 作用 |
|------|------|
| `@ConditionalOnBean(DataSource.class)` | 无 DataSource 时不创建，避免强制依赖 |
| `@ConditionalOnMissingBean(DSLContext.class)` | 用户自定义时退让，允许覆盖 |

两者组合实现：有数据源且无用户自定义时才自动配置。

### 6.3 Settings API：实现时按 jOOQ 版本确认

设计文档中使用 `SettingsBuilder` + `executeLogging(true)`。实际实现时需确认项目使用的 jOOQ 版本：

- 若版本支持 Builder 模式：使用 `new SettingsBuilder()...`
- 若版本使用不可变 Settings：使用 `new Settings().withExecuteLogging(true)`

---

## 七、使用示例

### 7.1 业务项目使用

```java
// 1. 添加依赖（build.gradle.kts）
// implementation("com.cartisan:cartisan-data-query:0.3.0")

// 2. 配置数据源（Spring Boot 自动配置）
// spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
// spring.datasource.username=user
// spring.datasource.password=pass

// 3. 直接注入 DSLContext 使用
@Service
public class UserService {
    private final DSLContext dsl;

    public UserService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<User> findAll() {
        return dsl.selectFrom(USER).fetchInto(User.class);
    }
}
```

### 7.2 用户自定义覆盖

```java
@Configuration
public class CustomJooqConfiguration {
    @Bean
    public DSLContext customDslContext(DataSource dataSource) {
        // 自定义配置（如多数据源、特殊设置）
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
// JooqAutoConfiguration 会自动退让（@ConditionalOnMissingBean）
```

---

## 八、错误码

**无自定义错误码。** 本 Feature 为自动配置层，不定义业务异常。

可能由框架/依赖抛出的异常：
| 异常 | 场景 | 处理 |
|------|------|------|
| `NoSuchBeanDefinitionException` | 无 DataSource 时注入 DSLContext | 配置数据源 |
| `SQLException` / `DataAccessException` | 首次查询时数据库连接失败 | 检查数据库连接配置 |
| `IllegalArgumentException` | sql-logging 配置非布尔值 | 修正配置为 true/false |

---

## 九、测试策略（详见 Phase 5）

| 测试类 | 测试内容 | 数据源 |
|--------|---------|--------|
| `JooqAutoConfigurationTest` | Bean 创建、配置开关 | PostgresTestContainer |
| `JooqPropertiesTest` | 配置绑定、默认值 | 无需 DataSource |
