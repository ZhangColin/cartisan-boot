# Feature: F02-05 Repository 保存时自动发布领域事件

> **Epic**: Epic 2 - Web + Data-JPA + Event
> **依赖**: F02-04 (BaseRepository) + F02-08 (DomainEventPublisher)
> **复杂度**: L (Large，跨模块集成)
> **日期**: 2026-03-14

---

## 背景

在 DDD 架构中，聚合根通过领域事件来通知外部系统其状态变化。当前 cartisan-core 已提供 `AbstractAggregateRoot` 基类用于事件注册（`registerEvent()`），cartisan-event 已提供 `DomainEventPublisher` 用于事件发布。

但业务代码每次保存聚合根后，需要手动调用发布逻辑：
```java
orderRepository.save(order);
domainEventPublisher.publish(order.getDomainEvents());  // 手动发布
order.clearDomainEvents();  // 手动清空
```

这种方式存在以下问题：
1. **易遗漏**：开发者可能忘记调用发布逻辑
2. **样板代码**：每个 save 操作后都要重复相同代码
3. **不一致**：不同项目可能有不同的实现方式

## 目标

实现 `BaseRepositoryImpl`，在 Repository `save()` 时自动发布领域事件，使业务代码只需：
```java
orderRepository.save(order);  // 事件自动发布
```

## 范围

### 包含（In Scope）

1. **BaseRepositoryImpl** - 继承 `SimpleJpaRepository`，重写 `save()` 方法
2. **CartisanJpaRepositoryFactory** - 自定义 RepositoryFactory，负责创建 `BaseRepositoryImpl` 实例
3. **CartisanJpaRepositoryFactoryBean** - 自定义 FactoryBean，负责创建 Factory 并注入 ApplicationContext
4. **集成测试** - 验证 save → 事件发布 → 清空 的完整链路
5. **自动配置** - 在 `CartisanDataJpaAutoConfiguration` 中启用自定义工厂

### 不包含（Out of Scope）

1. 异步事件发布 - 由业务通过 `@TransactionalEventListener(AFTER_COMMIT)` 自行处理
2. 事件存储/溯源 - 本 Feature 仅实现即时发布
3. 事件重试/补偿机制 - 事务内发布失败直接回滚

## 验收标准（Acceptance Criteria）

| AC | 描述 | 测试方式 |
|----|------|---------|
| **AC1** | 调用 `repository.save(aggregateRoot)` 后，聚合根上注册的事件被发布到 Spring 事件总线 | 集成测试：使用 `@EventListener` 验证事件被接收 |
| **AC2** | 事件发布后，聚合根的 `domainEvents` 列表被清空 | 集成测试：save 后 `getDomainEvents()` 返回空列表 |
| **AC3** | 只继承 `AbstractAggregateRoot` 的实体会触发事件发布，其他实体不受影响 | 单元测试：普通实体 save 不触发事件发布 |
| **AC4** | `saveAll()` 会为每个实体触发事件发布 | 集成测试：saveAll 多个聚合根，验证所有事件被发布 |
| **AC5** | 事件发布异常会导致整个事务回滚 | 集成测试：监听器抛异常，验证数据未持久化 |
| **AC6** | `BaseRepositoryImpl` 通过自定义 FactoryBean + Factory 获得 `DomainEventPublisher` | 单元测试：验证三参构造器被正确调用 |

## 约束

### 技术约束

1. **泛型约束** - `BaseRepository<T extends AggregateRoot<?>, ID>` 确保只有聚合根可以有 Repository
2. **依赖注入** - 必须通过自定义 FactoryBean + Factory 实现 `DomainEventPublisher` 的三参构造器注入
3. **事务边界** - 事件在事务内同步发布，不使用 `@TransactionalEventListener(AFTER_COMMIT)`

### 性能约束

- save 方法执行时间增加应控制在 10% 以内（仅事件发布开销）
- 不应引入额外的数据库查询

### 安全约束

- 事件发布失败必须触发事务回滚，防止数据与事件不一致

## 设计决策

相关技术决策已记录在 `docs/decisions/DECISIONS.md`：

- **ADR-032**: 选用 SimpleJpaRepository 继承方式
- **ADR-033**: 事件发布采用事务内同步模式
- **ADR-034**: 仅重写 save(S entity)，不重写 saveAll/saveAndFlush
- **ADR-035**: 使用 FactoryBean + Factory 两层结构注入 DomainEventPublisher

## 核心流程（伪代码）

```
用户调用: orderRepository.save(order)
    ↓
BaseRepositoryImpl.save(order)
    ↓
super.save(order)              → JPA 持久化到数据库
    ↓
publishDomainEvents(order)
    ↓
    ├─ if (order instanceof AbstractAggregateRoot)
    ├─ events = order.getDomainEvents()
    ├─ events.forEach(domainEventPublisher::publish)  → 发布到 Spring 事件总线
    └─ order.clearDomainEvents()
    ↓
return order
```

## 交付物

| 文件 | 说明 |
|------|------|
| `BaseRepositoryImpl.java` | Repository 实现类，重写 save() 方法 |
| `CartisanJpaRepositoryFactory.java` | 自定义 RepositoryFactory |
| `CartisanJpaRepositoryFactoryBean.java` | 自定义 FactoryBean |
| `BaseRepositoryImplTest.java` | 单元测试 |
| `RepositoryEventPublishingIntegrationTest.java` | 集成测试 |

## 相关文档

- 依赖：[F02-04 BaseRepository](../F02-04-base-repository/)
- 依赖：[F02-08 DomainEventPublisher](../F02-08-domain-event-publisher/)
- 基础：[Epic 1 AggregateRoot](../../epic-01-core-domain/F01-02-aggregate-root/)
