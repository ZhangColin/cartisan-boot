# Feature: F04-02 jOOQ 自动配置

## 背景

cartisan-data-query 模块需要提供 jOOQ 的 Spring Boot 自动配置能力。业务项目引入依赖后，应能够直接注入 `DSLContext` Bean 进行查询，无需手动配置 jOOQ。

Epic Backlog 中的 F04-01 已完成模块骨架和 `PageQuery` 基础设施，本 Feature 在此基础上添加自动配置能力。

## 目标

- 引入 cartisan-data-query 依赖后，自动创建 `DSLContext` Bean
- 使用项目已有的 `DataSource`，不强制额外配置
- 固定使用 PostgreSQL 方言，与项目技术栈一致
- 提供可选的 SQL 执行日志开关
- 允许用户自定义 `DSLContext` 覆盖自动配置

## 范围

### 包含（In Scope）

- `JooqAutoConfiguration` — 主自动配置类
  - 条件装配：有 `DataSource` 且无用户自定义 `DSLContext`
  - 配置 `SQLDialect.POSTGRES` 方言
  - 支持 SQL 日志开关
- `JooqProperties` — `@ConfigurationProperties` 配置属性类
  - `cartisan.data-query.jooq.sql-logging` 布尔配置
- `META-INF/spring/...AutoConfiguration.imports` — Spring Boot 自动配置注册
- `build.gradle.kts` 依赖更新
  - `spring-boot-autoconfigure`（compileOnly）
  - `spring-boot-configuration-processor`（annotationProcessor）
  - `cartisan-test`（testImplementation）
- 集成测试 `JooqAutoConfigurationTest`
  - 使用 `PostgresTestContainer` 提供真实 `DataSource`
  - 验证 `DSLContext` Bean 正确创建
  - 验证 `sql-logging=true` 不导致启动失败

### 不包含（Out of Scope）

- jOOQ 代码生成配置（由 F04-04 负责）
- 多租户查询工具 `JooqTenantSupport`（由 F04-03 负责）
- 其他数据库方言支持（项目已选定 PostgreSQL）
- SQL 日志级别、慢查询阈值等复杂配置（YAGNI）

## 验收标准（Acceptance Criteria）

- **AC1**：引入依赖后，有 `DataSource` 时自动创建 `DSLContext` Bean
- **AC2**：没有 `DataSource` 时，自动配置跳过，不影响项目启动
- **AC3**：用户自定义 `DSLContext` 时，自动配置退让
- **AC4**：方言固定为 PostgreSQL，自动检测失败不影响行为
- **AC5**：`cartisan.data-query.jooq.sql-logging=true` 时启用 SQL 日志
- **AC6**：集成测试验证 Bean 创建 + 配置开关场景

## 约束

### 技术约束

- 必须使用 Spring Boot 3.x 的 `@AutoConfiguration` 注解
- 必须遵循现有模块（cartisan-web、cartisan-data-jpa）的自动配置模式
- `@ConfigurationProperties` 前缀必须是 `cartisan.data-query.jooq`

### 兼容性约束

- 与 Spring Boot 3.4.x 兼容
- 与 jOOQ 版本（由 cartisan-dependencies 管理）兼容
- 不强制依赖 cartisan-security，可独立使用

### 质量约束

- 代码符合 `CLAUDE.md` 编码规范
- 测试命名遵循 `docs/skills/SKILL.md` 中的 TEST-002 规则
- JavaDoc 完整，包含配置说明、条件装配、用户覆盖方式
