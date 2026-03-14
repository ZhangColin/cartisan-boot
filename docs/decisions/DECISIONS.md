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

## ADR-016：ensure 断言使用 IllegalStateException 而非 DomainException

- **日期**：2026-03-13
- **状态**：已实施
- **决策**：`Assertions.ensure(condition, message)` 失败时抛出 `IllegalStateException`，而非 `DomainException`
- **理由**：
  1. **语义差异**：后置条件失败表示**代码有 bug**，而非业务规则违反
  2. **HTTP 映射**：IllegalStateException 被全局异常处理器映射为 500，DomainException 映射为 4xx
  3. **运维区分**：500 错误触发 bug 告警，4xx 错误属于正常业务拒绝
  4. **责任归属**：
     | 断言类型 | 责任方 | 异常类型 | HTTP | 运维处理 |
     |---------|--------|---------|------|---------|
     | require（前置条件） | 调用者 | DomainException | 4xx | 正常日志 |
     | ensure（后置条件） | 实现者 | IllegalStateException | 500 | Bug 告警 |
- **代码示例**：
  ```java
  // 领域方法中的典型用法
  public void addItem(OrderItem item) {
      // 前置条件：调用者的责任
      Assertions.require(item != null, OrderError.ITEM_REQUIRED);
      this.items.add(item);

      // 后置条件：实现者的责任，失败 = 我有 bug
      Assertions.ensure(this.items.contains(item), "item should be present after add");
  }
  ```
- **替代方案**：
  - 使用 `DomainException`：会误导运维认为是业务异常，而非代码 bug
  - 使用 `IllegalArgumentException`：语义是"参数非法"，属于前置条件范畴，非后置条件
- **参考**：DbC（Design by Contract）理论，后置条件违反是内部不变量被破坏，表示实现有缺陷

## ADR-017：ArchUnit 测试使用 importPaths 而非 importPackages

- **日期**：2026-03-13
- **状态**：已实施
- **决策**：`CartisanCoreModuleTest` 使用 `importPaths("build/classes/java/main")` 导入生产代码
- **理由**：
  1. `importPackages("com.cartisan.core")` 会同时导入测试代码（如 `CartisanCoreModuleTest` 自身）
  2. 测试规则不应该检查测试代码本身（自指问题）
  3. 使用 `importPaths()` 直接指向编译输出目录，只包含生产代码
  4. 与现有 `ArchitectureTest.java` 的模式保持一致
- **代码示例**：
  ```java
  // ✅ 正确：只导入生产代码
  private final JavaClasses productionClasses = new ClassFileImporter()
          .importPaths("build/classes/java/main");

  // ❌ 错误：会导入测试代码
  private final JavaClasses allClasses = new ClassFileImporter()
          .importPackages("com.cartisan.core");
  ```
- **前提条件**：测试前需要先运行 `compileJava`，确保 `build/classes/java/main` 存在
- **替代方案**：
  - `importPackages()` + 过滤测试类（复杂，且测试类位于不同包结构）
  - 使用 ArchUnit 的 `importClasspath()`（性能较差，且 API 不稳定）

## ADR-018：javadoc 任务绑定到 build 而非单独任务

- **日期**：2026-03-13
- **状态**：已实施
- **决策**：`tasks.build { dependsOn(tasks.javadoc) }`，每次 build 都执行 javadoc 校验
- **理由**：
  1. JavaDoc 格式错误也是代码质量问题，应在每次构建时检查
  2. 使用 `-Xdoclint:all,-missing` 配置，只检查格式不强制必须存在文档
  3. javadoc 任务执行快速（通常 < 5 秒），对开发效率影响有限
  4. 确保 JavaDoc 始终与代码保持同步
- **配置**：
  ```kotlin
  tasks.javadoc {
      (options as StandardJavadocDocletOptions).apply {
          addStringOption("Xdoclint:all,-missing", "-quiet")
      }
  }
  tasks.build {
      dependsOn(tasks.javadoc)
  }
  ```
- **替代方案**：
  - 仅在 CI 中启用（开发者本地可能忽略 JavaDoc 格式问题）
  - 使用单独的 `verify` 任务（需要开发者记住执行）
- **权衡**：轻微的性能开销换取更好的代码质量保证
## ADR-019：CartisanArchRules 使用直接字段引用而非 ArchRules.in()

- **日期**：2026-03-13
- **状态**：已实施
- **决策**：`CartisanArchRules` 使用直接字段引用聚合规则，而非规范中设想的 `ArchRules.in(Class)` 模式
- **背景**：
  1. 规范文档（02_interface.md）设想使用 `ArchRules.in(CartisanLayeringRules.class)` 组合规则
  2. 实现时发现 `ArchRules` 类和 `in()` 方法在 ArchUnit 1.3.0 中**不存在**
  3. 检查了 archunit-1.3.0.jar 和 archunit-junit5-api-1.3.0.jar，确认无此 API
- **实现方式**：
  ```java
  // 实际实现
  public class CartisanArchRules {
      @ArchTest
      static final ArchRule domainShouldNotDependOnInfrastructure =
          CartisanLayeringRules.domainShouldNotDependOnInfrastructure;
      // ... 其他 10 条规则
  }
  ```
- **理由**：
  1. 直接字段引用在功能上等价于组合 - 业务项目继承后仍可获得全部规则
  2. 规则字段有清晰的 JavaDoc，业务项目可查看具体规则内容
  3. 不影响业务项目的三种使用姿势（继承全部、选择部分、追加自定义）
  4. ArchUnit 的 `@ArchTest` 字段继承机制已经提供了分组能力
- **影响评估**：
  - ✅ 业务项目使用方式不变：`extends CartisanArchRules` 即可获得全部守护
  - ✅ 规则字段命名清晰，IDE 可以跳转到具体规则定义
  - ⚠️ 规范文档（02_interface.md）需要更新，以反映实际实现
- **替代方案**：
  - 使用 `@ArchTests` 注解的静态方法返回规则组（ArchUnit 1.3.0 中同样不存在）
  - 等待 ArchUnit 添加 `ArchRules.in()` API（不切实际，时间表未知）
- **后续行动**：
  - 更新 02_interface.md，移除不存在的 `ArchRules.in()` 模式描述
  - 如果未来 ArchUnit 添加组合 API，评估是否迁移

## ADR-020：Testcontainers 使用 @ServiceConnection 而非 @DynamicPropertySource

- **日期**：2026-03-13
- **状态**：已实施（F01-08）
- **决策**：Testcontainers 容器配置使用 Spring Boot 3.4 的 `@ServiceConnection` 自动注入连接属性，而非手动编写 `@DynamicPropertySource`
- **理由**：
  1. `@ServiceConnection` 是 Spring Boot 3.1+ 专门为 Testcontainers 设计的官方方式
  2. 自动识别容器类型并注入对应属性（DataSource、Redis 连接等），无需手动配置
  3. 类型安全，容器变更时自动适配
  4. 代码更简洁，零样板代码
- **代码对比**：
  ```java
  // ❌ 旧方式：手动配置属性
  @Testcontainers
  class MyTest {
      @Container
      static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

      @DynamicPropertySource
      static void configureProperties(DynamicPropertyRegistry registry) {
          registry.add("spring.datasource.url", postgres::getJdbcUrl);
          registry.add("spring.datasource.username", postgres::getUsername);
          registry.add("spring.datasource.password", postgres::getPassword);
      }
  }

  // ✅ 新方式：自动注入
  @TestConfiguration(proxyBeanMethods = false)
  public class PostgresTestContainer {
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
- **替代方案**：
  - 手动 `@DynamicPropertySource`：繁琐，容易遗漏属性，每个容器类型都要写一遍

## ADR-021：集成测试数据清理使用 TRUNCATE ... CASCADE

- **日期**：2026-03-13
- **状态**：已实施（F01-08）
- **决策**：`IntegrationTestBase` 使用 `TRUNCATE ... CASCADE` 清理数据库，配合 Redis `FLUSHDB`
- **理由**：
  1. `TRUNCATE ... CASCADE` 一次性清空所有表，PostgreSQL 自动处理外键约束
  2. 不需要排序表、不需要禁用约束
  3. 比 `DELETE` 快得多（不记录 WAL，直接释放页面）
  4. 比逐表 `DELETE` 更安全（`DELETE` 可能触发触发器）
- **SQL 实现**：
  ```sql
  DO $$
  DECLARE
      tables TEXT;
  BEGIN
      SELECT string_agg(tablename, ', ') INTO tables
      FROM pg_tables
      WHERE schemaname = 'public'
        AND tablename != 'flyway_schema_history';  -- 排除 Flyway 表
      IF tables IS NOT NULL THEN
          EXECUTE 'TRUNCATE TABLE ' || tables || ' CASCADE';
      END IF;
  END $$
  ```
- **替代方案**：
  - 逐表 `DELETE FROM table`：慢，且需要按外键依赖顺序排序
  - 禁用外键约束后清理：不安全，可能破坏数据完整性
  - `@DirtiesContext`：性能差，每次测试后重建整个 Spring 上下文

## ADR-022：测试基类不封装 MockMvc API

- **日期**：2026-03-13
- **状态**：已实施（F01-08）
- **决策**：`ApiTestBase` 只暴露 `protected MockMvc mvc` 字段，不封装 `get/post/put/delete` 便捷方法
- **理由**：
  1. MockMvc 的 fluent API 已经足够清晰
  2. 便捷方法覆盖不了真实场景（查询参数、Authorization header、multipart、PATCH 等）
  3. 每新增一个场景就要加一个重载（如 `postWithAuth`），基类会不断膨胀
  4. 保持薄基类原则：`ApiTestBase` 的价值是组合（IntegrationTestBase + MockMvc），不是二次封装
- **代码示例**：
  ```java
  @AutoConfigureMockMvc
  public abstract class ApiTestBase extends IntegrationTestBase {
      @Autowired
      protected MockMvc mvc;  // 只暴露 MockMvc，不封装
  }

  // 业务项目直接使用
  mvc.perform(post("/api/v1/orders")
          .contentType(MediaType.APPLICATION_JSON)
          .content(orderJson)
          .header("Authorization", "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.id").value("O001"));
  ```
- **替代方案**：
  - 封装 `get/post/put/delete` 方法：无法覆盖所有场景，方法名与静态导入冲突（无限递归 bug）

## ADR-023：不验证第三方库的承诺

- **日期**：2026-03-13
- **状态**：已实施（F01-08）
- **决策**：不为 Testcontainers 的 Virtual Threads 兼容性编写验证测试
- **理由**：
  1. Testcontainers 1.20+ 声明支持 Virtual Threads，这是它们的兼容性保证
  2. 写测试"验证"第三方库的承诺本质上是测试第三方库的代码，不是我们的代码
  3. 如果真的有 bug，我们的验证测试也帮不了什么——应该报 issue 给 Testcontainers 项目
  4. 这和"不测 JDK 的注解机制"是同一个原则
- **口诀**："不要测试别人的承诺"
- **适用范围**：
  - ✅ 第三方库明确声明支持的功能
  - ✅ 标准库（JDK）的行为
  - ❌ 我们自己的代码逻辑
- **替代方案**：
  - 写验证测试确保 Virtual Threads 下容器正常工作（浪费资源，且不会发现真正的 bug）

## ADR-024：API 测试断言方法返回 ResultMatcher 而非 ResultActions

- **日期**：2026-03-14
- **状态**：已实施（F01-09）
- **决策**：`ApiTestAssertions` 的断言方法返回 `ResultMatcher`，用于 MockMvc 的 `.andExpect()`，而非接受 `ResultActions` 参数
- **理由**：
  1. `ResultMatcher` 更符合 MockMvc 的习惯用法（`.andExpect(status().isOk())`）
  2. 使用 `.andExpect(ApiTestAssertions.assertOk())` 比 `.andDo(ApiTestAssertions::assertOk)` 更清晰
  3. 与 MockMvc 的 `MockMvcResultMatchers` 风格一致，降低学习成本
  4. 实现比原始规格更简洁（无需接受和返回 `ResultActions`）
- **代码对比**：
  ```java
  // ❌ 原规格设计：接受 ResultActions 参数
  public static ResultActions assertOk(ResultActions result) throws Exception {
      return result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
  }
  // 使用：.andDo(ApiTestAssertions::assertOk)

  // ✅ 实际实现：返回 ResultMatcher
  public static ResultMatcher assertOk() {
      return result -> {
          status().isOk().match(result);
          jsonPath("$.code").value(200).match(result);
      };
  }
  // 使用：.andExpect(ApiTestAssertions.assertOk())
  ```
- **影响**：规格文档（02_interface.md）已更新以反映实际设计
- **替代方案**：
  - 坚持原始规格：会导致使用方式不一致（部分方法用 `.andDo()`，部分用 `.andExpect()`）

## ADR-025：RequestPostProcessor 正确导入路径

- **日期**：2026-03-14
- **状态**：已实施（F01-08）
- **决策**：`RequestPostProcessor` 的正确导入路径是 `org.springframework.test.web.servlet.request.RequestPostProcessor`
- **理由**：
  1. Spring Test 的 JAR 包结构中，`RequestPostProcessor` 位于 `request` 子包
  2. 常见错误假设是在 `org.springframework.test.web.servlet.RequestPostProcessor`
  3. 错误导入会导致编译失败："找不到符号"
- **正确导入**：
  ```java
  import org.springframework.test.web.servlet.ResultActions;
  import org.springframework.test.web.servlet.request.RequestPostProcessor;  // 注意 request 子包
  ```
- **踩坑记录**：
  - 查找 JAR 包内容：`find ~/.gradle/caches -name "spring-test-*.jar" | xargs jar tf | grep RequestPostProcessor`
  - 确认路径：`org/springframework/test/web/servlet/request/RequestPostProcessor.class`
- **替代方案**：
  - 使用 IDE 自动导入（可能导入错误的路径，导致编译失败）

## ADR-026：cartisan-test 依赖 Spring Test 需显式声明

- **日期**：2026-03-14
- **状态**：已实施（F01-09）
- **决策**：`cartisan-test` 模块的 build.gradle.kts 需要同时声明 `api` 和 `implementation` 依赖
- **理由**：
  1. `api` 配置将依赖暴露给使用者，但不会对本模块的 main 代码编译可用
  2. `implementation` 配置确保本模块 main 代码可以编译使用这些类
  3. 这是 Gradle 依赖配置的特性，`api` ≠ 本模块可用的传递依赖
- **代码示例**：
  ```kotlin
  // Spring Test（MockMvc、ResultActions、RequestPostProcessor）
  api("org.springframework:spring-test:6.2.0")
  implementation("org.springframework:spring-test:6.2.0")  // 必须同时声明

  // Jackson（JSON 序列化，ApiTestAssertions 需要）
  implementation("com.fasterxml.jackson.core:jackson-databind")
  ```
- **影响**：
  - 如果只声明 `api`，编译时会出现"找不到符号"错误
  - 如果只声明 `implementation`，业务项目无法使用这些类
- **替代方案**：
  - 使用 `compileOnly` + `api`：Gradle 不支持这种组合，且语义不清晰

## ADR-027：临时使用 IllegalArgumentException 模拟 AccessDeniedException

- **日期**：2026-03-14
- **状态**：已实施（F02-02）
- **决策**：在 `cartisan-web` 模块中使用 `IllegalArgumentException` 代替 Spring Security 的 `AccessDeniedException`
- **理由**：
  1. cartisan-web 不应强制依赖 Spring Security（保持框架轻量）
  2. 通过检查异常消息是否包含 "Access denied" 来区分权限拒绝和普通参数错误
  3. 实际项目中引入 Spring Security 后，应替换为真正的 `AccessDeniedException` 处理器
- **代码示例**：
  ```java
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(IllegalArgumentException ex) {
      if (ex.getMessage() != null && ex.getMessage().contains("Access denied")) {
          log.warn("Access denied: {}", ex.getMessage());
          return ResponseEntity.status(HttpStatus.FORBIDDEN)
                  .body(ApiResponse.error(BaseCodeMessage.FORBIDDEN));
      }
      // 其他 IllegalArgumentException 作为通用 400 处理
      log.warn("Bad request: {}", ex.getMessage());
      return ResponseEntity.badRequest()
              .body(ApiResponse.error(400, ex.getMessage()));
  }
  ```
- **后续行动**：在 cartisan-security 模块中实现真正的 `AccessDeniedException` 处理器
- **替代方案**：
  - 强制依赖 Spring Security：违背框架轻量原则

## ADR-028：全局异常日志策略 4xx → WARN、5xx → ERROR

- **日期**：2026-03-14
- **状态**：已实施（F02-02）
- **决策**：全局异常处理器根据 HTTP 状态码决定日志级别和堆栈打印
- **规则**：
  | HTTP 状态码范围 | 日志级别 | 打印堆栈 | 理由 |
  |---------------|---------|---------|------|
  | 4xx | WARN | 否 | 客户端错误，正常业务拒绝 |
  | 5xx | ERROR | 是 | 服务器错误，需要运维关注 |
- **实现细节**：
  ```java
  // CartisanException 根据状态码判断
  @ExceptionHandler(CartisanException.class)
  public ResponseEntity<ApiResponse<Void>> handleCartisanException(CartisanException ex) {
      int status = ex.getCodeMessage().httpStatus();
      if (status >= 500) {
          log.error("Business error: {}", ex.getMessage(), ex);  // 5xx + 堆栈
      } else {
          log.warn("Business error: {}", ex.getMessage());  // 4xx，无堆栈
      }
      // ...
  }
  ```
- **理由**：
  1. 4xx 错误是正常的业务拒绝（如参数校验失败），不应产生大量 ERROR 日志
  2. 5xx 错误表示服务端异常，需要立即告警并定位问题
  3. 不打印堆栈减少日志量，避免干扰关键错误追踪
- **替代方案**：
  - 所有异常都打印堆栈：日志量巨大，关键错误被淹没

## ADR-029：Testcontainers 版本选择与 Docker 兼容性

- **日期**：2026-03-14
- **状态**：已实施
- **决策**：使用 Testcontainers 1.21.4+ 以兼容 Docker Engine 29 / Docker Desktop 4.59+
- **理由**：
  1. Testcontainers 1.20.x 及以下版本与 Docker Engine 29 不兼容
  2. 1.21.4 发布说明明确写道："This release makes version 1.21.x works with recent Docker Engine changes."
  3. 典型错误：`Could not find a valid Docker environment`（但 Docker CLI 正常工作）
- **影响范围**：cartisan-test 模块
- **依赖配置**：
  ```kotlin
  // gradle/libs.versions.toml
  testcontainers = "1.21.4"
  ```
- **验证方式**：
  ```bash
  ./gradlew :cartisan-test:test --info | grep "Container is started"
  # 应输出：Container postgres:16-alpine started in PT0.6s
  ```
- **附加依赖**：需显式添加 PostgreSQL JDBC 驱动
  ```kotlin
  runtimeOnly("org.postgresql:postgresql:42.7.4")
  ```
- **替代方案**：
  - 降级 Docker Desktop：不现实，新版本有安全修复和新特性
  - 使用 Docker 环境变量绕过：无效，这是 API 兼容性问题而非配置问题

## ADR-030：DomainEvent 直接发布，不使用自定义包装类

- **日期**：2026-03-14
- **状态**：已实施（F02-08）
- **决策**：`SpringDomainEventPublisher` 直接发布 `DomainEvent`，利用 Spring 4.2+ 的 `PayloadApplicationEvent` 机制，不创建自定义包装类
- **理由**：
  1. Spring 4.2+ 的 `ApplicationEventPublisher.publishEvent(Object)` 可以发布任意对象
  2. Spring 自动将非 `ApplicationEvent` 对象包装为 `PayloadApplicationEvent<T>`
  3. 监听器直接接收 `DomainEvent` 子类，无需解包
  4. cartisan-core 保持零 Spring 依赖
- **代码示例**：
  ```java
  // 发布器：直接发布 DomainEvent
  @Override
  public void publish(DomainEvent event) {
      applicationEventPublisher.publishEvent(event);  // Spring 自动包装
  }

  // 监听器：直接接收领域事件子类
  @EventListener
  void handle(OrderCreatedEvent event) {  // 直接收 DomainEvent 子类
      // ...
  }
  ```
- **替代方案**：
  - 创建 `SpringDomainEvent extends ApplicationEvent` 包装类：需要解包，增加复杂度

## ADR-031：事件发布使用 @ConditionalOnMissingBean 支持用户覆盖

- **日期**：2026-03-14
- **状态**：已实施（F02-08）
- **决策**：自动配置使用 `@ConditionalOnMissingBean(DomainEventPublisher.class)`，不提供 `enabled` 配置开关
- **理由**：
  1. 用户可通过自定义 Bean 覆盖默认实现（如发到 Kafka）
  2. 职责单一，无需"关闭但不提供替代"的场景
  3. 符合 YAGNI 原则
- **代码示例**：
  ```java
  @Configuration
  @ConditionalOnMissingBean(DomainEventPublisher.class)
  public class CartisanEventAutoConfiguration {
      @Bean
      public DomainEventPublisher domainEventPublisher(ApplicationEventPublisher publisher) {
          return new SpringDomainEventPublisher(publisher);
      }
  }
  ```
- **替代方案**：
  - 添加 `cartisan.event.enabled` 配置开关：过度设计，当前无需求

## ADR-032：F02-05 选用 SimpleJpaRepository 继承方式实现事件自动发布

- **日期**：2026-03-14
- **状态**：设计决策（F02-05 Phase 1）
- **决策**：通过创建 `BaseRepositoryImpl extends SimpleJpaRepository` 重写 `save()` 方法，实现聚合根保存时自动发布领域事件
- **理由**：
  1. **SimpleJpaRepository 继承** 是替换 Repository 默认 save 行为的唯一途径
  2. Spring Data 的「自定义实现模式」（XxxRepositoryCustom + XxxRepositoryImpl）只能增加新方法，不能替换已有 save()
  3. 通过 `@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl.class)` 全局启用，用户只需继承 `BaseRepository<T, ID>` 接口
  4. 保持 `DomainEventPublisher` 作为统一发布入口，与 cartisan-event 模块设计一致
- **实现要点**：
  ```java
  // 核心逻辑
  @Override
  public <S extends T> S save(S entity) {
      S savedEntity = super.save(entity);  // 先持久化
      publishDomainEvents(savedEntity);     // 再发布事件
      return savedEntity;
  }

  private void publishDomainEvents(T entity) {
      if (entity instanceof AbstractAggregateRoot) {
          List<DomainEvent> events = ((AbstractAggregateRoot<?>) entity).getDomainEvents();
          events.forEach(domainEventPublisher::publish);
          ((AbstractAggregateRoot<?>) entity).clearDomainEvents();
      }
  }
  ```
- **替代方案**：
  - 自定义实现模式（XxxRepositoryCustom）：无法替换 save()，会导致双入口（save 不发布，saveAndPublishEvents 才发布）
  - Spring Data @DomainEvents 注解：脱离 cartisan-event 的 DomainEventPublisher，破坏统一设计

## ADR-033：F02-05 事件发布采用事务内同步模式

- **日期**：2026-03-14
- **状态**：设计决策（F02-05 Phase 1）
- **决策**：`BaseRepositoryImpl.save()` 在同一事务内同步发布事件，不使用 `@TransactionalEventListener(AFTER_COMMIT)`
- **约定**：
  1. 先 `super.save(entity)` 持久化
  2. 立即调用 `DomainEventPublisher.publish(...)` 发布事件
  3. 立即调用 `entity.clearDomainEvents()` 清空事件
- **监听器侧**：
  - 默认使用 `@EventListener`：与 save 在同一事务内同步执行，抛异常会导致整个事务回滚
  - 若需事务提交后执行：由业务使用 `@TransactionalEventListener(phase = AFTER_COMMIT)`，框架不强制
- **理由**：
  1. 实现简单，行为明确，符合"数据一致性"直觉
  2. cartisan-boot 定位是基础框架，应提供清晰的默认行为
  3. 复杂场景（异步事件、事件溯源）可由业务项目自行扩展
- **替代方案**：
  - 事务提交后发布：需要注册 TransactionSynchronization，需处理异常与补偿，复杂度高
  - 配置开关：YAGNI，当前无需求

## ADR-034：F02-05 仅重写 save(S entity)，不重写 saveAll/saveAndFlush

- **日期**：2026-03-14
- **状态**：设计决策（F02-05 Phase 1）
- **决策**：仅重写 `save(S entity)` 方法，不重写 `saveAll()` 和 `saveAndFlush()`
- **理由**：
  1. `SimpleJpaRepository.saveAll(Iterable<S>)` 内部遍历调用 `save(entity)`，会自动走重写后的逻辑
  2. `SimpleJpaRepository.saveAndFlush(S)` 实现：`save(entity)` + `flush()`，同样会走重写后的 save
  3. YAGNI：单一入口点足以覆盖所有保存场景
- **代码溯源**：
  ```java
  // SimpleJpaRepository 源码（Spring Data JPA）
  @Override
  public <S extends T> List<S> saveAll(Iterable<S> entities) {
      assertIterableNotNull(entities);  // 非空检查
      List<S> result = new ArrayList<>();
      for (S entity : entities) {
          result.add(save(entity));  // ← 循环调用 save()
      }
      return result;
  }

  @Override
  public <S extends T> S saveAndFlush(S entity) {
      S result = save(entity);  // ← 调用 save()
      flush();
      return result;
  }
  ```
- **替代方案**：
  - 同时重写 saveAll/saveAndFlush：代码重复，无额外收益

## ADR-035：F02-05 使用 FactoryBean + Factory 两层结构注入 DomainEventPublisher

- **日期**：2026-03-14
- **状态**：设计决策（F02-05 Phase 1）
- **决策**：通过自定义 `JpaRepositoryFactoryBean` + `JpaRepositoryFactory` 实现三参构造器注入
- **层次结构**：
  ```
  @EnableJpaRepositories(repositoryFactoryBeanClass = CartisanJpaRepositoryFactoryBean.class)
                                     │
                                     v
                   CartisanJpaRepositoryFactoryBean（FactoryBean）
                                     │
                                     │ createRepositoryFactory(EntityManager)
                                     v
                   CartisanJpaRepositoryFactory（Factory）
                                     │
                                     │ getTargetRepository(RepositoryInformation)
                                     v
                   BaseRepositoryImpl(entityInformation, entityManager, eventPublisher)
  ```
- **职责分工**：
  - **FactoryBean**：负责创建 Factory 实例，将 ApplicationContext 传入
  - **Factory**：负责创建 Repository 实例，从 ApplicationContext 获取 DomainEventPublisher 并注入
- **关键代码**：
  ```java
  // FactoryBean
  public class CartisanJpaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
          extends JpaRepositoryFactoryBean<T, S, ID> {
      @Override
      protected RepositoryFactorySupport createRepositoryFactory(EntityManager em) {
          return new CartisanJpaRepositoryFactory(em, getApplicationContext());
      }
  }

  // Factory
  public class CartisanJpaRepositoryFactory extends JpaRepositoryFactory {
      @Override
      protected Object getTargetRepository(RepositoryInformation information) {
          DomainEventPublisher publisher = applicationContext.getBean(DomainEventPublisher.class);
          return new BaseRepositoryImpl(entityInformation, entityManager, publisher);
      }
  }
  ```
- **替代方案**：
  - 只自定义 Factory：无法获得 ApplicationContext，无法获取 DomainEventPublisher

## ADR-008：Repository 事件发布采用静态持有者模式

- **日期**：2026-03-14
- **上下文**：F02-05 Repository 保存时自动发布领域事件
- **问题**：Spring Data JPA 创建的 Repository 实例不是 Spring Bean，无法通过 @Autowired 注入 DomainEventPublisher
- **决策**：使用 `DomainEventPublisherHolder` 静态持有者模式，在 AutoConfiguration 中通过回调设置 Publisher
- **理由**：
  - Repository 实例由 Spring Data JPA 动态代理创建，不在 Spring 容器中
  - 三参数构造函数（手动注入 Publisher）需要覆盖 final 方法，行不通
  - FactoryBean + Factory 方案过于复杂，需要侵入 Spring Data JPA 内部 API
- **代码**：
  ```java
  // 静态持有者
  public final class DomainEventPublisherHolder {
      private static volatile DomainEventPublisher publisher;

      public static void setPublisher(DomainEventPublisher publisher) {
          DomainEventPublisherHolder.publisher = Objects.requireNonNull(publisher);
      }

      public static DomainEventPublisher getPublisher() {
          return publisher;
      }
  }

  // AutoConfiguration 中设置
  @Bean
  public Runnable configureDomainEventPublisherHolder(DomainEventPublisher publisher) {
      return () -> DomainEventPublisherHolder.setPublisher(publisher);
  }
  ```
- **替代方案**：
  - 三参数构造函数：无法覆盖 final 方法
  - FactoryBean + Factory：过于复杂，需要维护自定义 Factory

## ADR-009：JPA save() 返回新实例，事件发布必须使用原始实体参数

- **日期**：2026-03-14
- **上下文**：F02-05 实现 BaseRepositoryImpl.save() 方法时发现
- **问题**：调用 `publishDomainEvents(savedEntity)` 时事件列表为空
- **原因**：JPA 的 `save()` 方法可能返回一个新实例（如延迟加载代理），而不是原始传入的实体
- **决策**：事件发布时使用原始 `entity` 参数，而非 `savedEntity` 返回值
- **代码**：
  ```java
  @Override
  public <S extends T> S save(S entity) {
      S savedEntity = super.save(entity);
      // 使用原始 entity 发布事件，因为 savedEntity 可能是新实例
      publishDomainEvents(entity);  // ✅ 正确
      return savedEntity;
  }
  ```
- **调试验证**：
  ```
  savedEntity.events = 0  // 空的！
  entity.events = 1       // 事件在这里
  ```
- **替代方案**：无，这是 JPA 规范行为

## ADR-036：F02-06 审计人通过 AuditorAware 接口与安全层解耦

- **日期**：2026-03-14
- **状态**：设计决策（F02-06 Phase 2）
- **决策**：审计字段（`@CreatedBy`/`@LastModifiedBy`）通过 Spring Data JPA 的 `AuditorAware<T>` 接口获取当前用户，由 cartisan-security 或业务项目实现
- **理由**：
  1. cartisan-boot 认证层使用 Sa-Token，不硬依赖 Spring Security
  2. `AuditorAware` 是 Spring Data JPA 标准接口，提供与安全框架解耦的扩展点
  3. cartisan-security 可实现 `AuditorAware<String>`，内部从 `SecurityContext` 获取当前用户
  4. 业务项目也可直接提供自己的实现（如从线程局部变量获取）
- **代码示例**：
  ```java
  // cartisan-security 中的实现（未来）
  @Bean
  public AuditorAware<String> auditorAware() {
      return () -> {
          String currentUser = SecurityContext.getCurrentUser();
          return Optional.ofNullable(currentUser);
      };
  }
  ```
- **替代方案**：
  - 直接使用 Spring Security 的 `SecurityContextHolder`：硬依赖，与 Sa-Token 冲突
  - 提供 "system"/"anonymous" 默认值：数据库大量无意义值，语义不准确

## ADR-037：F02-06 JPA Auditing 条件装配

- **日期**：2026-03-14
- **状态**：设计决策（F02-06 Phase 2）
- **决策**：使用 `@ConditionalOnBean(AuditorAware.class)` 控制是否启用 JPA Auditing
- **理由**：
  1. 无 `AuditorAware` Bean 时，`@CreatedBy`/`@LastModifiedBy` 保持 null，表示"未设置操作人"
  2. 有 `AuditorAware` Bean 时，自动启用 auditing 并注入该 Bean
  3. 不提供 "system"/"anonymous" 占位值，保持 null 语义更清晰
- **代码**：
  ```java
  @Configuration
  @ConditionalOnBean(AuditorAware.class)
  @EnableJpaAuditing(auditorAwareRef = "auditorAware")
  public class JpaAuditingConfiguration {
      // 无需额外代码，注解即完成配置
  }
  ```
- **替代方案**：
  - 无条件启用 + 默认实现：数据库大量无意义占位值

## ADR-038：F02-06 软删除使用 @SQLRestriction 实现

- **日期**：2026-03-14
- **状态**：设计决策（F02-06 Phase 2）
- **决策**：软删除使用 Hibernate 的 `@SQLRestriction("deleted = false")` 注解自动过滤查询
- **理由**：
  1. `@SQLRestriction` 是 Hibernate 原生支持，简洁有效
  2. 自动附加到所有 JPQL/Criteria 查询，无需手动编写 WHERE 条件
  3. Spring Data JPA 方法名查询（如 `findAll()`）自动生效
- **代码**：
  ```java
  @MappedSuperclass
  @SQLRestriction("deleted = false")
  public abstract class SoftDeletable extends Auditable {
      @Column(name = "deleted", nullable = false)
      private boolean deleted = false;
  }
  ```
- **行为约定**：
  - `repository.delete(entity)` 将 `deleted` 设为 `true`（非物理删除）
  - 所有查询自动过滤 `deleted = true` 的记录
  - 允许多次删除（幂等），`deleted` 保持 `true`
  - 允许更新已删除实体（`@SQLRestriction` 只影响查询，不影响 UPDATE）
- **替代方案**：
  - JPA `@Where` 注解：功能较弱，不如 `@SQLRestriction`
  - 自定义 Repository 覆盖所有方法：代码量大，易遗漏

## ADR-039：F02-06 不提供软删除还原、级联、物理删除能力

- **日期**：2026-03-14
- **状态**：设计决策（F02-06 Phase 2）
- **决策**：软删除只提供基础能力（删除标记 + 查询过滤），不提供 `restore()`、级联软删除、强制物理删除
- **理由**：
  1. **还原（`restore()`）**：业务语义因场景而异（谁可还原、是否校验状态），应由业务层实现
  2. **级联软删除**：需要业务规则约定（哪些关联跟着软删），框架无法通用实现
  3. **强制物理删除（`hardDelete()`）**：运维场景，业务项目可用 `@Modifying` + `@Query` 自行实现
  4. 控制代码量在 100-150 行范围内，符合复杂度 M 级别
- **范围约定**：
  | 功能 | 是否包含 | 原因 |
  |------|---------|------|
  | 删除时标记 `deleted = true` | ✅ | 核心能力 |
  | 查询时自动过滤 | ✅ | 核心能力 |
  | 还原（`restore()`） | ❌ | 业务语义复杂 |
  | 级联软删除 | ❌ | 需要业务规则 |
  | 强制物理删除 | ❌ | 运维场景，业务可自实现 |
- **替代方案**：
  - 全部实现：代码量大，超出 100-150 行范围，且通用性差

## ADR-040：F02-07 TsidGenerator 采用纯随机无锁实现

- **日期**：2026-03-14
- **状态**：已实施（F02-07）
- **决策**：v1 使用纯随机实现（42 位时间戳 + 22 位随机数），不使用计数器或同步机制
- **实现**：
  ```java
  public long generate() {
      long currentMillis = System.currentTimeMillis() - EPOCH_MILLIS;
      long timestampPart = currentMillis & TIMESTAMP_MASK;
      int random = this.random.nextInt(MAX_RANDOM + 1);
      return (timestampPart << RANDOM_BITS) | random;
  }
  ```
- **理由**：
  1. **满足性能约束**：无锁设计，单线程生成速度 > 100万/秒
  2. **符合设计文档**：与 02_interface.md 的伪代码一致
  3. **ThreadLocalRandom 保证线程安全**：无需 synchronized 或锁
  4. **极低冲突率可接受**：同毫秒内冲突概率约 0.000024%（1/4,194,304）
  5. **YAGNI 原则**：计数器、严格单调、时钟回拨处理留待后续版本
- **权衡**：
  | 方案 | 优点 | 缺点 | 决策 |
  |------|------|------|------|
  | 纯随机 | 无锁、高性能、符合设计 | 同毫秒内可能冲突（极低概率） | ✅ v1 采用 |
  | 计数器 + synchronized | 保证同毫秒唯一 | 违反性能约束、代码复杂 | ❌ |
- **替代方案**：
  - 计数器 + synchronized：违反"避免使用 synchronized 或 lock"约束，性能下降
- **后续规划**：
  - v2 可评估 AtomicLong 计数器方案（无锁但仍有状态管理开销）
  - v3 可考虑引入 node_id 实现雪花算法模式（需要配置）

## ADR-041：F02-09 使用 @AutoConfiguration 实现零配置引入

- **日期**：2026-03-14
- **状态**：已实施（F02-09）
- **决策**：使用 Spring Boot 3.4+ 的 `@AutoConfiguration` 注解，通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 声明自动配置类
- **理由**：
  1. `@AutoConfiguration` 是 Spring Boot 3.2+ 推荐的自动配置注解，替代传统的 `@Configuration` + `@AutoConfigureBefore/After`
  2. imports 文件路径标准化，Spring Boot 自动扫描和加载
  3. Bean 注册完全通过 `@Bean` 方法显式声明，不依赖 `@Component` 扫描
- **模块设计**：
  | 模块 | AutoConfiguration | 主要 Bean |
  |------|------------------|-----------|
  | cartisan-web | CartisanWebAutoConfiguration | RequestContextFilter、GlobalExceptionHandler |
  | cartisan-data-jpa | CartisanDataJpaAutoConfiguration | @Import JpaAuditingConfiguration |
  | cartisan-event | CartisanEventAutoConfiguration | DomainEventPublisher |
- **条件装配策略**：
  - **cartisan-web**：使用 `@ConditionalOnWebApplication`，仅在 Web 应用中生效
  - **cartisan-event**：使用 `@ConditionalOnMissingBean(DomainEventPublisher.class)`，允许用户覆盖
  - **cartisan-data-jpa**：JPA Auditing 使用 `@ConditionalOnBean(AuditorAware.class)`，有 Bean 时才启用
- **核心组件强制注册**：
  - `RequestContextFilter`、`GlobalExceptionHandler` 不使用 `@ConditionalOnMissingBean`
  - 用户需通过排除 AutoConfiguration 或显式注册自定义 Bean 覆盖
- **替代方案**：
  - 依赖 `@ComponentScan("com.cartisan.*")`：用户需手动配置，违反"引入即用"原则

## ADR-042：F02-09 RequestContextFilter 通过实现 Ordered 控制顺序

- **日期**：2026-03-14
- **状态**：已实施（F02-09）
- **决策**：`RequestContextFilter` 实现 `Ordered` 接口，`getOrder()` 返回 `Ordered.HIGHEST_PRECEDENCE`
- **理由**：
  1. `@Order` 注解在 `@Bean` 方法上对 Filter 在过滤器链中的顺序无效
  2. Filter 的执行顺序由 Filter 本身的 `getOrder()` 方法决定
  3. 移除 `@Component` 注解后，必须通过实现 `Ordered` 接口来控制顺序
- **代码对比**：
  ```java
  // ❌ 错误：@Order 在 @Bean 方法上对 Filter 顺序无效
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public RequestContextFilter requestContextFilter() {
      return new RequestContextFilter();
  }

  // ✅ 正确：Filter 实现 Ordered 接口
  public class RequestContextFilter extends OncePerRequestFilter implements Ordered {
      @Override
      public int getOrder() {
          return Ordered.HIGHEST_PRECEDENCE;
      }
  }
  ```
- **替代方案**：
  - 使用 `FilterRegistrationBean`：增加配置复杂度，YAGNI

## ADR-043：F02-09 GlobalExceptionHandler 保留 @ControllerAdvice

- **日期**：2026-03-14
- **状态**：已实施（F02-09）
- **决策**：`GlobalExceptionHandler` 保留 `@ControllerAdvice` 注解，通过 AutoConfiguration 的 `@Bean` 方法注册
- **理由**：
  1. `@ControllerAdvice` 是 Spring MVC 识别全局异常处理器的必要注解
  2. 移除后 `@ExceptionHandler` 方法不会生效
  3. "不依赖包扫描"指的是不通过 `@ComponentScan` 发现类，而非移除行为注解
- **代码示例**：
  ```java
  // ✅ 正确：保留 @ControllerAdvice，通过 @Bean 注册
  @ControllerAdvice  // 行为注解，必须保留
  public class GlobalExceptionHandler {
      @ExceptionHandler(CartisanException.class)
      public ResponseEntity<ApiResponse<Void>> handleCartisanException(CartisanException ex) {
          // ...
      }
  }

  @AutoConfiguration
  public class CartisanWebAutoConfiguration {
      @Bean
      public GlobalExceptionHandler globalExceptionHandler() {
          return new GlobalExceptionHandler();  // 显式注册，不依赖扫描
      }
  }
  ```
- **替代方案**：
  - 移除 `@ControllerAdvice`：异常处理器不生效，功能失效

## ADR-044：java-platform BOM 使用 api() 直接声明版本约束

- **日期**：2026-03-14
- **状态**：已实施（F03-01）
- **决策**：cartisan-dependencies 使用 `java-platform` 插件时，在 `dependencies` 块中直接用 `api("group:name:version")` 声明版本约束，不使用 `constraints {}`
- **理由**：
  1. `java-platform` 插件不支持 `constraints {}` 块，这是普通 Java 项目的用法
  2. 平台模块应使用顶层 `api()` / `runtime()` 声明带版本的约束
  3. `javaPlatform { allowDependencies() }` 配置允许平台依赖其他平台（如 Spring Boot BOM）
- **代码示例**：
  ```kotlin
  plugins {
      `java-platform`
  }
  
  javaPlatform {
      allowDependencies()
  }
  
  dependencies {
      // Spring Boot BOM - manages all Spring Boot starter versions
      api(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
      
      // Sa-Token（F03-01）- 直接用 api() 声明带版本约束
      api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")
  }
  ```
- **错误模式**：
  ```kotlin
  // ❌ 错误：java-platform 不支持 constraints {}
  dependencies {
      constraints {
          api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")
      }
  }
  ```
- **替代方案**：
  - 使用普通 `java` 插件而非 `java-platform`：失去平台版本管理的优势

## ADR-045：F03-02 不同类型注解使用 AND 逻辑

- **日期**：2026-03-14
- **状态**：已实施（F03-02）
- **决策**：`@RequireAuth`、`@RequireRole`、`@RequirePermission` 之间使用 AND 逻辑，全部检查通过才放行
- **理由**：
  1. 方法注解优先仅适用于**同一类型注解**（如类和方法都有 `@RequireAuth`）
  2. 不同类型注解之间是叠加关系，如 `@RequireAuth` + `@RequireRole("admin")` 表示"需登录 AND 需 admin 角色"
  3. 符合安全最佳实践：多重安全控制应该全部满足，而非满足其一即可
  4. 与 02_interface.md 的设计一致："鉴权顺序：@RequireAuth → @RequireRole → @RequirePermission（AND 逻辑）"
- **代码示例**：
  ```java
  @RequireAuth  // 类级别：需要登录
  public class AdminController {
      @RequireRole({"admin"})  // 方法级别：需要 admin 角色
      @PostMapping("/users")
      public void createUser() {
          // 需要同时满足：登录 AND admin 角色（AND 逻辑）
      }
  }
  ```
- **测试验证**：
  - `given_classRequireAuthAndMethodRequireRole_when_preHandle_then_callBoth()` 验证 AND 逻辑
  - 验证 `StpUtil.checkLogin()` 和 `StpUtil.checkRoleOr("admin")` 都被调用
- **替代方案**：
  - 方法有注解时忽略类注解：会导致 `@RequireAuth(false)` 无法覆盖类注解，且违背安全原则

## ADR-046：F03-02 异常处理器在 cartisan-security 内部实现

- **日期**：2026-03-14
- **状态**：已实施（F03-02）
- **决策**：`SecurityExceptionHandler` 位于 cartisan-security 模块内部，使用 `@ControllerAdvice`，依赖 cartisan-web 的 `ApiResponse`
- **理由**：
  1. 保持 cartisan-web 与安全实现解耦（cartisan-web 不依赖 Sa-Token）
  2. cartisan-security 已依赖 cartisan-web（需要 `ApiResponse`），反方向不依赖保证无循环
  3. 多个 `@ControllerAdvice` 是正常做法，Spring MVC 会收集所有异常处理器
  4. 职责清晰：安全相关异常由安全模块处理，通用异常由 cartisan-web 处理
- **包结构**：
  ```
  cartisan-security
  ├── com.cartisan.security.config.SecurityExceptionHandler (@ControllerAdvice)
  └── 依赖 cartisan-web.ApiResponse

  cartisan-web
  ├── com.cartisan.web.exception.GlobalExceptionHandler (@ControllerAdvice)
  └── 不依赖 Sa-Token 或 cartisan-security
  ```
- **替代方案**：
  - 在 cartisan-web 中处理 Sa-Token 异常：会导致 cartisan-web 依赖 Sa-Token，违背解耦原则
  - 在 F03-07 统一处理：延迟异常处理会导致鉴权失败暴露原始异常给前端

## ADR-047：F03-03 getCurrentUsername() 返回登录 ID 而非独立用户名字段

- **日期**：2026-03-14
- **状态**：设计决策（F03-03 Phase 2）
- **决策**：`SecurityContext.getCurrentUsername()` 返回 `StpUtil.getLoginIdAsString()`，即登录时传入的 ID
- **理由**：
  1. Sa-Token 的设计就是用 loginId 作为用户标识，不存储独立的 username 字段
  2. 避免额外从 Session 获取 username 的复杂度
  3. 业务项目可自行决定 loginId 是 username 还是 userId
- **行为说明**：
  - 若业务登录时传入 username 作为 loginId，则 `getCurrentUsername()` 返回 username
  - 若业务登录时传入 userId 作为 loginId，则 `getCurrentUsername()` 返回 userId 的字符串形式
  - 框架层不做额外假设，与 Sa-Token 保持一致
- **替代方案**：
  - 从 Session 获取独立 username 字段：增加复杂度，Sa-Token 无此约定

## ADR-048：F03-03 hasRole/hasPermission 参数校验使用 IllegalArgumentException

- **日期**：2026-03-14
- **状态**：设计决策（F03-03 Phase 2）
- **决策**：`hasRole()` / `hasPermission()` 参数为 null 或空白时抛 `IllegalArgumentException`
- **理由**：
  1. 快速失败（Fail Fast）原则
  2. 与 `Objects.requireNonNull()` 风格一致
  3. Sa-Token 内部也会对 null 参数做校验，我们提前校验提供更清晰的错误消息
- **代码示例**：
  ```java
  public static boolean hasRole(String role) {
      if (role == null || role.isBlank()) {
          throw new IllegalArgumentException("Role cannot be null or blank");
      }
      return StpUtil.hasRole(role);
  }
  ```
- **替代方案**：
  - 直接调用 Sa-Token，让 Sa-Token 抛异常：错误消息不够清晰，且依赖 Sa-Token 的具体实现

## ADR-032：TenantContext 使用 ScopedValue.isBound() + get() 模式

- **日期**：2026-03-14
- **Epic**：Epic 03 Security / Feature F03-04 TenantContext
- **决策**：使用 `ScopedValue.isBound()` 检查后 `get()`，而非假设的 `getOrDefault()`
- **背景**：原 02_interface.md 设计使用 `ScopedValue.getOrDefault(TENANT_ID, null)`，但 Java 21 的 ScopedValue API 不存在此方法
- **实现**：
  ```java
  public static Long getCurrentTenantId() {
      if (!TENANT_ID.isBound()) {
          return null;
      }
      return TENANT_ID.get();
  }
  ```
- **理由**：
  - `ScopedValue.get()` 在未绑定时抛 `NoSuchElementException`
  - `isBound()` 检查明确表达意图："先检查是否存在，再获取值"
  - 避免异常处理的开销
- **影响**：02_interface.md 中的设计描述需要更新，实际实现更优
