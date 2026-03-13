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

## ADR-010：异常体系使用 MessageFormat 而非 SLF4J/占位符替换

- **日期**：2026-03-13
- **状态**：已实施
- **决策**：异常消息参数化使用 `java.text.MessageFormat`，占位符语法为 `{0}`, `{1}` 等
- **理由**：
  1. MessageFormat 是 JDK 标准库，符合零外部依赖原则
  2. 支持数字、日期等复杂格式化（如 `{0,number}`、`{0,date}`）
  3. 参数不足时保留占位符，而非抛出异常（更宽容的行为）
- **代码示例**：
  ```java
  // 定义
  INVALID_PARAMETER(400, "INVALID_PARAMETER", "Invalid parameter: {0}")

  // 使用
  throw new DomainException(BaseCodeMessage.INVALID_PARAMETER, "email");
  // 结果：getMessage() 返回 "Invalid parameter: email"
  ```
- **边界行为**：
  - 无参数：返回原始模板 `"Invalid parameter: {0}"`
  - 参数不足：保留未替换的占位符 `"Error type at {1}"`
  - 多余参数：忽略
- **替代方案**：
  - SLF4J 占位符 `{}`（引入外部依赖，且仅支持日志场景）
  - String.format（占位符为 `%s`，与日志框架不一致）
  - 字符串拼接（无法预定义模板）

## ADR-011：异常基类构造器 NPE 检查必须在 formatMessage 调用之前

- **日期**：2026-03-13
- **状态**：已实施（代码审查修复）
- **决策**：`CartisanException` 构造器中 `Objects.requireNonNull(codeMessage)` 必须在 `formatMessage()` 调用之前执行
- **理由**：
  1. 如果 codeMessage 为 null，在 formatMessage 中调用 `codeMessage.message()` 会抛出 NPE
  2. 该 NPE 堆栈不清晰，不会显示 "codeMessage cannot be null" 的错误消息
  3. 将 requireNonNull 放在前面，可以提供更清晰的错误信息
- **代码对比**：
  ```java
  // ❌ 错误：formatMessage 先执行，NPE 堆栈不清晰
  protected CartisanException(CodeMessage codeMessage, Object... args) {
      super(formatMessage(codeMessage, args));  // NPE here
      this.codeMessage = Objects.requireNonNull(codeMessage, "codeMessage cannot be null");
  }

  // ✅ 正确：先检查 null，提供清晰错误信息
  protected CartisanException(CodeMessage codeMessage, Object... args) {
      super(formatMessage(
              Objects.requireNonNull(codeMessage, "codeMessage cannot be null"),
              args
      ));
  }
  ```
- **替代方案**：在构造器最后检查（无法覆盖 super() 调用）

## ADR-012：异常分层仅包含 Domain 和 Application 两层

- **日期**：2026-03-13
- **状态**：已实施
- **决策**：异常体系仅提供 `DomainException` 和 `ApplicationException`，不包含 `InfrastructureException`
- **理由**：
  1. 基础设施异常应在**端口适配器**中被转换为领域或应用异常
  2. 避免基础设施泄漏到领域层（违反 DDD 分层原则）
  3. 两层已覆盖 DDD 六边形架构的所有场景
- **转换示例**：
  ```java
  // 在 Repository 实现（端口适配器）中
  try {
      jpaRepository.save(entity);
  } catch (DataIntegrityViolationException e) {
      throw new DomainException(BaseCodeMessage.DUPLICATE, e, "email");
  }
  ```
- **替代方案**：
  - 添加 `InfrastructureException`：会导致领域层可能依赖基础设施异常类型
  - 统一使用 `RuntimeException`：丢失错误语义和分层信息

## ADR-013：PortType 设计为独立枚举而非注解内嵌

- **日期**：2026-03-13
- **状态**：已实施
- **决策**：`PortType` 作为独立的顶层枚举类，被 `@Port` 和 `@Adapter` 两个注解共享
- **理由**：
  1. `@Port` 和 `@Adapter` 都需要引用端口类型，内嵌在任一注解中都会导致语义别扭
  2. `@Adapter(Port.Type.REPOSITORY)` 语义错误——Adapter 不属于 Port
  3. 独立枚举 API 更简洁：`@Port(PortType.REPOSITORY)` vs `@Port(Port.Type.REPOSITORY)`
  4. 未来扩展无压力（如新增 @Gateway 注解引用 PortType）
- **替代方案**：内嵌在 `@Port` 注解中（会导致 Adapter 引用 Port 的内部类型）

## ADR-014：@BoundedContext 不支持 @Repeatable

- **日期**：2026-03-13
- **状态**：已实施
- **决策**：一个包只能属于一个限界上下文，不支持 `@Repeatable`
- **理由**：
  1. DDD 的限界上下文边界应该是清晰的
  2. 框架应该让架构错误不可表达
  3. 需要多重标注的场景应通过 shared kernel 表达
- **实现**：子包继承由 F01-07 的 ArchUnit 规则向上查找实现
- **替代方案**：支持 `@Repeatable`（会模糊边界，违背 DDD 原则）

## ADR-015：SubDomain 枚举不包含 OTHER 值

- **日期**：2026-03-13
- **状态**：已实施
- **决策**：`SubDomain` 只有三个标准值（CORE, SUPPORTING, GENERIC），无 OTHER 值
- **理由**：
  1. Core/Supporting/Generic 是一个完备分类（决策树穷举）
  2. 框架应强制思考，不提供逃避路径
  3. 判断子域归属是 DDD 战略设计的重要决策，不应有"不确定"选项
- **决策树**：
  ```
  这个领域能力是否构成业务核心竞争力？
    ├─ 是 → CORE
    └─ 否 → 是否业务流程必须有它？
        ├─ 是 → SUPPORTING
        └─ 否 → GENERIC
  ```
- **替代方案**：增加 OTHER 值（会让开发者跳过重要思考）