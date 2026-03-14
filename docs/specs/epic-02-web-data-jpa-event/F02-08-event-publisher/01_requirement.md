# Feature: F02-08 cartisan-event 领域事件发布器

> 版本：v0.1 | 日期：2026-03-14
> Epic：Epic 2 - Web + Data-JPA + Event
> 依赖：无（批次 1，可并行开发）
> 复杂度：M | 预估代码量：80-120 行

---

## 背景

cartisan-core 已提供领域事件基础类型（`DomainEvent`、`AbstractAggregateRoot`），聚合根可以通过 `registerEvent()` 注册事件。但缺少发布这些事件的基础设施。

F02-05（BaseRepositoryImpl）需要在 `save()` 方法中自动发布领域事件，因此需要先实现 cartisan-event 模块，提供事件发布能力。

## 目标

- 提供统一的领域事件发布器接口 `DomainEventPublisher`
- 提供基于 Spring Events 的默认实现 `SpringDomainEventPublisher`
- 通过 Spring Boot 自动配置实现零配置引入
- 支持用户自定义发布器实现覆盖

## 范围

### 包含（In Scope）

- `DomainEventPublisher` 接口
- `SpringDomainEventPublisher` 实现（基于 Spring 4.2+ `ApplicationEventPublisher`）
- `CartisanEventAutoConfiguration` 自动配置
- Spring Boot 自动配置发现文件
- 单元测试

### 不包含（Out of Scope）

- 消息队列发布器（如 Kafka、RabbitMQ）—— 预留扩展接口，不实现
- 事件存储（Event Sourcing）—— 属于业务层或未来扩展
- 事件重试/死信队列 —— 暂不实现

## 验收标准（Acceptance Criteria）

### AC1: 基础发布功能
- **Given** 已创建 `SpringDomainEventPublisher` Bean
- **When** 调用 `publish(domainEvent)`
- **Then** 事件成功发布到 Spring 事件总线

### AC2: 监听器可接收事件
- **Given** 已注册 `@EventListener` 监听器
- **When** 发布 `OrderCreatedEvent`（继承 `DomainEvent`）
- **Then** 监听器收到原始 `OrderCreatedEvent` 实例

### AC3: 支持事务后监听
- **Given** 监听器使用 `@TransactionalEventListener(phase = AFTER_COMMIT)`
- **When** 在事务内发布事件，事务成功提交
- **Then** 监听器在事务提交后执行

### AC4: 自动配置生效
- **Given** 引入 `cartisan-event` 依赖
- **When** Spring Boot 启动
- **Then** `DomainEventPublisher` Bean 自动注册

### AC5: 支持用户自定义覆盖
- **Given** 用户自定义 `DomainEventPublisher` Bean
- **When** Spring Boot 启动
- **Then** 默认的 `SpringDomainEventPublisher` 不注册，用户 Bean 生效

## 约束

### 架构约束
- cartisan-core 保持零 Spring 依赖
- cartisan-event 依赖 cartisan-core + spring-context
- 接口与实现分离，支持用户自定义实现

### 编码规范
- 使用构造函数注入，禁止 `@Autowired` 字段注入
- 使用 AssertJ 进行断言
- 测试命名遵循 `given_..._when_..._then_...` 格式

### 性能约束
- 事件发布为同步操作，性能由 Spring 事件总线保证
- 不在发布器内引入异步/重试逻辑（由监听器自行处理）

## 依赖模块

| 模块 | 用途 |
|------|------|
| cartisan-core | 依赖 `DomainEvent` 基类 |
| spring-context | 使用 `ApplicationEventPublisher` |
| spring-boot-autoconfigure | 自动配置注解 |

## 后续 Feature 依赖

本 Feature 被 F02-05（BaseRepositoryImpl）依赖，必须在 F02-05 之前完成。

---

## 附录：关键设计决策

### 事件发布时机
- **调用时机**：在 `BaseRepositoryImpl.save()` 中、事务内、save 之后同步调用
- **执行时机**：由监听器的 `@TransactionalEventListener(phase = ...)` 控制
- **推荐**：`phase = TransactionPhase.AFTER_COMMIT`，保证事件对应已持久化的数据

### 事件包装方式
- 直接发布 `DomainEvent`，利用 Spring 4.2+ 的 `PayloadApplicationEvent` 机制
- 不创建自定义包装类，监听器直接接收领域事件子类

### 自动配置策略
- 使用 `@ConditionalOnMissingBean(DomainEventPublisher.class)` 支持用户覆盖
- 不提供 `enabled` 配置开关，职责单一无需关闭
