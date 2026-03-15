# Epic 4: Data-Query — Backlog

> **Epic 目标**：构建 cartisan-boot 的 jOOQ 读侧封装，提供 CQRS 读侧基础设施
>
> **依赖**：Epic 2（cartisan-web 已完成 PageResponse，cartisan-data-jpa 已完成写侧）
>
> **复杂度**：S（Small，功能聚焦，无跨模块复杂集成）

---

## 设计决策

| 事项 | 决策 |
|------|------|
| 代码生成 | 提供配置指南 + 示例片段，不内置 CLI |
| 分页响应 | 复用 `cartisan-web` 的 `PageResponse<T>` |
| 集成测试 | 三项必测 + 可选的 JPA/jOOQ 共存验证 |
| 多租户 | 提供 `JooqTenantSupport.eqTenantId(TableField)` 工具方法 |

---

## Feature 列表

| ID | Feature | 描述 | 复杂度 | 预估代码量 |
|----|---------|------|--------|-----------|
| **F04-01** | 模块骨架 + PageQuery | 创建 cartisan-data-query 模块、定义 PageQuery Record | S | 50-80 行 |
| **F04-02** | jOOQ 自动配置 | DSLContext Bean 配置、PostgreSQL 方言、DataSource 集成 | S | 60-100 行 | ✅ Phase 5 审查归档 |
| **F04-03** | JooqTenantSupport | 多租户查询工具方法 `eqTenantId(TableField)` | S | 30-50 行 | ✅ Phase 5 审查归档 |
| **F04-04** | 代码生成配置指南 | build.gradle.kts 示例、生成策略文档 | S | 文档 + 示例 | ✅ 完成 |
| **F04-05** | 集成测试 | jOOQ 查询、分页、CQRS 共存验证 | M | 150-200 行 | ✅ Phase 5 审查归档 |

---

## 依赖关系

```
F04-01 (模块骨架 + PageQuery) ────────────────┐
                                                │
F04-02 (jOOQ 自动配置) ────────────────→ F04-01 │
                                                ├──→ F04-05 (集成测试)
F04-03 (JooqTenantSupport) ─────────────→ F04-01 │
                                                │
F04-04 (代码生成配置指南) ──────────────────────┘
      （独立文档，仅依赖 F04-01 的包结构约定）
```

**关键依赖说明：**
- `F04-02` 依赖 `F04-01`：自动配置需要模块骨架已建立
- `F04-03` 依赖 `F04-01`：工具类需要模块包结构
- `F04-05` 依赖 `F04-02 + F04-03`：集成测试需要 jOOQ 配置和多租户工具可用
- `F04-04` 独立：仅文档，无代码依赖

---

## 推荐开发顺序

### 批次 1 — 基础设施

| Feature | 理由 |
|---------|------|
| F04-01 | 无依赖，建立模块骨架和 PageQuery |

### 批次 2（并行）— 核心能力

| Feature | 依赖 |
|---------|------|
| F04-02 | F04-01 |
| F04-03 | F04-01 |
| F04-04 | F04-01（仅包结构约定）|

### 批次 3 — 集成验证

| Feature | 依赖 | 说明 |
|---------|------|------|
| F04-05 | F04-02 + F04-03 | 集成测试验证 jOOQ 可用性 + CQRS 共存 |

---

## 各 Feature 详细说明

### F04-01: 模块骨架 + PageQuery

**交付物：**
- `cartisan-data-query/build.gradle.kts`
  - 依赖：`jooq`、`cartisan-web`（复用 PageResponse）
  - 配置 jOOQ 版本（通过 Version Catalog）
- `PageQuery.java`（Record）
  ```java
  public record PageQuery(
      int page,
      int size
  ) {
      public PageQuery {
          page = page < 1 ? 1 : page;
          size = size < 1 ? 20 : Math.min(size, 100);
      }

      public long offset() {
          return (long) (page - 1) * size;
      }

      public static PageQuery of(int page, int size) {
          return new PageQuery(page, size);
      }
  }
  ```

**验收标准：**
- 模块可被 Gradle 构建识别
- PageQuery 参数校验正确（page 最小 1，size 范围 1-100）
- offset() 计算正确

---

### F04-02: jOOQ 自动配置

**交付物：**
- `JooqAutoConfiguration.java`
  - 配置 `DSLContext` Bean（使用项目的 DataSource）
  - 配置 PostgreSQL 方言
  - 配置 SQL 执行日志（可选）
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**配置属性（可选）：**
```java
@ConfigurationProperties("cartisan.data-query.jooq")
public class JooqProperties {
    private boolean logSql = false;  // 是否打印 SQL
}
```

**验收标准：**
- 引入依赖后 DSLContext 自动注入可用
- 集成测试验证查询成功执行
- PostgreSQL 方言正确配置

---

### F04-03: JooqTenantSupport

**交付物：**
- `JooqTenantSupport.java`
  ```java
  public final class JooqTenantSupport {
      public static Condition eqTenantId(TableField<?, Long> tenantIdField) {
          Long tenantId = TenantContext.getCurrentTenantId();
          return tenantId == null ? DSL.noCondition() : tenantIdField.eq(tenantId);
      }
  }
  ```

**设计要点：**
- 按列入参（`TableField`），任意表都可用
- 无租户上下文时返回 `noCondition()`，不添加过滤
- 显式调用，代码意图清晰，调试友好

**依赖说明：**
- 依赖 `cartisan-security` 的 `TenantContext`
- **可选依赖**：若业务项目未引入 security，调用时 tenantId 为 null，返回 `noCondition()`

**验收标准：**
- 有租户上下文时正确添加条件
- 无租户上下文时不报错、不添加条件
- 单元测试覆盖两种场景

---

### F04-04: 代码生成配置指南

**交付物：**
- `docs/guides/jooq-code-generation.md`
  - build.gradle.kts 配置示例
  - generateJooq 任务依赖 flywayMigrate
  - 生成目录约定（`src/main/generated/jooq`）
  - PostgreSQL 方言配置
  - 与 AI 协作 SOP 的衔接

**配置示例（片段）：**
```kotlin
// build.gradle.kts
jooq {
    configuration {
        generator {
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
            }
            generate {
                isJavaTimeTypes = true
            }
            target {
                packageName = "com.example.db"
                directory = "src/main/generated/jooq"
            }
        }
    }
}

tasks.named<generateJooq>("generateJooq") {
    dependsOn("flywayMigrate")
}
```

**验收标准：**
- 复制配置片段后可直接使用
- 与 AI 协作 SOP 约定一致
- 文档清晰易懂

---

### F04-05: 集成测试

**交付物：**
- `JooqIntegrationTest.java`
  - 测试 DSL 查询（类型安全、生成代码与 schema 一致）
  - 测试分页（offset/limit 转换、PageResponse 封装）
  - 测试多租户条件
  - **可选**：JPA 写 → jOOQ 读 共存验证

**必测项：**
1. jOOQ DSL 正确查询数据库
2. 分页计算正确（offset/limit 转换）
3. DataSource 集成正常

**可选增强：**
- JPA 写一条记录 → jOOQ 能读到 + 分页对
- 不涉及事务边界、领域事件、审计等复杂场景

**测试命名示例：**
```java
void given_dataExists_when_queryWithPageQuery_then_returnPageResponse() { }
void given_noTenantContext_when_callEqTenantId_then_returnNoCondition() { }
void given_jpaWrite_when_jooqQuery_then_dataConsistent() { } // 可选
```

**验收标准：**
- 所有必测项通过
- CQRS 共存测试可选通过
- 测试命名遵循 `given_{条件}_when_{操作}_then_{预期结果}` 规范

---

## 模块依赖

```
cartisan-data-query 依赖：
├── cartisan-web      (复用 PageResponse<T>)
├── cartisan-security (可选，用于 JooqTenantSupport)
├── jooq              (jOOQ 核心)
└── spring-boot       (自动配置支持)
```

**注意：** cartisan-data-query 可独立使用，不依赖 cartisan-core。当配合 cartisan-web 使用时分页 API 更顺畅。

---

## 复杂度评估标准

| 复杂度 | 代码量 | 特征 |
|--------|--------|------|
| **S** | 50-80 行 | 纯数据类、简单工具、无复杂集成 |
| **M** | 100-200 行 | 涉及 Spring 集成、多组件协作、中等复杂度 |
| **L** | 200-300 行 | 跨模块集成、复杂状态管理、需要仔细设计 |

---

## 参考文档

- 设计文档：[cartisan-boot-设计文档.md](../../cartisan-boot-设计文档.md#46-cartisan-data-queryjooq-读侧封装)
- Epic 2 规格：[epic-02-web-data-jpa-event/00_epic_backlog.md](../epic-02-web-data-jpa-event/00_epic_backlog.md)
- AI 协作 SOP：[AI协作开发SOP.md](../sop/AI协作开发SOP.md)
