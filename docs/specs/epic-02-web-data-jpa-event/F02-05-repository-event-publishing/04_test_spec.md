# F02-05 测试规格 (Test Spec)

> **Feature**: Repository 保存时自动发布领域事件
> **版本**: v1.0
> **日期**: 2026-03-14

---

## 一、测试策略

### 1.1 测试金字塔

| 层级 | 工具 | 覆盖范围 |
|------|------|----------|
| 单元测试 | JUnit 5 + Mockito | BaseRepositoryImpl 核心逻辑 |
| 集成测试 | @DataJpaTest + TestEntityManager | Repository 与 JPA/事件发布集成 |

### 1.2 测试原则

1. **TDD 驱动**：先写红灯测试，再写绿灯实现
2. **真实环境**：使用 @DataJpaTest 和 H2 内存数据库
3. **事件验证**：通过 TestEventCollector 验证事件是否被发布
4. **边界测试**：覆盖非 AggregateRoot 实体、异常场景

---

## 二、验收标准与测试映射

### AC1: save 后事件被发布

**测试**: `given_aggregateRootWithEvent_when_save_then_eventPublished`

**验证点**:
- 调用 `repository.save()` 后，事件被收集到 TestEventCollector
- 事件内容正确（aggregateId 匹配）

```java
TestAggregateRoot aggregateRoot = new TestAggregateRoot();
aggregateRoot.registerTestEvent("test-event");
testRepository.save(aggregateRoot);

assertThat(testEventCollector.getEvents())
    .hasSize(1)
    .allMatch(event -> event.getAggregateId().equals(aggregateRoot.getId()));
```

---

### AC2: 事件发布后被清空

**测试**: `given_aggregateRootWithEvent_when_save_then_eventsCleared`

**验证点**:
- save() 后，aggregateRoot 上的事件列表被清空
- 避免重复发布

```java
TestAggregateRoot aggregateRoot = new TestAggregateRoot();
aggregateRoot.registerTestEvent("test-event");
testRepository.save(aggregateRoot);

assertThat(aggregateRoot.getDomainEvents()).isEmpty();
```

---

### AC3: 非 AbstractAggregateRoot 不发布事件

**测试**: `given_nonAggregateRootEntity_when_save_then_noEventPublished`

**验证点**:
- 不继承 AbstractAggregateRoot 的实体，save() 不触发事件发布
- 使用 TestEntityManager 直接持久化 SimpleEntity

```java
SimpleEntity simpleEntity = new SimpleEntity();
testEntityManager.persist(simpleEntity);
testEntityManager.flush();

assertThat(testEventCollector.getEvents()).isEmpty();
```

---

### AC4: saveAll 批量发布事件

**测试**: `given_multipleAggregatesWithEvents_when_saveAll_then_allEventsPublished`

**验证点**:
- saveAll() 批量保存时，每个聚合根的事件都被发布
- 事件总数等于所有聚合根事件数量之和

```java
List<TestAggregateRoot> aggregates = List.of(
    new TestAggregateRoot(),
    new TestAggregateRoot()
);
aggregates.forEach(a -> a.registerTestEvent("batch-event"));
testRepository.saveAll(aggregates);

assertThat(testEventCollector.getEvents()).hasSize(2);
```

---

### AC5: 监听器异常导致事务回滚

**测试**: `given_listenerThrowsException_when_save_then_transactionRollback`

**验证点**:
- 事件监听器抛出异常时，整个事务回滚
- 数据不会被持久化

```java
TestAggregateRoot aggregateRoot = new TestAggregateRoot();
aggregateRoot.registerTestEvent("boom-event");

assertThatThrownBy(() -> testRepository.save(aggregateRoot))
    .isInstanceOf(RuntimeException.class)
    .hasMessageContaining("Listener exception");

// 验证数据未持久化
assertThat(testRepository.findById(aggregateRoot.getId())).isEmpty();
```

---

## 三、测试覆盖范围

### 3.1 覆盖的类

| 类 | 覆盖内容 |
|----|----------|
| BaseRepositoryImpl | save() 方法、publishDomainEvents() 方法 |
| DomainEventPublisherHolder | setPublisher()、getPublisher() |
| CartisanDataJpaAutoConfiguration | 两个 Bean 配置方法 |

### 3.2 测试类清单

| 测试类 | 类型 | 覆盖 AC |
|--------|------|---------|
| RepositoryEventPublishingIntegrationTest | 集成测试 | AC1-AC5 |

---

## 四、测试执行

```bash
# 运行 cartisan-data-jpa 模块测试
./gradlew :cartisan-data-jpa:test

# 运行完整检查（编译 + 测试 + ArchUnit）
./gradlew :cartisan-data-jpa:check
```

### 执行结果

```
RepositoryEventPublishingIntegrationTest
├── given_aggregateRootWithEvent_when_save_then_eventPublished ✅
├── given_aggregateRootWithEvent_when_save_then_eventsCleared ✅
├── given_nonAggregateRootEntity_when_save_then_noEventPublished ✅
├── given_multipleAggregatesWithEvents_when_saveAll_then_allEventsPublished ✅
└── given_listenerThrowsException_when_save_then_transactionRollback ✅

BUILD SUCCESSFUL
```

---

## 五、交叉审查

**审查日期**: 2026-03-14
**审查方式**: superpowers:code-reviewer subagent
**审查结论**: 通过
**遗留问题**: 无

### 修复的问题

1. **Critical**: AC3 测试缺失 — 已添加 `SimpleEntity` 测试用例
2. **Critical**: AutoConfiguration 未设置 repositoryBaseClass — 已添加 `JpaRepositoryFactoryEntryCustomizer`
3. **Important**: 测试配置冗余 — 已优化 RepositoryTestConfig

---

## 六、相关文档

- [01_requirement.md](./01_requirement.md) - 需求规格
- [02_interface.md](./02_interface.md) - 接口设计
- [03_implementation.md](./03_implementation.md) - 实现方案
