# Epic 2: Web + Data-JPA + Event — Backlog

> **Epic 目标**：构建 cartisan-boot 的 Web 层规范、JPA 写侧封装、领域事件基础设施
>
> **依赖**：Epic 1（cartisan-core 已完成 AggregateRoot、DomainEvent、CodeMessage 等基础类型）
>
> **复杂度**：L（Large，跨 3 个模块，含集成验证）

---

## Feature 列表

| ID | Feature | 描述 | 复杂度 | 预估代码量 |
|----|---------|------|--------|-----------|
| **F02-01** | cartisan-web 响应体 | `ApiResponse<T>`、`PageResponse<T>`、静态工厂方法 | S | 50-80 行 |
| **F02-02** | cartisan-web 异常处理 | `GlobalExceptionHandler`（异常→响应映射） | M | 100-150 行 |
| **F02-03** | cartisan-web 请求上下文 | `RequestContext`、`RequestContextFilter`、requestId 生成 | S | 60-100 行 |
| **F02-04** | cartisan-data-jpa Repository | `BaseRepository<T, ID>` 接口、泛型约束 `T extends AggregateRoot` | M | 50-100 行 |
| **F02-05** | cartisan-data-jpa 事件发布 | `BaseRepositoryImpl` 实现 save 时自动发布事件（含集成测试） | L | 150-250 行 |
| **F02-06** | cartisan-data-jpa 审计与软删除 | `Auditable`/`SoftDeletable` 基类、自动配置 | M | 100-150 行 |
| **F02-07** | cartisan-data-jpa 分布式 ID | `TsidGenerator` | S | 30-50 行 |
| **F02-08** | cartisan-event 发布器 | `DomainEventPublisher` 接口、`SpringDomainEventPublisher` 实现 | M | 80-120 行 |
| **F02-09** | cartisan-web/data-jpa/event 自动配置 | Spring Boot AutoConfiguration、条件装配、配置属性 | M | 150-200 行 |

---

## 依赖关系

```
F02-01 (ApiResponse) ─────────────────────────────┐
F02-02 (GlobalExceptionHandler) ────→ F02-01 ──────┤
F02-03 (RequestContext) ───────────────────────────┤
                                                   ├──→ F02-09 (AutoConfiguration)
F02-04 (BaseRepository) ───────────────────────────┤
F02-08 (DomainEventPublisher) ─────────────────────┤
                                                   │
F02-05 (BaseRepositoryImpl) ──────→ F02-04 ───→ F02-08 │
F02-06 (Auditable/SoftDeletable) ──────────────────┤
F02-07 (TsidGenerator) ────────────────────────────┘
```

**关键依赖说明：**
- `F02-02` 依赖 `F02-01`：异常处理器需要构造 `ApiResponse`
- `F02-05` 依赖 `F02-04 + F02-08`：`BaseRepositoryImpl` 继承 `BaseRepository` 并调用 `DomainEventPublisher`
- `F02-09` 依赖所有：自动配置需要扫描并装配所有组件

---

## 推荐开发顺序

### 批次 1（并行）— 核心能力建立

| Feature | 理由 |
|---------|------|
| F02-01 | 无依赖，提供基础响应体 |
| F02-03 | 无依赖，提供请求上下文 |
| F02-04 | 无依赖，定义 Repository 契约 |
| F02-08 | 无依赖，定义事件发布器接口 |

### 批次 2（并行）— 基于批次 1 构建

| Feature | 依赖 |
|---------|------|
| F02-02 | F02-01 |
| F02-06 | F02-04 |
| F02-07 | 无（可并入批次 1）|

### 批次 3（串行）— 跨模块集成

| Feature | 依赖 | 说明 |
|---------|------|------|
| F02-05 | F02-04 + F02-08 | **关键路径**：在 `BaseRepositoryImpl.save()` 中实现事件自动发布，测试中验证 save → 事件发布链路 |

### 批次 4（收尾）— 统一配置

| Feature | 依赖 | 说明 |
|---------|------|------|
| F02-09 | 以上全部 | Spring Boot AutoConfiguration，实现零配置引入 |

---

## 各 Feature 详细说明

### F02-01: cartisan-web 响应体

**交付物：**
- `ApiResponse.java`（Record）
  - 字段：`code: int`, `message: String`, `data: T`, `requestId: String`
  - 静态工厂：`ok(T data)`, `ok()`, `error(CodeMessage)`
- `PageResponse.java`（Record）
  - 字段：`items: List<T>`, `total: long`, `page: int`, `size: int`

**测试：**
- 单元测试验证静态工厂方法正确构造响应体
- 验证泛型类型安全

---

### F02-02: cartisan-web 异常处理

**交付物：**
- `GlobalExceptionHandler.java`（`@ControllerAdvice`）
  - `CartisanException` → `ApiResponse.error(exception.codeMessage)`
  - `ConstraintViolation` → `ApiResponse.error(400, ...)`
  - `MethodArgumentNotValid` → `ApiResponse.error(400, ...)`
  - `AccessDeniedException` → `ApiResponse.error(403, ...)`
  - `Exception` → `ApiResponse.error(500, ...)` + 日志

**测试：**
- 集成测试使用 MockMvc 验证各类异常映射到正确的 HTTP 状态码和响应格式

---

### F02-03: cartisan-web 请求上下文

**交付物：**
- `RequestContext.java`（ThreadLocal / ScopedValue）
  - `getRequestId()`, `getClientIp()`
- `RequestContextFilter.java`（`@Component`）
  - 生成/读取 requestId
  - 请求结束时清理

**测试：**
- 单元测试验证 ThreadLocal 行为
- 集成测试验证 Filter 链路

---

### F02-04: cartisan-data-jpa Repository

**交付物：**
- `BaseRepository<T extends AggregateRoot, ID extends Serializable>`
  - 继承 `JpaRepository<T, ID>`, `JpaSpecificationExecutor<T>`
  - 泛型约束强制只有聚合根才能有 Repository

**测试：**
- ArchUnit 规则验证 `BaseRepository` 的泛型约束
- 接口契约测试

---

### F02-05: cartisan-data-jpa 事件发布

**交付物：**
- `BaseRepositoryImpl.java`
  - 实现 `save()` 方法：调用 JPA save → 获取 domainEvents → 发布事件 → 清空事件

**测试（关键集成点）：**
- 集成测试：
  1. 创建测试实体继承 `AbstractAggregateRoot`
  2. 在实体中 `registerEvent(new TestEvent(...))`
  3. 调用 `repository.save(entity)`
  4. 使用 `@EventListener` stub 验证事件被发布

---

### F02-06: cartisan-data-jpa 审计与软删除

**交付物：**
- `Auditable.java`（`@MappedSuperclass`）
  - `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`
- `SoftDeletable.java` extends `Auditable`
  - `deleted: boolean = false`
  - `@SQLRestriction("deleted = false")`
- JPA Auditing 自动配置

**测试：**
- 集成测试验证 `@CreatedDate` / `@LastModifiedDate` 自动填充
- 集成测试验证软删除查询自动过滤

---

### F02-07: cartisan-data-jpa 分布式 ID

**交付物：**
- `TsidGenerator.java`
  - 基于 TSID（Time-Sorted ID）算法
  - `generate() → Long`

**测试：**
- 单元测试验证唯一性、有序性、可提取时间戳

---

### F02-08: cartisan-event 发布器

**交付物：**
- `DomainEventPublisher.java`（接口）
  - `publish(DomainEvent event)`
- `SpringDomainEventPublisher.java`（实现）
  - 包装为 Spring `ApplicationEvent` 发布
  - 支持 `@TransactionalEventListener`

**测试：**
- 单元测试验证事件包装和发布

---

### F02-09: cartisan-web/data-jpa/event 自动配置

**交付物：**
- `CartisanWebAutoConfiguration.java`
- `CartisanDataJpaAutoConfiguration.java`
- `CartisanEventAutoConfiguration.java`
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 配置属性类（可选）

**测试：**
- 集成测试验证条件装配
- 验证引入依赖后自动生效

---

## 复杂度评估标准

| 复杂度 | 代码量 | 特征 |
|--------|--------|------|
| **S** | 50-80 行 | 纯数据类、简单工具、无复杂集成 |
| **M** | 100-200 行 | 涉及 Spring 集成、多组件协作、中等复杂度 |
| **L** | 200-300 行 | 跨模块集成、复杂状态管理、需要仔细设计 |

---

## 参考文档

- 设计文档：[cartisan-boot-设计文档.md](../../cartisan-boot-设计文档.md)
- Epic 1 规格：[epic-01-spec.md](./epic-01-spec.md)
- AI 协作 SOP：[AI协作开发SOP.md](../sop/AI协作开发SOP.md)
