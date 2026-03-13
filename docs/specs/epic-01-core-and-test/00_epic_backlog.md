/# Epic 01 Backlog: 项目骨架 + Core + Test

> Epic 负责人：待定
> 版本：v0.1 | 日期：2026-03-13
> 依赖：无（foundational Epic）
> 复杂度：M

---

## Epic 目标

建立 cartisan-boot 项目基础设施，完成 DDD 核心建模类型和测试工具箱，为后续所有 Epic 提供可复用的基础能力。

**完成标准：**
1. Gradle 多模块项目可正常构建
2. cartisan-core 通过 ArchUnit 架构验证（零 Spring 依赖）
3. cartisan-test 可被业务项目继承，架构规则自动生效
4. 单元测试覆盖率 ≥ 80%

---

## Feature 拆解

### F01-01: Gradle 多模块项目骨架

| 属性 | 值 |
|------|-----|
| 复杂度 | S |
| 依赖 | 无 |
| 优先级 | P0 |
| 预估工时 | 0.5d |

**描述：**
搭建 Gradle Kotlin DSL 多模块项目结构，配置 Version Catalog，建立 BOM 模块。

**验收标准：**
- [ ] 根项目 `settings.gradle.kts` 定义所有子模块
- [ ] `gradle/libs.versions.toml` 统一管理依赖版本
- [ ] `cartisan-dependencies` 模块使用 `java-platform` 插件发布为 BOM
- [ ] 根项目 `./gradlew build` 可正常执行
- [ ] 根项目 `./gradlew :cartisan-core:compileJava` 可正常编译

**技术要点：**
- 使用 Gradle 8.x + Kotlin DSL
- Java 21 工具链配置
- Version Catalog 管理第三方库版本
- BOM 模块仅做版本管理，不含代码

---

### F01-02: cartisan-core — domain 基础类型

| 属性 | 值 |
|------|-----|
| 复杂度 | M |
| 依赖 | F01-01 |
| 优先级 | P0 |
| 预估工时 | 2d |

**描述：**
实现 DDD 战术设计的基础类型：聚合根、实体、值对象、领域事件、ID 封装、审计基类。

**验收标准：**
- [ ] `AggregateRoot` 标记接口 + `AbstractAggregateRoot` 抽象类
- [ ] `Entity<T, ID>` 接口含 `getId()` 和 `sameIdentityAs()`
- [ ] `ValueObject<T>` 接口含 `sameValueAs()`
- [ ] `Identity<T>` 接口含 `value()` 方法
- [ ] `DomainEvent` 基类含 eventId、occurredAt、aggregateId
- [ ] `Auditable` 和 `SoftDeletable` JPA 基类
- [ ] 单元测试覆盖所有公开 API

**技术要点：**
- **零外部依赖**：仅依赖 JDK 标准库
- `AbstractAggregateRoot` 使用 ArrayList 存储 `DomainEvent`
- `Auditable` 使用 `@MappedSuperclass`
- `SoftDeletable` 使用 Hibernate `@SQLRestriction`

**包含的 Story：**
1. 聚合根体系（AggregateRoot + AbstractAggregateRoot）
2. 实体接口（Entity）
3. 值对象接口（ValueObject）
4. ID 封装（Identity）
5. 领域事件基类（DomainEvent）
6. 审计基类（Auditable + SoftDeletable）

---

### F01-03: cartisan-core — exception 异常体系

| 属性 | 值 |
|------|-----|
| 复杂度 | S |
| 依赖 | F01-02 |
| 优先级 | P0 |
| 预估工时 | 0.5d |

**描述：**
实现统一的错误码接口、通用错误码枚举、业务异常基类。

**验收标准：**
- [ ] `CodeMessage` 接口含 `code()` 和 `message()`
- [ ] `BaseCodeMessage` 枚举含标准 HTTP 状态码
- [ ] `CartisanException` 携带 `CodeMessage`
- [ ] `DomainException` 和 `ApplicationException` 继承层次
- [ ] 单元测试覆盖所有异常类型

**技术要点：**
- `CodeMessage` 设计为接口，允许业务模块扩展
- `CartisanException` 为 `RuntimeException`，无需强制捕获
- 异常消息支持参数化格式化

---

### F01-04: cartisan-core — stereotype 架构注解

| 属性 | 值 |
|------|-----|
| 复杂度 | S |
| 依赖 | F01-02 |
| 优先级 | P0 |
| 预估工时 | 0.5d |

**描述：**
实现 DDD 架构注解，作为"可执行的架构文档"，支持 ArchUnit 自动验证。

**验收标准：**
- [ ] `@BoundedContext` 注解含 name 和 subDomain 属性
- [ ] `@Aggregate`、`@DomainService` 注解
- [ ] `@Port(PortType)` 注解支持 REPOSITORY/CLIENT/PUBLISHER
- [ ] `@Adapter(PortType)` 注解
- [ ] 所有注解使用 `@Retention(RUNTIME)`
- [ ] 单元测试验证注解属性

**技术要点：**
- 注解本身不产生运行时行为
- `RUNTIME` 保留策略允许 ArchUnit 反射读取
- `package-info.java` 上使用 `@BoundedContext`

---

### F01-05: cartisan-core — util 工具类

| 属性 | 值 |
|------|-----|
| 复杂度 | S |
| 依赖 | F01-03 |
| 优先级 | P0 |
| 预估工时 | 0.5d |

**描述：**
实现通用断言工具类，简化领域模型中的防御式编程。

**验收标准：**
- [ ] `Assertions.requirePresent(Optional<T>) → T`
- [ ] `Assertions.requirePresent(Optional<T>, CodeMessage) → T`
- [ ] `Assertions.require(boolean, CodeMessage)`
- [ ] `Assertions.ensure(boolean, String)`
- [ ] 单元测试覆盖所有断言方法及其异常路径

**技术要点：**
- 失败时抛出 `DomainException`（require）或 `IllegalArgumentException`（ensure）
- 清晰的方法命名区分前置条件（require）和后置条件（ensure）

---

### F01-06: cartisan-core 模块完整性验证

| 属性 | 值 |
|------|-----|
| 复杂度 | S |
| 依赖 | F01-02, F01-03, F01-04, F01-05 |
| 优先级 | P0 |
| 预估工时 | 0.5d |

**描述：**
创建 cartisan-core 模块的完整性测试，验证模块满足"零外部依赖"约束。

**验收标准：**
- [ ] ArchUnit 规则验证 cartisan-core 不依赖任何第三方库
- [ ] ArchUnit 规则验证 cartisan-core 不依赖 Spring
- [ ] 所有公开 API 的 JavaDoc 完整
- [ ] 模块可独立打包为 jar
- [ ] `./gradlew :cartisan-core:build` 成功

**技术要点：**
- 使用 ArchUnit 的 `dependency-rule` DSL
- 验证 `classes()` should `onlyDependOnClassesThat()...` 约束

---

### F01-07: cartisan-test — ArchUnit 规则集

| 属性 | 值 |
|------|-----|
| 复杂度 | M |
| 依赖 | F01-06 |
| 优先级 | P0 |
| 预估工时 | 2d |

**描述：**
实现可复用的 ArchUnit 架构规则集，业务项目继承后自动守护 DDD 分层和编码规范。

**验收标准：**
- [ ] `CartisanArchRules` 基类，业务项目继承即可使用
- [ ] DDD 分层规则：领域层不依赖基础设施/应用层
- [ ] DDD 分层规则：领域层不依赖 Spring
- [ ] 命名规范规则：Controller/Service/Repository 后缀约束
- [ ] 禁止规则：禁止字段注入、禁止 Date/Double 金额
- [ ] 所有规则有清晰的中文文档注释
- [ ] 单元测试验证规则有效性（故意写反例验证规则生效）

**技术要点：**
- 规则设计为"可扩展"：业务项目可追加自定义规则
- 使用 `@AnalyzeClasses` 指定扫描包
- 规则失败时提供清晰的错误消息

**包含的 Story：**
1. DDD 分层规则（4 条规则）
2. 命名规范规则（3 条规则）
3. 禁止规则（3 条规则）
4. `CartisanArchRules` 基类封装

---

### F01-08: cartisan-test — Testcontainers 基类

| 属性 | 值 |
|------|-----|
| 复杂度 | M |
| 依赖 | F01-07 |
| 优先级 | P0 |
| 预估工时 | 1.5d |

**描述：**
提供预配置的 Testcontainers 基类，简化集成测试中的中间件环境准备。

**验收标准：**
- [ ] `PostgresTestContainer` 预配置 PostgreSQL 16 容器
- [ ] `RedisTestContainer` 预配置 Redis 容器
- [ ] `IntegrationTestBase` 启动必要容器并加载 Spring 上下文
- [ ] 每个测试方法后自动清理数据
- [ ] 容器端口动态分配，避免冲突
- [ ] 单元测试验证容器正常启动和连接

**技术要点：**
- 使用 `@Testcontainers` 和 `@Container` 注解
- 容器生命周期与测试类同步
- `@DynamicPropertySource` 注入连接属性
- 支持 Virtual Threads（Testcontainers 1.20+ 兼容）

---

### F01-09: cartisan-test — API 测试基类

| 属性 | 值 |
|------|-----|
| 复杂度 | S |
| 依赖 | F01-08 |
| 优先级 | P1 |
| 预估工时 | 1d |

**描述：**
提供基于 MockMvc 的 API 测试基类，简化 Controller 层测试。

**验收标准：**
- [ ] `ApiTestBase` 集成 MockMvc
- [ ] 提供便捷方法：`get()`, `post()`, `put()`, `delete()`
- [ ] 提供断言方法：`assertOk()`, `assertError()`, `assertData()`
- [ ] 自动序列化/反序列化 JSON
- [ ] 单元测试验证便捷方法正常工作

**技术要点：**
- 使用 `@AutoConfigureMockMvc`
- JsonPath 或 ObjectMapper 断言响应
- 便捷方法设计为流式 API

---

### F01-10: cartisan-test — Fixture 工具

| 属性 | 值 |
|------|-----|
| 复杂度 | S |
| 依赖 | F01-08 |
| 优先级 | P1 |
| 预估工时 | 0.5d |

**描述：**
提供测试辅助工具类，简化测试数据构建和断言。

**验收标准：**
- [ ] `FixtureBuilder` 简化测试对象构建
- [ ] 提供 `randomString()`, `randomEmail()` 等随机数据生成器
- [ ] 单元测试覆盖所有工具方法

**技术要点：**
- 使用 Builder 模式
- 随机数据可设置种子保证可重复性

---

## 依赖关系图

```
F01-01 (项目骨架)
    │
    └──→ F01-02 (domain 基础类型)
            │
            ├──→ F01-03 (exception)
            ├──→ F01-04 (stereotype)
            ├──→ F01-05 (util)
            │
            └──→ F01-06 (core 完整性验证)
                    │
                    └──→ F01-07 (ArchUnit 规则集)
                            │
                            ├──→ F01-08 (Testcontainers)
                            │       │
                            │       ├──→ F01-09 (API 测试基类)
                            │       └──→ F01-10 (Fixture 工具)
```

---

## 推荐开发顺序

### Phase 1：基础设施（第 1 天）
1. **F01-01**: Gradle 多模块项目骨架
2. **F01-02**: cartisan-core — domain 基础类型

### Phase 2：Core 完善（第 2-3 天）
3. **F01-03**: cartisan-core — exception 异常体系
4. **F01-04**: cartisan-core — stereotype 架构注解
5. **F01-05**: cartisan-core — util 工具类
6. **F01-06**: cartisan-core 模块完整性验证

### Phase 3：Test 工具箱（第 4-6 天）
7. **F01-07**: cartisan-test — ArchUnit 规则集
8. **F01-08**: cartisan-test — Testcontainers 基类
9. **F01-09**: cartisan-test — API 测试基类
10. **F01-10**: cartisan-test — Fixture 工具

**总计预估：6 个工作日**

---

## 复杂度汇总

| 复杂度 | Feature 数 | 占比 |
|--------|-----------|------|
| S | 6 | 60% |
| M | 4 | 40% |
| L | 0 | 0% |

---

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Gradle Kotlin DSL 学习曲线 | 中 | 参考 Spring Boot 官方示例，使用 Version Catalog 简化 |
| ArchUnit 规则设计不当导致误报 | 中 | 规则设计初期充分测试，提供清晰的错误消息 |
| Testcontainers 与 Virtual Threads 兼容性 | 低 | 使用 Testcontainers 1.20+ 版本，验证兼容性 |
| JPA @SQLRestriction 在 Hibernate 6 中行为变化 | 低 | 验证目标 Hibernate 版本，补充单元测试 |

---

## 下一步

1. 本 Backlog 经评审后，进入 Phase 0（需求确认）
2. 为每个 Story 编写详细的 Acceptance Criteria
3. 准备开发环境检查清单
