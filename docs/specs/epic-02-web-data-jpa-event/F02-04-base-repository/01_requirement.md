# Feature: F02-04 BaseRepository

## 背景

在 DDD 架构中，Repository 是聚合根的持久化入口。cartisan-boot 需要提供强类型的 Repository 基类，在编译期强制"只有聚合根才能拥有 Repository"这一约束，防止非聚合根实体绕过聚合根直接访问数据库。

## 目标

- 创建 `cartisan-data-jpa` 模块
- 定义 `BaseRepository<T, ID>` 接口，通过泛型约束 `T extends AggregateRoot<?>` 强制只有聚合根才能有 Repository
- 继承 Spring Data JPA 的 `JpaRepository` 和 `JpaSpecificationExecutor`，提供标准 CRUD 和动态查询能力
- 为后续 F02-05（BaseRepositoryImpl 事件自动发布）打下基础

## 范围

### 包含（In Scope）

- 创建 `cartisan-data-jpa` 模块骨架（`build.gradle.kts`、`settings.gradle.kts` 修改）
- 包结构：`com.cartisan.data.jpa.repository`
- `BaseRepository<T extends AggregateRoot<?>, ID extends Serializable>` 接口
- 接口 JavaDoc（含使用示例）
- `@NoRepositoryBean` 注解防止 Spring Data 为基类创建代理
- `package-info.java` 包说明文档

### 不包含（Out of Scope）

- `BaseRepositoryImpl` 实现（F02-05）
- 事件自动发布逻辑（F02-05）
- `Auditable` / `SoftDeletable` 基类（F02-06）
- `TsidGenerator` 分布式 ID（F02-07）
- Spring Boot AutoConfiguration（F02-09）
- 测试代码（测试留给 F02-05，因为纯接口无单测价值）

## 验收标准（Acceptance Criteria）

- **AC1**：`cartisan-data-jpa` 模块编译通过
- **AC2**：`BaseRepository` 接口定义正确，泛型约束 `T extends AggregateRoot<?>` 生效
- **AC3**：非聚合根实体无法继承 `BaseRepository`（编译期约束有效）
- **AC4**：聚合根实体继承 `BaseRepository` 后，拥有 Spring Data JPA 的所有 CRUD 方法
- **AC5**：接口 JavaDoc 完整，包含用途说明、与 BaseRepositoryImpl 关系、使用示例
- **AC6**：`@NoRepositoryBean` 注解正确应用，Spring 不会为 BaseRepository 本身创建 Bean

## 约束

### 技术约束

- 依赖 `cartisan-core` 模块的 `AggregateRoot` 接口
- 依赖 `spring-boot-starter-data-jpa`
- Java 21
- 必须使用 `@NoRepositoryBean` 注解

### 架构约束

- 遵循 DDD 原则：只有聚合根才能有 Repository
- 接口不包含实现代码
- 遵循 YAGNI：不预留 `id`/`audit`/`config` 空包

### DDD 分层约束

- `BaseRepository` 位于 `cartisan-data-jpa` 模块（基础设施层）
- 继承 `cartisan-core` 的 `AggregateRoot`（领域层）
- 模块依赖方向正确：基础设施层 → 领域层
