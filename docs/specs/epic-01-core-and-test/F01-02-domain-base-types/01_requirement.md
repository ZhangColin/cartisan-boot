# F01-02: cartisan-core — domain 基础类型

## 元数据

| 属性 | 值 |
|------|-----|
| Epic | Epic 01: 项目骨架 + Core + Test |
| Feature | F01-02: cartisan-core — domain 基础类型 |
| 文档版本 | v0.1.0 |
| 日期 | 2026-03-13 |
| 作者 | Claude |
| 状态 | Phase 1: 需求定义 |
| 依赖 | F01-01: Gradle 多模块项目骨架 |
| 复杂度 | M |
| 预估工时 | 2d |

---

## 1. 问题陈述

### 1.1 背景

Spring Boot 项目在做 DDD（领域驱动设计）时，每个业务团队都要自己实现一套领域基础类型——聚合根、实体、值对象、领域事件。这些基础代码在各个项目中重复造轮子，而且实现方式五花八门：

- 有的用 `boolean equals()` 比较实体，有的用 `sameIdentityAs()`
- 有的领域事件用 Spring `ApplicationEvent`，有的自己造轮子
- 有的 ID 就用 `Long`，容易把 `userId` 和 `orderId` 混用
- 值对象没人用，因为写起来太麻烦（要手写 equals/hashCode/toString）

### 1.2 影响

这些问题导致：

1. **重复劳动**：每个项目都要重新设计和实现相同的基础设施
2. **不一致性**：不同项目的 DDD 实现风格差异大，代码难以复用
3. **类型不安全**：原始类型 ID 容易混用，编译器无法检查
4. **学习成本高**：新团队成员需要理解每个项目独特的 DDD 约定

### 1.3 目标

提供一套**零外部依赖**的 DDD 基础类型，让所有业务项目开箱即用。

---

## 2. 解决方案概述

### 2.1 设计原则

1. **纯粹** — 只依赖 JDK，不引入 Spring/JPA，保证领域模型不被框架污染
2. **类型安全** — 通过泛型和接口约束，让编译器帮你发现错误
3. **开箱即用** — 默认实现覆盖 99% 场景，特殊场景允许覆写

### 2.2 核心类型

| 类型 | 职责 |
|------|------|
| `AggregateRoot` | 标记接口，标识聚合根 |
| `AbstractAggregateRoot<T>` | 事件暂存器：registerEvent/getDomainEvents/clearDomainEvents |
| `Entity<T extends Entity<T, ID>, ID>` | getId() + sameIdentityAs() 默认实现 |
| `ValueObject<T extends ValueObject<T>>` | sameValueAs() 默认委托 equals |
| `Identity<T>` | 接口：T value() |
| `DomainEvent` | eventId/occurredAt 自动生成，aggregateId 必填 |

---

## 3. 功能范围

### 3.1 包含（In Scope）

| 类型 | 包路径 | 职责 |
|------|--------|------|
| `AggregateRoot` | `com.cartisan.core.domain` | 标记接口，标识聚合根 |
| `AbstractAggregateRoot<T>` | `com.cartisan.core.domain` | 事件暂存器：registerEvent/getDomainEvents/clearDomainEvents |
| `Entity<T extends Entity<T, ID>, ID>` | `com.cartisan.core.domain` | getId() + sameIdentityAs() 默认实现 |
| `ValueObject<T extends ValueObject<T>>` | `com.cartisan.core.domain` | sameValueAs() 默认委托 equals |
| `Identity<T>` | `com.cartisan.core.domain` | 接口：T value() |
| `DomainEvent` | `com.cartisan.core.domain` | eventId/occurredAt 自动生成，aggregateId 必填 |

### 3.2 不包含（Out of Scope）

| 功能 | 理由 | 归属 |
|------|------|------|
| `Auditable` / `SoftDeletable` | 属于持久化关注点 | F01-06（cartisan-data-jpa） |
| `CartisanException` 体系 | 属于异常处理领域 | F01-03 |
| Repository 接口 | 属于数据访问抽象 | cartisan-data-jpa |
| 事件发布机制 | 本 Feature 只做暂存 | cartisan-event + cartisan-data-jpa |

---

## 4. 用户故事

### US-01: 聚合根事件暂存

**作为** 领域建模者
**我想要** 在聚合根中注册领域事件
**以便** 在持久化后发布这些事件

**验收标准：**
- `registerEvent()` 能正确添加事件到列表
- `getDomainEvents()` 返回不可修改的列表
- `clearDomainEvents()` 清空后列表为空

### US-02: 实体标识性比较

**作为** 领域建模者
**我想要** 通过 ID 判断两个实体是否为同一实体
**以便** 在集合中去重或查找特定实体

**验收标准：**
- `sameIdentityAs()` 正确比较相同 ID 的实体
- `sameIdentityAs(null)` 返回 `false`
- `sameIdentityAs()` 处理 null ID（新建未保存的实体）

### US-03: 值对象值比较

**作为** 领域建模者
**我想要** 通过值相等性比较两个值对象
**以便** 将值对象用于 Map Key 或在集合中去重

**验收标准：**
- `sameValueAs()` 默认委托 `equals`
- Record 实现零成本使用 `sameValueAs`
- 允许覆写支持部分字段比较

### US-04: 标识符类型安全

**作为** 领域建模者
**我想要** 使用类型安全的标识符
**以便** 编译器阻止我把 `userId` 赋值给 `orderId` 字段

**验收标准：**
- `UserId(1L)` 和 `OrderId(1L)` 是不同类型
- 编译器拒绝不同 ID 类型比较

### US-05: 领域事件元数据

**作为** 领域建模者
**我想要** 领域事件自动携带 eventId 和 occurredAt
**以便** 事件发布后可追溯和排序

**验收标准：**
- `eventId` 自动生成 UUID
- `occurredAt` 自动生成当前时间
- `aggregateId` 必填，null 抛 NPE

---

## 5. 边界场景与异常处理

| 场景 | 处理方式 | 异常类型 |
|------|----------|----------|
| `registerEvent(null)` | 拒绝注册 | `NullPointerException` |
| `DomainEvent(null)` | 拒绝创建 | `NullPointerException` |
| `sameIdentityAs(null)` | 返回 `false` | 无异常 |
| `sameValueAs(null)` | 委托 `equals`，返回 `false` | 无异常 |
| 聚合根 ID 为 null（新建未保存） | `Objects.equals` 处理，返回 `false` | 无异常 |
| 事件列表并发修改 | 不处理（单线程聚合根修改） | - |
| ValueObject 需要部分字段比较 | 允许覆写 `sameValueAs` | - |

---

## 6. 非功能性需求

### 6.1 技术约束

- **JDK 版本**：Java 21（利用 Record、Sealed Classes）
- **依赖**：仅 JDK 标准库
- **测试框架**：JUnit 5 + AssertJ
- **架构验证**：ArchUnit（零 Spring 依赖规则）

### 6.2 质量标准

- 单元测试覆盖率 > 90%
- 所有 public API 有 JavaDoc
- package-info.java 说明模块职责

### 6.3 架构约束

- **零外部依赖**：仅依赖 JDK 标准库
- **ArchUnit 验证**：无 Spring/JPA 依赖

---

## 7. 验收标准

### 7.1 功能验收

| 类型 | 验收标准 |
|------|----------|
| 聚合根事件暂存 | `registerEvent()` 能正确添加事件到列表；`getDomainEvents()` 返回不可修改的列表；`clearDomainEvents()` 清空后列表为空 |
| 实体标识性比较 | `sameIdentityAs()` 正确比较相同 ID 的实体；`sameIdentityAs(null)` 返回 `false`；`sameIdentityAs()` 处理 null ID |
| 值对象值比较 | `sameValueAs()` 默认委托 `equals`；Record 实现零成本使用 `sameValueAs`；允许覆写支持部分字段比较 |
| 标识符类型安全 | `UserId(1L)` 和 `OrderId(1L)` 是不同类型；编译器拒绝不同 ID 类型比较 |
| 领域事件元数据 | `eventId` 自动生成 UUID；`occurredAt` 自动生成当前时间；`aggregateId` 必填，null 抛 NPE |

### 7.2 质量验收

- **零外部依赖**：仅依赖 JDK 标准库；ArchUnit 验证无 Spring/JPA 依赖
- **代码质量**：单元测试覆盖率 > 90%；所有 public API 有 JavaDoc；package-info.java 说明模块职责

---

## 8. 依赖关系

```
F01-01 (Gradle 多模块项目骨架)
    │
    └──→ F01-02 (domain 基础类型) ◄── 当前文档
```

---

## 9. 相关文档

- [F01-02 接口设计](./02_interface.md)
- [F01-02 实现方案](./03_implementation.md)
- [00_epic_backlog.md](../00_epic_backlog.md)

---

## 10. 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v0.1.0 | 2026-03-13 | 初始版本 | Claude |
