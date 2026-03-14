# Feature: cartisan-data-jpa 审计与软删除

> **Feature ID**: F02-06
> **Epic**: Epic 2 — Web + Data-JPA + Event
> **复杂度**: M (100-150 行)
> **依赖**: F02-04 (BaseRepository)

---

## 背景

在业务系统中，实体通常需要记录「谁在什么时候创建/修改」以及「软删除」能力。Spring Data JPA 提供了审计功能（`@CreatedDate`、`@LastModifiedDate` 等）和软删除支持，但需要手动配置。

本 Feature 将这些能力封装为可复用的基类，业务项目的实体只需继承 `Auditable` 或 `SoftDeletable` 即自动获得审计和软删除能力。

**与设计文档的关联**：
- 设计文档 4.2 节：定义 `Auditable` 和 `SoftDeletable` 基类
- 设计文档 4.5 节：审计字段从 `SecurityContext` 获取（本 Feature 通过 `AuditorAware` 接口实现）
- 架构约束：认证层使用 Sa-Token，不硬依赖 Spring Security

---

## 目标

- 提供 `Auditable` 基类，包含创建时间、修改时间、创建人、修改人 4 个审计字段
- 提供 `SoftDeletable` 基类，继承 `Auditable`，增加软删除标记和查询过滤
- 配置 JPA Auditing，支持自动填充审计字段
- 通过 `AuditorAware` 接口与安全层解耦，由 cartisan-security 或业务项目实现

---

## 范围

### 包含（In Scope）

| 交付物 | 描述 |
|--------|------|
| **Auditable** | `@MappedSuperclass`，包含 4 个审计字段 |
| **SoftDeletable** | 继承 `Auditable`，增加 `deleted` 字段 + `@SQLRestriction` |
| **JpaAuditingConfiguration** | `@EnableJpaAuditing` 配置类，条件装配 |
| **AuditorAware 接口依赖** | 定义扩展点，不提供具体实现 |
| **集成测试** | 验证审计字段自动填充、软删除查询过滤 |

### 不包含（Out of Scope）

| 功能 | 原因 |
|------|------|
| 软删除还原（`restore()`） | 业务语义因场景而异，由业务层实现 |
| 级联软删除 | 需要业务规则约定，框架无法通用实现 |
| 强制物理删除（`hardDelete()`） | 运维场景，业务项目可用 `@Modifying` + `@Query` 自行实现 |
| Spring Security 集成 | 认证层使用 Sa-Token，通过 `AuditorAware` 解耦 |
| 默认审计人实现 | 不提供 "system"/"anonymous" 占位值，保持 null 语义更清晰 |

---

## 验收标准（Acceptance Criteria）

### AC1: Auditable 基类提供 4 个审计字段

**Given** 一个继承 `Auditable` 的实体
**When** 首次保存该实体
**Then** `createdAt`、`lastModifiedDate` 被自动填充为当前时间
**And** `createdBy`、`lastModifiedBy` 被自动填充为 `AuditorAware.getCurrentAuditor()` 返回值

### AC2: 审计字段在更新时自动更新

**Given** 一个已保存的实体
**When** 修改该实体并再次保存
**Then** `lastModifiedDate` 被自动更新为新的修改时间
**And** `lastModifiedBy` 被自动更新为新的操作人
**And** `createdAt` 和 `createdBy` 保持不变

### AC3: SoftDeletable 提供软删除能力

**Given** 一个继承 `SoftDeletable` 的实体
**When** 调用 `repository.delete(entity)` 删除该实体
**Then** 实体的 `deleted` 字段被设置为 `true`
**And** 实体仍存在于数据库中（物理记录未删除）

### AC4: 软删除实体被自动过滤

**Given** 数据库中存在 `deleted = true` 和 `deleted = false` 的记录
**When** 执行 `repository.findAll()` 查询
**Then** 只返回 `deleted = false` 的记录
**And** `deleted = true` 的记录被 `@SQLRestriction` 自动过滤

### AC5: JPA Auditing 条件装配

**Given** Spring 容器中不存在 `AuditorAware` Bean
**When** 启动应用
**Then** `@EnableJpaAuditing` 不启用（或 `auditorAwareRef` 为空）
**And** `@CreatedBy`/`@LastModifiedBy` 保持 null
**And** `@CreatedDate`/`@LastModifiedDate` 仍正常填充

**Given** Spring 容器中存在 `AuditorAware<String>` Bean
**When** 启动应用
**Then** JPA Auditing 启用并注入该 `AuditorAware`
**And** `@CreatedBy`/`@LastModifiedBy` 被正确填充

### AC6: 边界场景处理

| 场景 | 约定 |
|------|------|
| **新建实体** | `deleted = false`；`createdAt`/`lastModifiedDate`/`createdBy`/`lastModifiedBy` 首次保存前为 null，保存后由 JPA Auditing 自动填充 |
| **多次删除同一实体** | 允许，幂等；`deleted` 保持 `true`，不抛异常 |
| **保存已删除实体** | 允许；`@SQLRestriction` 仅影响查询（WHERE 条件），不阻止 UPDATE 操作 |
| **AuditorAware 返回 null** | `@CreatedBy`/`@LastModifiedBy` 保持 null，不抛异常 |

---

## 约束

### 技术约束

| 约束 | 说明 |
|------|------|
| **Spring Data JPA** | 使用 JPA 原生审计功能，不引入额外依赖 |
| **不依赖 Spring Security** | 通过 `AuditorAware` 接口与安全层解耦 |
| **条件装配** | 使用 `@ConditionalOnBean(AuditorAware.class)` 控制 auditing 启用 |
| **ArchUnit 合规** | 基类位于 `cartisan-data-jpa` 模块，符合分层架构 |

### 性能约束

- `@SQLRestriction` 在所有查询时自动生效，需注意对大数据量查询的影响（业务层可通过自定义查询绕过）

### 安全约束

- 软删除的实体只是查询过滤，物理数据仍存在，敏感数据需额外加密处理

---

## 参考资料

- 设计文档：[cartisan-boot-设计文档.md](../../cartisan-boot-设计文档.md)
- Spring Data JPA Auditing：https://docs.spring.io/spring-data/jpa/reference/jpa/auditing.html
- Epic Backlog：[00_epic_backlog.md](../00_epic_backlog.md)
- SKILL.md：[SKILL.md](../../../skills/SKILL.md)
