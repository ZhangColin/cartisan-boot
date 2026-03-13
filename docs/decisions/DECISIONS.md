# 架构决策记录（ADR）

## ADR-001：选用 Gradle Kotlin DSL 而非 Maven

- **日期**：2026-03-11
- **决策**：使用 Gradle Kotlin DSL 管理多模块项目
- **理由**：多模块灵活管理，Version Catalog 统一版本，Kotlin DSL 有类型检查和 IDE 补全
- **替代方案**：Maven（社区更大但多模块配置繁琐）

## ADR-002：Java 21 + Virtual Threads

- **日期**：2026-03-11
- **决策**：使用 Java 21，启用 Virtual Threads
- **理由**：AI 平台 SSE 流式输出场景高并发，Virtual Threads 大幅提升吞吐量；Record、Sealed Classes 提升代码质量
- **替代方案**：Java 17（稳定但缺少新特性）

## ADR-003：认证层选用 Sa-Token，通过抽象层封装

- **日期**：2026-03-11
- **决策**：底层用 Sa-Token，但业务代码只依赖 cartisan-security 的抽象接口
- **理由**：Sa-Token 轻量实用；抽象层确保将来可无痛替换为 Spring Security
- **替代方案**：Spring Security（功能强大但配置复杂）

## ADR-004：cartisan-boot 收纳准则

- **日期**：2026-03-11
- **决策**：技术能力进入框架须满足：① 不需要业务数据库表 ② 不同项目调用方式一致 ③ API 足够稳定
- **理由**：保持框架纯粹性，避免混入业务逻辑

## ADR-005：持久化采用 JPA（写）+ jOOQ（读）

- **日期**：2026-03-11
- **决策**：写侧用 Spring Data JPA，读侧用 jOOQ
- **理由**：JPA 与 DDD 聚合根天然适配；jOOQ 类型安全 SQL，编译期捕获错误，AI 生成代码更可靠
- **替代方案**：MyBatis-Plus（读写都行，但类型安全弱）

## ADR-006：Entity.sameIdentityAs() 保留运行时类型检查

- **日期**：2026-03-13
- **决策**：Entity.sameIdentityAs() 方法中保留 `getClass()` 检查和强制类型转换
- **理由**：由于 Java 泛型类型擦除，接口方法签名中的 `T` 在运行时被擦除为 `Object`，无法直接调用 `other.getId()`
- **代码对比**：
  ```java
  // ❌ 无法编译：other 被擦除为 Object 类型
  default boolean sameIdentityAs(T other) {
      return Objects.equals(this.getId(), other.getId());
  }

  // ✅ 正确实现：需要类型检查和强制转换
  @SuppressWarnings("unchecked")
  default boolean sameIdentityAs(T other) {
      if (other == null) return false;
      if (this.getClass() != other.getClass()) return false;
      ID otherId = ((Entity<T, ID>) other).getId();
      return Objects.equals(this.getId(), otherId);
  }
  ```
- **替代方案**：使用 `Object getId()` 声明（失去类型安全）

## ADR-007：ValueObject.sameValueAs() 简化为直接委托 equals()

- **日期**：2026-03-13
- **决策**：ValueObject.sameValueAs() 直接调用 `this.equals(other)`，无需类型检查
- **理由**：`equals()` 方法接受 `Object` 类型，不需要类型擦除的兼容处理；Record 实现的 equals() 已经正确处理类型检查
- **代码对比**：
  ```java
  // ✅ 简洁实现
  default boolean sameValueAs(T other) {
      if (other == null) return false;
      return this.equals(other);
  }
  ```
- **替代方案**：像 Entity 一样进行类型检查（冗余）

## ADR-008：domain 包零外部依赖约束

- **日期**：2026-03-13
- **决策**：cartisan-core.domain 包仅依赖 JDK 标准库，不依赖任何第三方库
- **理由**：
  1. 纯粹的领域抽象，可被任何技术栈复用
  2. 便于单元测试，无 Mock 依赖
  3. 降低框架迁移成本
- **验证**：通过 ArchUnit 自动化规则验证
- **例外**：测试代码使用 JUnit 5、AssertJ、ArchUnit（仅在 test 作用域）

## ADR-009：F01-02 不包含 Auditable/SoftDeletable

- **日期**：2026-03-13
- **决策**：F01-02 仅实现 DDD 核心抽象（AggregateRoot/Entity/ValueObject/Identity/DomainEvent），不包含审计（Auditable）和软删除（SoftDeletable）
- **理由**：
  1. 审计和软删除属于**基础设施关注点**，非 DDD 核心概念
  2. 不同业务对审计字段要求不同（如 created_by、updated_by、tenant_id 等），框架层难以统一
  3. 软删除策略（逻辑删除 vs 物理删除标记位）应由业务项目根据需求选择
- **后续规划**：在 cartisan-data-jpa 模块中提供 JPA 相关的基础设施（如 @MappedSuperclass 的审计基类）
- **替代方案**：在 domain 层定义 Auditable 接口（会导致所有实体依赖持久化概念，违反 DDD 分层原则）