 # cartisan-boot 基础框架设计文档

> 版本：v0.3 | 日期：2026-03-11
> 定位：可复用的 Java 基础研发框架，为公司所有 Spring Boot 项目提供 DDD 基建、Web 规范、安全抽象、数据层封装、通用技术能力封装和测试守护。
> 性质：Epic 阶段前的输入——项目愿景与技术蓝图。

---

## 目录

- [一、项目定位与设计原则](#一项目定位与设计原则)
- [二、技术栈](#二技术栈)
- [三、模块架构](#三模块架构)
- [四、各模块详细设计](#四各模块详细设计)
- [五、模块间依赖关系](#五模块间依赖关系)
- [六、业务项目接入方式](#六业务项目接入方式)
- [七、Epic 拆分建议](#七epic-拆分建议)

---

## 一、项目定位与设计原则

### 1.1 定位

cartisan-boot 是**业务无关的技术基础框架**。它为所有 Java 项目提供统一的技术能力封装，让业务项目专注于业务逻辑本身。

### 1.2 收纳准则

**一个技术能力是否应该放入 cartisan-boot，由以下三条准则判定：**

| # | 准则 | 说明 |
|---|------|------|
| 1 | **不需要业务数据库表** | 框架不建表、不管理业务数据。需要持久化配置/规则的能力属于业务层 |
| 2 | **不同项目的调用方式基本一致** | 接口定义和使用模式是稳定的、通用的。如果每个项目用法差异大，说明它含有业务语义 |
| 3 | **API 足够稳定** | 底层依赖的 SDK/框架不会频繁 breaking change。不满足时预留位置但暂不实现 |

**三条全满足 → 放入 cartisan-boot。**
**第 3 条暂不满足 → 预留模块位置，等稳定后纳入。**
**第 1 或第 2 条不满足 → 属于业务层，不放入。**

判定示例：

| 能力 | 准则 1 | 准则 2 | 准则 3 | 结论 |
|------|--------|--------|--------|------|
| 统一响应体、全局异常处理 | ✅ 无表 | ✅ 一致 | ✅ 稳定 | 放入 |
| 大模型调用（Provider 适配器） | ✅ 无表 | ✅ 一致 | ✅ 稳定 | 放入 |
| 文件存储（OSS/MinIO 封装） | ✅ 无表 | ✅ 一致 | ✅ 稳定 | 放入 |
| 支付对接（微信/支付宝封装） | ✅ 无表 | ✅ 一致 | ✅ 稳定 | 放入 |
| Agent 框架封装 | ✅ 无表 | ✅ 一致 | ❌ 快速迭代中 | 预留，暂不实现 |
| 路由规则管理（按成本/质量路由） | ❌ 需要规则表 | ❌ 不同项目策略不同 | — | 业务项目 |
| 租户管理 CRUD | ❌ 需要租户表 | ❌ 不同项目模型不同 | — | 业务项目 |
| 计费（虚拟币/汇率） | ❌ 需要流水表 | ❌ 不同项目模型不同 | — | 业务项目 |

### 1.3 设计原则

| 原则 | 说明 |
|------|------|
| **业务无关** | 只封装技术能力，不含业务逻辑。框架不知道"用户"、"订单"、"套餐"是什么 |
| **零侵入** | 业务项目按需引入模块，不用的不引入。框架不绑架业务 |
| **强约束** | 通过泛型、接口、ArchUnit 规则，在编译期/测试期约束代码质量 |
| **AI 友好** | 强类型、命名规范、无魔法——让 AI 生成的代码有工具链兜底 |
| **可替换** | 底层实现可切换（Sa-Token → Spring Security，MinIO → 阿里云 OSS），业务代码通过抽象层隔离 |

### 1.4 框架提供什么，业务项目做什么

| | cartisan-boot 提供 | 业务项目实现 |
|---|---|---|
| DDD | 聚合根、实体、值对象、领域事件等基础积木 | 具体的聚合根、实体、领域服务 |
| Web | 统一响应体、全局异常处理、请求上下文 | Controller、DTO |
| 安全 | 认证/授权抽象、多租户上下文基础设施 | 用户/租户/权限的业务逻辑 |
| 数据 | Repository 基类、审计字段、软删除 | 具体的 DomainEntity 和 Repository |
| 事件 | 领域事件发布/订阅基础设施 | 具体的业务事件定义 |
| AI | 大模型调用 SPI + 各厂商适配器 + SSE 流式工具 | 路由策略、Token 计费、Agent 编排 |
| 存储 | 文件上传/下载 SPI + 各厂商适配器 | "附件"的业务概念（关联实体、权限） |
| 支付 | 支付下单/回调 SPI + 微信/支付宝适配器 | 充值套餐、订单管理等业务流程 |
| 测试 | ArchUnit 规则集、Testcontainers 基类 | 具体的业务测试 |

---

## 二、技术栈

| 层面 | 选型 | 版本 | 理由 |
|------|------|------|------|
| 语言 | Java | 21 | Virtual Threads（高并发长连接）、Record（不可变 DTO）、Sealed Classes、Pattern Matching |
| 框架 | Spring Boot | 3.4.x | Virtual Threads 原生支持、Spring Modulith |
| 构建 | Gradle Kotlin DSL | 最新 | 多模块灵活管理、Version Catalog 统一版本 |
| 编程模型 | Virtual Threads + 阻塞式 | — | 代码直观，AI 生成质量高，Virtual Threads 解决吞吐量 |
| 持久化（写） | Spring Data JPA (Hibernate) | — | 聚合根持久化，DDD 标准实践 |
| 持久化（读） | jOOQ | — | 类型安全 SQL，编译期校验，CQRS 读侧 |
| 数据库 | PostgreSQL | 16+ | JSONB、强并发控制、丰富扩展 |
| 缓存 | Redis (Lettuce) | — | 会话、限流、热数据缓存 |
| 认证 | Sa-Token | — | 轻量实用，通过框架抽象层隔离，可替换 |
| DB 迁移 | Flyway | — | 数据库版本管理 |
| API 文档 | SpringDoc OpenAPI 3.1 | — | 标准化，可自动生成前端 TypeScript 类型 |
| 单元测试 | JUnit 5 + AssertJ + Mockito | — | 业界标准 |
| 架构测试 | ArchUnit | — | 自动化架构规则守护 |
| 集成测试 | Testcontainers | — | 真实中间件环境 |
| 变异测试 | PIT (Pitest) | — | 验证测试质量 |
| 监控 | Micrometer + Prometheus | — | 可观测性基础 |

---

## 三、模块架构

### 3.1 模块总览

```
cartisan-boot/
├── gradle/
│   └── libs.versions.toml                 # Version Catalog
├── build.gradle.kts
├── settings.gradle.kts
│
├── cartisan-dependencies/                  # BOM
│
│  ── 核心模块（短期实现）──
├── cartisan-core/                          # DDD 基建
├── cartisan-web/                           # Web 层规范
├── cartisan-security/                      # 安全与多租户基础设施
├── cartisan-data-jpa/                      # JPA 写侧封装
├── cartisan-data-query/                    # jOOQ 读侧封装
├── cartisan-event/                         # 领域事件基础设施
├── cartisan-test/                          # 测试工具箱
├── cartisan-ai/                            # 大模型调用封装
│
│  ── 扩展模块（按需实现）──
├── cartisan-storage/                       # 文件存储封装
├── cartisan-payment/                       # 支付对接封装
│
│  ── 预留模块（暂不实现）──
└── cartisan-ai-agent/                      # Agent 框架封装（待生态稳定）
```

### 3.2 各模块一句话定位

| 模块 | 状态 | 定位 |
|------|------|------|
| **cartisan-dependencies** | — | BOM（Bill of Materials）：业务项目引入即可管理所有 cartisan 模块版本 |
| **cartisan-core** | 短期实现 | DDD 战术设计基础积木：实体、值对象、聚合根、领域事件、架构注解、错误码体系。**零外部依赖，纯 Java** |
| **cartisan-web** | 短期实现 | Spring MVC 统一封装：强类型响应体、全局异常处理、请求上下文、参数校验格式化 |
| **cartisan-security** | 短期实现 | 认证授权薄抽象层 + 多租户上下文基础设施。底层 Sa-Token 可替换 |
| **cartisan-data-jpa** | 短期实现 | JPA 持久化封装：Repository 基类（含事件自动发布）、审计字段、软删除、分布式 ID |
| **cartisan-data-query** | 短期实现 | jOOQ 读侧封装：自动配置、分页工具、代码生成配置 |
| **cartisan-event** | 短期实现 | 领域事件发布/订阅基础设施：Spring Events 桥接，预留消息队列扩展 |
| **cartisan-test** | 短期实现 | 测试工具箱：ArchUnit 预置规则集、Testcontainers 基类、测试辅助工具 |
| **cartisan-ai** | 短期实现 | 大模型调用 SPI + 各厂商 Provider 实现 + SSE 流式输出工具 |
| **cartisan-storage** | 按需实现 | 文件存储 SPI + 各厂商适配器（阿里云 OSS、MinIO 等） |
| **cartisan-payment** | 按需实现 | 支付 SPI + 各渠道适配器（微信支付、支付宝等） |
| **cartisan-ai-agent** | 预留 | Agent 编排框架封装。等 LangChain4j / Spring AI Agent 稳定后纳入 |

---

## 四、各模块详细设计

### 4.1 cartisan-dependencies（BOM）

**职责：** 统一管理所有 cartisan 模块的版本号，业务项目只需引入 BOM，无需逐个指定版本。

无代码，只有 Gradle 配置。通过 `javaPlatform` 插件发布为 BOM。

---

### 4.2 cartisan-core（DDD 基建）

**核心约束：零外部依赖，只依赖 JDK 标准库。** 这保证了领域模型的纯粹性——任何放在 domain 包中的代码不会被框架污染。

#### 包结构

```
com.cartisan.core/
├── domain/               # 领域建模基础类型
├── exception/            # 异常与错误码体系
├── stereotype/           # DDD 架构注解
└── util/                 # 通用工具
```

#### domain — 领域建模基础类型

**AggregateRoot 体系：**

```
AggregateRoot                          — 标记接口：标识哪些实体是聚合根
  └── AbstractAggregateRoot            — 抽象类：携带领域事件注册能力

  核心行为：
  - registerEvent(DomainEvent)         — 注册一个待发布的领域事件
  - getDomainEvents() → List           — 获取已注册的事件列表
  - clearDomainEvents()                — 清空事件（发布后调用）
```

只有实现 `AggregateRoot` 的实体才能拥有 Repository——这通过 `BaseRepository<T extends AggregateRoot>` 的泛型约束在编译期强制。

**DomainEntity 接口：**

```
DomainEntity<T, ID>
  - getId() → ID                       — 获取标识
  - sameIdentityAs(T other) → boolean  — 身份比较
```

**ValueObject 接口：**

```
ValueObject<T>
  - sameValueAs(T other) → boolean     — 值相等比较
```

推荐使用 Java Record 实现值对象，天然不可变且自动生成 equals/hashCode。

**Identity 接口：**

```
Identity<T>
  - value() → T                        — 获取原始值
```

用于类型安全的 ID 封装。避免 `Long userId` 和 `Long orderId` 被混用——`UserId` 和 `OrderId` 是不同类型。

**DomainEvent 基类：**

```
DomainEvent
  - eventId: String                    — 唯一标识（UUID）
  - occurredAt: Instant                — 发生时间
  - aggregateId: String                — 触发事件的聚合根 ID
```

所有业务事件继承此基类。

**Auditable 基类：**

```
Auditable（JPA @MappedSuperclass）
  - createdAt: LocalDateTime            — 创建时间（自动填充）
  - updatedAt: LocalDateTime            — 更新时间（自动填充）
  - createdBy: String                   — 创建人（可选，配合安全模块）
  - updatedBy: String                   — 更新人（可选）
```

**AuditableSoftDeletable 基类：**

```
AuditableSoftDeletable extends Auditable implements SoftDeletable
  - deleted: boolean = false            — 软删除标记
  自动过滤：查询时自动排除 deleted=true 的记录（@SQLRestriction）

**SoftDeletable 接口：**
```
SoftDeletable（接口）
  - markAsDeleted()                     — 标记为已删除（领域方法）
  - getDeleted() → boolean              — 获取软删除标记值
```

#### exception — 异常与错误码体系

**错误码设计：**

```
CodeMessage（接口）
  - code() → int
  - message() → String

BaseCodeMessage（枚举，实现 CodeMessage）
  - SUCCESS(200, "success")
  - BAD_REQUEST(400, "请求参数错误")
  - UNAUTHORIZED(401, "未认证")
  - FORBIDDEN(403, "无权限")
  - NOT_FOUND(404, "资源不存在")
  - CONFLICT(409, "资源冲突")
  - INTERNAL_ERROR(500, "系统内部错误")
```

`CodeMessage` 是接口而非枚举，这样业务模块可以定义自己的错误码枚举并实现该接口。

**异常体系：**

```
CartisanException（统一业务异常，携带 CodeMessage）
  ├── DomainException（领域层异常）
  └── ApplicationException（应用层异常）
```

#### stereotype — DDD 架构注解

所有注解使用 `@Retention(RUNTIME)`，支持 ArchUnit 在测试时检查。

```
@BoundedContext(name, subDomain)        — 标注在 package-info.java 上
  subDomain: Core / Supporting / Generic

@Aggregate                              — 标注在聚合根类上
@DomainService                          — 标注在领域服务类上
@Port(PortType)                         — 标注在端口接口上
  PortType: Repository / Client / Publisher
@Adapter(PortType)                      — 标注在适配器实现上
```

这些注解本身不产生运行时行为，它们是"可执行的架构文档"——人读了知道这个类的角色，ArchUnit 读了可以自动检查分层约束。

#### util — 通用工具

```
Assertions
  - requirePresent(Optional<T>) → T           — Optional 断言，不存在则抛 NOT_FOUND
  - requirePresent(Optional<T>, CodeMessage)   — 自定义错误码
  - require(boolean, CodeMessage)              — 条件断言
  - ensure(boolean, String)                    — 后置条件断言
```

---

### 4.3 cartisan-web（Web 层规范）

#### 包结构

```
com.cartisan.web/
├── response/             # 统一响应体
├── exception/            # 全局异常处理
├── context/              # 请求上下文
└── config/               # 自动配置
```

#### 核心组件

**ApiResponse\<T\> — 统一响应体（替代 Map 式响应）：**

```
ApiResponse<T>（Record）
  - code: int
  - message: String
  - data: T                             — 强类型，泛型安全
  - requestId: String                   — 全链路追踪 ID

  静态工厂：
  - ok(T data) → ApiResponse<T>
  - ok() → ApiResponse<Void>
  - error(CodeMessage) → ApiResponse<Void>

  优势：
  - AI 生成 Controller 时，返回类型有编译器校验
  - 前端可从 OpenAPI 自动生成 TypeScript 类型
  - 不再需要 (Type) 强制转换
```

**PageResponse\<T\> — 分页响应体：**

```
PageResponse<T>
  - items: List<T>
  - total: long
  - page: int
  - size: int
```

**GlobalExceptionHandler — 全局异常 → ApiResponse 映射：**

```
@ControllerAdvice
处理映射：
  CartisanException     → ApiResponse.error(exception.codeMessage)
  ConstraintViolation   → ApiResponse.error(400, 格式化的校验错误)
  MethodArgumentNotValid → ApiResponse.error(400, 格式化的校验错误)
  AccessDeniedException → ApiResponse.error(403, "无权限")
  Exception             → ApiResponse.error(500, "系统内部错误") + 日志记录
```

业务项目不需要自己处理异常到响应的转换——抛 `CartisanException` 即可，框架自动转为标准格式。

**RequestContext — 请求上下文：**

```
RequestContext（基于 ThreadLocal / ScopedValue）
  - getRequestId() → String             — 请求唯一标识
  - getClientIp() → String              — 客户端 IP
  
RequestContextFilter
  - 在请求进入时初始化 RequestContext
  - 生成 requestId（或从 Header 读取，支持链路追踪）
  - 请求结束时清理
```

**自动配置：**

通过 Spring Boot AutoConfiguration 机制，业务项目引入 `cartisan-web` 依赖后，上述组件自动生效，零配置。

---

### 4.4 cartisan-security（安全与多租户基础设施）

#### 包结构

```
com.cartisan.security/
├── annotation/           # 权限注解
├── context/              # 安全/租户上下文
├── authentication/       # 认证抽象与实现
└── config/               # 自动配置
```

#### 核心设计：薄抽象层

**原则：** 业务代码使用框架提供的抽象（注解、Context），不直接调用 Sa-Token API。底层实现封装在 cartisan-security 内部，将来可整体替换为 Spring Security。

**权限注解：**

```
@RequireAuth                            — 需要登录
@RequireRole("admin")                   — 需要角色
@RequirePermission("user:create")       — 需要权限
```

**SecurityContext — 当前用户上下文：**

```
SecurityContext
  - getCurrentUserId() → Long
  - getCurrentUsername() → String
  - hasRole(String) → boolean
  - hasPermission(String) → boolean
  - isAuthenticated() → boolean
```

**TenantContext — 多租户上下文基础设施：**

```
TenantContext
  - getCurrentTenantId() → Long
  - setCurrentTenantId(Long)
  - clear()

TenantContextFilter
  - 从请求 Header（X-Tenant-Id）或 Token 中解析 tenantId
  - 放入 TenantContext
  - 请求结束时清理

  注意：兼容 Virtual Threads（ScopedValue 或 ThreadLocal + 复制策略）
```

**TenantContext 只是基础设施**——它负责"在当前请求上下文中保持 tenantId"。至于"租户怎么创建、怎么审批、数据怎么隔离"——这些是业务项目的事。但业务项目的数据隔离拦截器可以读取 `TenantContext.getCurrentTenantId()` 来实现。

**认证服务抽象：**

```
AuthenticationService（接口）
  - login(username, password) → TokenInfo
  - logout()
  - getTokenInfo() → TokenInfo

SaTokenAuthenticationService（实现）
  - 内部调用 Sa-Token API
```

---

### 4.5 cartisan-data-jpa（JPA 写侧封装）

#### 包结构

```
com.cartisan.data.jpa/
├── repository/           # Repository 基类
├── id/                   # 分布式 ID 生成
├── audit/                # 审计配置
└── config/               # 自动配置
```

#### 核心组件

**BaseRepository — Repository 基类：**

```
BaseRepository<T extends AggregateRoot, ID extends Serializable>
  extends JpaRepository<T, ID>, JpaSpecificationExecutor<T>

  泛型约束 T extends AggregateRoot 确保：
  - 只有聚合根才能有 Repository
  - 非聚合根实体必须通过聚合根访问（DDD 规则的编译期强制）
```

**BaseRepositoryImpl — 增强实现：**

```
在 save() 方法中：
  1. 调用 JPA save
  2. 如果实体是 AbstractAggregateRoot：
     a. 获取 domainEvents 列表
     b. 通过 DomainEventPublisher 逐一发布
     c. 清空 domainEvents
  
  效果：业务代码只需要在领域模型中 registerEvent()，
  save 时事件自动发布，业务代码不需要手动发布事件。
```

**TSID — 分布式 ID 生成：**

```
TsidGenerator
  - 基于 TSID（Time-Sorted ID）算法
  - 时间有序 + 全局唯一
  - 比 Snowflake 更现代：无需配置 workerId/dataCenterId
  - 生成的 Long 值可排序、可提取时间戳
```

**审计配置：**

```
自动配置 Spring Data JPA Auditing：
  - @CreatedDate / @LastModifiedDate 自动填充时间
  - @CreatedBy / @LastModifiedBy 自动填充操作人
    （从 SecurityContext 获取当前用户）
```

---

### 4.6 cartisan-data-query（jOOQ 读侧封装）

#### 包结构

```
com.cartisan.data.query/
├── tenant/               # 多租户工具
└── config/               # jOOQ 自动配置
```

#### 核心组件

**jOOQ 自动配置：**

```
- 配置 DSLContext Bean（使用项目的 DataSource）
- 配置 PostgreSQL 方言
- 配置 SQL 执行日志（可选）
- 通过 Spring Boot AutoConfiguration 实现，引入依赖后自动生效
```

**多租户工具（可选依赖 cartisan-security）：**

```
JooqTenantSupport
  - eqTenantId(TableField<?, Long> tenantIdField) → Condition

设计原则：
  - 按列入参（TableField），任意表都可用
  - 显式调用，代码意图清晰，调试友好
  - 无租户上下文时返回 noCondition()，不添加过滤

业务项目使用示例：
  ctx.selectFrom(USER)
     .where(eqTenantId(USER.TENANT_ID))
     .fetch();
```

**代码生成（在业务项目中完成）：**

```
本模块不提供独立代码生成 CLI，而是提供：
  - 标准 build.gradle.kts 配置片段
  - PostgreSQL 方言与生成策略示例
  - 文档：generateJooq 依赖 flywayMigrate、生成目录约定等

代码生成必须在业务项目执行，因为真实 schema 来自业务项目的 Flyway 迁移。
```

**集成测试：**

```
必测项：
  - jOOQ DSL 正确查询数据库
  - 分页计算正确（offset/limit 转换、PageResponse 封装）
  - DataSource 集成正常

可选增强（CQRS 共存验证）：
  - JPA 写 → jOOQ 读 数据一致性
  - 不涉及事务边界、领域事件等复杂场景
```

**CQRS 使用模式：**

```
写侧：JPA Repository → 操作聚合根实体
读侧：jOOQ DSL → 直接查询返回 DTO（不经过领域模型）

好处：
  - 写侧保持 DDD 模型的纯粹性
  - 读侧不受领域模型约束，可以灵活组合多表、做聚合查询
  - jOOQ 的类型安全在编译期捕获 SQL 错误，AI 生成的查询更可靠
```

---

### 4.7 cartisan-event（领域事件基础设施）

#### 包结构

```
com.cartisan.event/
├── DomainEventPublisher.java           # 发布器接口
├── SpringDomainEventPublisher.java     # Spring Events 实现
└── config/
```

#### 设计

```
DomainEventPublisher（接口）
  - publish(DomainEvent event)

SpringDomainEventPublisher（实现）
  - 将 DomainEvent 包装为 Spring ApplicationEvent 发布
  - 同一事务内同步处理（@TransactionalEventListener 可选异步）

扩展路径：
  后续可增加 MQ 实现（如 KafkaDomainEventPublisher），
  将事件发布到消息队列供其他服务消费。
  业务代码不变，只切换 Publisher 实现。
```

**使用方式（在业务项目中）：**

```
领域模型中：
  this.registerEvent(new OrderCreatedEvent(this.id));

Repository save 时自动发布（cartisan-data-jpa 处理）。

消费方：
  @EventListener
  void handle(OrderCreatedEvent event) { ... }
```

---

### 4.8 cartisan-test（测试工具箱）

#### 包结构

```
com.cartisan.test/
├── archunit/             # ArchUnit 预置规则集
├── container/            # Testcontainers 预配置
├── base/                 # 测试基类
└── fixture/              # 测试辅助工具
```

#### ArchUnit 预置规则集

cartisan-test 提供一组开箱即用的架构规则，业务项目继承后自动生效：

**DDD 分层规则：**
- 领域层不依赖基础设施层和应用层
- 领域层不依赖 Spring 框架
- Controller 只依赖应用服务层
- 应用服务不直接操作数据库（通过 Repository）

**命名规范规则：**
- `@RestController` 类必须以 `Controller` 结尾
- `@Service` 在 application 包下的类以 `AppService` 结尾
- Repository 接口以 `Repository` 结尾

**禁止规则：**
- 禁止 `@Autowired` 字段注入（必须构造函数注入）
- 禁止 `java.util.Date`（使用 `java.time`）
- 禁止 `Double`/`Float` 用于金额字段

**业务项目使用方式：**

```
继承 CartisanArchRules 并指定扫描包：

@AnalyzeClasses(packages = "com.aieducenter")
class ArchitectureTest extends CartisanArchRules {
    // 自动继承所有预置规则
    // 可追加业务特有规则
}
```

#### Testcontainers 预配置

```
PostgresTestContainer
  - 预配置 PostgreSQL 16 容器
  - 自动注入 DataSource 属性

RedisTestContainer
  - 预配置 Redis 容器
  - 自动注入 Redis 连接属性
```

#### 测试基类

```
IntegrationTestBase
  - 启动 PostgreSQL + Redis 容器
  - 加载 Spring 上下文
  - 每个测试方法后自动清理数据

ApiTestBase
  - 集成 MockMvc
  - 提供发送请求和断言响应的便捷方法
```

---

### 4.9 cartisan-ai（大模型调用封装）

#### 包结构

```
com.cartisan.ai/
├── model/                 # 统一模型（ChatMessage, ChatCompletion 等）
├── provider/              # Provider SPI + 各厂商实现
├── sse/                   # SSE 流式输出工具
└── config/                # 自动配置
```

#### 核心设计

**统一对话模型（与具体厂商无关的中间表示）：**

```
ChatMessage（Record）
  - role: Role（SYSTEM / USER / ASSISTANT）
  - content: String

ChatRequest
  - model: String                          — 模型标识（gpt-4o, claude-sonnet-4 等）
  - messages: List<ChatMessage>
  - temperature: Double（可选）
  - maxTokens: Integer（可选）
  - stream: boolean

ChatResponse（Record）
  - content: String
  - model: String                          — 实际使用的模型
  - usage: TokenUsage                      — Token 消耗统计

TokenUsage（Record）
  - promptTokens: int
  - completionTokens: int
  - totalTokens: int

ChatStreamEvent（Record）
  - delta: String                          — 增量内容
  - finished: boolean
  - usage: TokenUsage（仅最后一个 event 携带）
```

**Provider SPI — 各厂商统一抽象：**

```
ModelProvider（接口）
  - id() → String                          — 提供商标识（openai / anthropic / deepseek 等）
  - supportedModels() → List<String>       — 该 Provider 支持的模型列表
  - chat(ChatRequest) → ChatResponse       — 同步调用
  - chatStream(ChatRequest) → Flux<ChatStreamEvent>  — 流式调用

内置实现（短期完成核心，其余按需扩展）：
  OpenAiProvider                           — OpenAI / Azure OpenAI
  AnthropicProvider                        — Claude 系列
  DeepSeekProvider                         — DeepSeek

扩展方式：
  业务项目或后续 cartisan 版本实现新的 ModelProvider，
  通过 Spring SPI（@Component）注册即可自动发现。
```

**ModelProviderRegistry — Provider 管理：**

```
ModelProviderRegistry
  - getProvider(providerId) → ModelProvider
  - getProviderByModel(modelName) → ModelProvider
  - listProviders() → List<ModelProvider>

  自动扫描所有 ModelProvider Bean，建立 model→provider 映射。
  业务项目可以通过 model name 直接调用，无需关心具体 Provider。
```

注意边界：**ModelProviderRegistry 只做"按名查找"**。按成本/质量/负载做智能路由，需要业务规则表，属于业务项目（如 aieducenter-platform 的 AI Gateway Context）。

**SSE 流式工具：**

```
SseHelper
  - toSse(Flux<ChatStreamEvent>) → SseEmitter   — 将流式事件转为 Spring SSE
  - toSse(Flux<ChatStreamEvent>, Consumer<TokenUsage>)  — 流结束时回调 usage

  封装 SSE 连接管理：超时、异常、客户端断开等边界情况。
  业务项目的 Controller 只需一行即可返回流式响应。
```

**自动配置：**

```
cartisan:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1     # 可指向代理
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}

各 Provider 条件装配：配了 api-key 才创建对应 Provider Bean。
```

---

### 4.10 cartisan-storage（文件存储封装）

#### 包结构

```
com.cartisan.storage/
├── StorageService.java                    # 存储 SPI
├── StorageObject.java                     # 存储对象模型
├── provider/                              # 各厂商适配器
│   ├── AliyunOssStorageProvider.java
│   ├── MinioStorageProvider.java
│   └── LocalStorageProvider.java          # 开发环境用本地存储
└── config/                                # 自动配置
```

#### 核心设计

```
StorageObject（Record）
  - key: String                            — 存储路径/key
  - url: String                            — 访问地址
  - size: long                             — 文件大小
  - contentType: String                    — MIME 类型

StorageService（接口）
  - upload(key, InputStream, contentType) → StorageObject
  - download(key) → InputStream
  - delete(key) → void
  - getUrl(key) → String                   — 生成访问 URL（支持预签名）
  - exists(key) → boolean

内置实现：
  AliyunOssStorageProvider                 — 阿里云 OSS
  MinioStorageProvider                     — MinIO / S3 兼容
  LocalStorageProvider                     — 本地文件系统（开发/测试用）
```

**自动配置：**

```
cartisan:
  storage:
    provider: aliyun-oss                   # aliyun-oss / minio / local
    aliyun-oss:
      endpoint: oss-cn-hangzhou.aliyuncs.com
      access-key-id: ${OSS_ACCESS_KEY}
      access-key-secret: ${OSS_ACCESS_SECRET}
      bucket-name: my-bucket
    minio:
      endpoint: http://localhost:9000
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      bucket-name: my-bucket
    local:
      root-path: /data/storage

条件装配：根据 provider 配置创建对应实现 Bean。
```

---

### 4.11 cartisan-payment（支付对接封装）

#### 包结构

```
com.cartisan.payment/
├── PaymentService.java                    # 支付 SPI
├── model/                                 # 统一模型
│   ├── PaymentRequest.java
│   ├── PaymentResult.java
│   └── RefundRequest.java
├── callback/                              # 回调处理
│   └── PaymentCallbackHandler.java
├── provider/                              # 各渠道适配器
│   ├── WechatPayProvider.java
│   └── AlipayProvider.java
└── config/                                # 自动配置
```

#### 核心设计

```
PaymentRequest（Record）
  - outTradeNo: String                     — 业务方订单号
  - amount: BigDecimal                     — 金额（元）
  - subject: String                        — 商品描述
  - channel: PayChannel                    — 支付渠道（WECHAT / ALIPAY）
  - notifyUrl: String                      — 回调地址

PaymentResult（Record）
  - outTradeNo: String
  - transactionId: String                  — 支付平台交易号
  - channel: PayChannel
  - status: PayStatus                      — PENDING / SUCCESS / FAILED
  - paidAt: Instant

RefundRequest（Record）
  - outTradeNo: String
  - refundNo: String
  - amount: BigDecimal
  - reason: String

PaymentService（接口）
  - createPayment(PaymentRequest) → PaymentResult
  - queryPayment(outTradeNo) → PaymentResult
  - refund(RefundRequest) → RefundResult
  - verifyCallback(headers, body) → PaymentResult  — 验签 + 解析回调

内置实现：
  WechatPayProvider                        — 微信支付（V3 API）
  AlipayProvider                           — 支付宝
```

注意边界：PaymentService 只负责**技术层面的下单/回调/验签**。至于"充值套餐"、"虚拟币兑换"、"流水记录"——这些需要数据库表，属于业务项目。

**自动配置：**

```
cartisan:
  payment:
    wechat:
      app-id: ${WECHAT_APP_ID}
      mch-id: ${WECHAT_MCH_ID}
      private-key-path: classpath:wechat/apiclient_key.pem
      certificate-serial-no: ${WECHAT_CERT_SERIAL}
      api-v3-key: ${WECHAT_API_V3_KEY}
    alipay:
      app-id: ${ALIPAY_APP_ID}
      private-key: ${ALIPAY_PRIVATE_KEY}
      alipay-public-key: ${ALIPAY_PUBLIC_KEY}

条件装配：配了对应渠道参数才创建 Provider Bean。
```

---

### 4.12 cartisan-ai-agent（预留，暂不实现）

**预留理由：** Agent 框架（LangChain4j、Spring AI Agent、AgentScope）当前迭代速度极快，API 频繁 breaking change，不满足收纳准则第 3 条。

**预计纳入时机：** 2026 年底至 2027 年初，待主流框架发布稳定大版本。

**预期能力方向：**
- Agent 定义与编排 SPI
- Tool/Function Calling 注册与分发
- Memory / Context 管理抽象
- Multi-Agent 协作基础设施

**当前策略：** 在 aieducenter-platform 中直接使用 LangChain4j / Spring AI，积累实践经验。待 API 稳定后，提取通用部分到 cartisan-ai-agent。

---

## 五、模块间依赖关系

```
cartisan-core（零外部依赖，纯 Java）
    │
    ├──→ cartisan-web        （core + Spring MVC）
    ├──→ cartisan-data-jpa   （core + Spring Data JPA）
    ├──→ cartisan-event      （core + Spring Context）
    ├──→ cartisan-ai         （core + WebFlux/Reactor for SSE）
    └──→ cartisan-test       （core + JUnit 5 + ArchUnit + Testcontainers）

cartisan-security            （core + web + Sa-Token）

cartisan-data-query          （jOOQ，依赖 web 复用 PageResponse；可独立使用，配合 web 时更顺畅）

cartisan-storage             （Spring Boot Starter，不依赖 core）

cartisan-payment             （Spring Boot Starter，不依赖 core）

cartisan-data-jpa ──→ cartisan-event（save 时发布领域事件）
cartisan-data-query ──→ cartisan-web（复用 PageResponse，读侧与写侧分页 API 一致）
```

**依赖方向铁律：**
- core 不依赖任何其他 cartisan 模块
- core 不依赖 Spring
- web、data-jpa、event、ai、test 依赖 core
- security 依赖 core + web
- data-query 依赖 web（复用 PageResponse，读侧与写侧分页 API 一致）
- storage、payment 是独立 Starter，仅依赖 Spring Boot
- 任何模块不产生循环依赖

---

## 六、业务项目接入方式

### 6.1 开发期：Gradle Composite Build

在框架和业务项目并行开发期间，使用 Composite Build 实现本地联调：

```
// aieducenter-platform/settings.gradle.kts
includeBuild("../cartisan-boot")
```

修改 cartisan-boot 后，业务项目立即可见，无需发布。

### 6.2 稳定期：私有 Maven 仓库

框架发布稳定版本后，业务项目通过 BOM 引入：

```
// aieducenter-platform/build.gradle.kts
dependencyManagement {
    imports {
        mavenBom("com.cartisan:cartisan-dependencies:1.0.0")
    }
}

dependencies {
    implementation("com.cartisan:cartisan-core")
    implementation("com.cartisan:cartisan-web")
    implementation("com.cartisan:cartisan-security")
    implementation("com.cartisan:cartisan-data-jpa")
    implementation("com.cartisan:cartisan-data-query")
    implementation("com.cartisan:cartisan-event")
    implementation("com.cartisan:cartisan-ai")         // 按需
    implementation("com.cartisan:cartisan-storage")    // 按需
    implementation("com.cartisan:cartisan-payment")    // 按需
    testImplementation("com.cartisan:cartisan-test")
}
```

### 6.3 业务项目推荐目录结构

```
aieducenter-platform/
├── CLAUDE.md
├── docs/
├── src/main/java/com/aieducenter/
│   ├── account/                    # Account Context
│   │   ├── domain/                 # 聚合根、实体、领域服务
│   │   ├── application/            # 应用服务
│   │   ├── controller/             # REST 控制器
│   │   └── infrastructure/         # 适配器（Repository 实现等）
│   ├── tenant/                     # Tenant Context（同上分层）
│   ├── ai/                         # AI Gateway Context
│   ├── billing/                    # Billing Context
│   └── ...
└── src/test/java/com/aieducenter/
    ├── ArchitectureTest.java       # 继承 CartisanArchRules
    └── ...
```

---

## 七、Epic 拆分建议

cartisan-boot 的开发可按以下 Epic 顺序推进：

| Epic | 内容 | 依赖 | 复杂度 | 优先级 |
|------|------|------|--------|--------|
| **Epic 1: 项目骨架 + Core + Test** | Gradle 多模块项目搭建、cartisan-core 全部类型、cartisan-test 的 ArchUnit 规则集 | 无 | M | P0 |
| **Epic 2: Web + Data-JPA + Event** | cartisan-web 统一响应和异常处理、cartisan-data-jpa Repository 和审计、cartisan-event 事件发布 | Epic 1 | L | P0 |
| **Epic 3: Security** | cartisan-security 认证抽象、多租户上下文、Sa-Token 集成 | Epic 2 | M | P0 |
| **Epic 4: Data-Query** | cartisan-data-query jOOQ 自动配置、代码生成、分页工具 | Epic 2 | S | P0 |
| **Epic 5: AI** | cartisan-ai 统一模型、Provider SPI + OpenAI/Anthropic/DeepSeek 实现、SSE 工具 | Epic 1 | M | P1 |
| **Epic 6: Storage** | cartisan-storage 存储 SPI + 阿里云 OSS / MinIO / Local 实现 | 无 | S | P2 |
| **Epic 7: Payment** | cartisan-payment 支付 SPI + 微信支付 / 支付宝实现 | 无 | M | P2 |

**开发节奏：**

- aieducenter-platform 可在 **Epic 1 完成后**即开始领域建模和业务开发（使用 Composite Build 引用）
- Epic 5（AI）在 aieducenter-platform 需要接入大模型时启动，可与 Epic 3/4 并行
- Epic 6/7（Storage、Payment）独立于其他模块，在业务需要时按需启动
- cartisan-ai-agent 不占 Epic，待框架生态稳定后单独规划
