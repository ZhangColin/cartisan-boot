# F01-02: cartisan-core — domain 基础类型（测试规格）

## 元数据

| 属性 | 值 |
|------|-----|
| Epic | Epic 01: 项目骨架 + Core + Test |
| Feature | F01-02: cartisan-core — domain 基础类型 |
| 文档版本 | v1.0.0 |
| 日期 | 2026-03-13 |
| 状态 | Phase 5: 已归档 |

---

## 1. 测试策略

### 1.1 测试分层

| 层级 | 工具 | 覆盖范围 |
|------|------|----------|
| 单元测试 | JUnit 5 + AssertJ | 所有 domain 类的核心行为 |
| 架构测试 | ArchUnit | 零外部依赖约束 |

### 1.2 覆盖率目标

| 指标 | 目标 | 实际 |
|------|------|------|
| 行覆盖率 | > 90% | ~95% |
| 分支覆盖率 | > 80% | ~90% |

### 1.3 PIT 变异测试

| 状态 | 说明 |
|------|------|
| ✅ 已启用 | 使用 `info.solidsoft.pitest` 1.19.0-rc.3，兼容 Gradle 9.0；变异杀死率 94%，满足 ≥70% 门禁 |

---

## 2. 单元测试清单

### 2.1 IdentityTest

| 测试方法 | 场景 | 验证点 |
|---------|------|--------|
| givenRecordImplementation_whenGetValue_thenReturnCorrectValue | Record 实现 | value() 返回正确值 |
| givenLongIdentity_whenGetValue_thenReturnCorrectLong | Long 类型 ID | 类型安全 |
| givenDifferentRecordTypes_whenCompare_thenTypesAreDifferent | 不同 ID 类型 | 编译期类型安全 |
| givenNullValue_whenGetValue_thenReturnNull | null 值 | 允许 null ID |

### 2.2 DomainEventTest

| 测试方法 | 场景 | 验证点 |
|---------|------|--------|
| givenValidAggregateId_whenCreateEvent_thenEventIdIsGenerated | 创建事件 | eventId 自动生成 UUID |
| givenValidAggregateId_whenCreateEvent_thenOccurredAtIsSetToNow | 创建事件 | occurredAt 为当前时间 |
| givenNullAggregateId_whenCreateEvent_thenThrowsNullPointerException | null aggregateId | 抛出 NPE |
| givenValidAggregateId_whenCreateEvent_thenAggregateIdIsStored | 存储 aggregateId | 正确存储 |
| givenEvent_whenGetEventType_thenReturnClassName | 获取事件类型 | 返回类名 |
| givenMultipleEvents_whenCreate_thenEachEventHasUniqueEventId | 多个事件 | eventId 唯一 |
| givenMultipleEvents_whenCreate_thenOccurredAtIsDifferent | 多个事件 | occurredAt 递增 |

### 2.3 AbstractAggregateRootTest

| 测试方法 | 场景 | 验证点 |
|---------|------|--------|
| givenAggregateRoot_whenRegisterEvent_thenEventIsAdded | 注册单个事件 | 事件被添加 |
| givenAggregateRoot_whenRegisterMultipleEvents_thenAllEventsAreAdded | 注册多个事件 | 所有事件被添加 |
| givenAggregateRoot_whenRegisterNullEvent_thenThrowsNullPointerException | 注册 null 事件 | 抛出 NPE |
| givenAggregateRoot_whenGetDomainEvents_thenReturnsUnmodifiableList | 获取事件列表 | 返回不可修改列表 |
| givenAggregateRoot_whenClearDomainEvents_thenListIsEmpty | 清空事件 | 列表变为空 |
| givenAggregateRoot_whenClearDomainEventsMultipleTimes_thenNoException | 多次清空 | 不抛异常 |
| givenAggregateRoot_whenRegisterEventAfterClear_thenEventIsAdded | 清空后注册 | 可以继续注册 |
| givenAggregateRoot_whenDoSomething_thenEventIsRegistered | 业务方法触发 | 事件正确注册 |

### 2.4 EntityTest

| 测试方法 | 场景 | 验证点 |
|---------|------|--------|
| givenTwoEntitiesWithSameId_whenSameIdentityAs_thenReturnTrue | 相同 ID | 返回 true |
| givenTwoEntitiesWithDifferentIds_whenSameIdentityAs_thenReturnFalse | 不同 ID | 返回 false |
| givenEntityAndNull_whenSameIdentityAs_thenReturnFalse | null 参数 | 返回 false |
| givenTwoEntitiesWithNullIds_whenSameIdentityAs_thenReturnTrue | 两个 null ID | 返回 true |
| givenEntityWithNullIdAndEntityWithNonNullId_whenSameIdentityAs_thenReturnFalse | 一个 null 一个非 null | 返回 false |
| givenEntity_whenSameIdentityAsSelf_thenReturnTrue | 自身比较 | 返回 true |
| givenEntityWithDifferentType_whenSameIdentityAs_thenReturnFalse | 不同类型 | 编译期类型安全 |

### 2.5 ValueObjectTest

| 测试方法 | 场景 | 验证点 |
|---------|------|--------|
| givenTwoValueObjectsWithSameValue_whenSameValueAs_thenReturnTrue | 相同值 | 返回 true |
| givenTwoValueObjectsWithDifferentValue_whenSameValueAs_thenReturnFalse | 不同值 | 返回 false |
| givenValueObjectAndNull_whenSameValueAs_thenReturnFalse | null 参数 | 返回 false |
| givenValueObject_whenSameValueAsSelf_thenReturnTrue | 自身比较 | 返回 true |
| givenRecordValueObject_whenSameValueAs_thenEquivalentToEquals | Record 实现 | 等价于 equals |
| givenCustomValueObject_whenSameValueAs_thenUsesCustomLogic | 自定义逻辑 | 可覆写 |
| givenCustomValueObjectAndNull_whenSameValueAs_thenReturnFalse | 自定义 null 检查 | 返回 false |
| givenDifferentValueObjectTypes_whenSameValueAs_thenCompileTimeSafety | 不同类型 | 编译期类型安全 |

---

## 3. 架构测试清单 (ArchitectureTest)

| 测试方法 | 验证规则 | 状态 |
|---------|---------|------|
| domainPackage_shouldNotDependOnAnyThirdPartyLibrary | 不依赖任何第三方库 | ✅ |
| domainPackage_shouldOnlyDependOnJdk | 仅依赖 JDK | ✅ |
| domainPackage_shouldNotAccessTestClasses | 不访问测试类 | ✅ |
| domainInterfaces_shouldBePublic | 接口为 public | ✅ |

---

## 4. 测试执行结果

### 4.1 最终统计

```
总测试数: 40
通过: 40
失败: 0
错误: 0
执行时间: ~1s
```

### 4.2 测试报告位置

- HTML 报告: `cartisan-core/build/reports/tests/test/index.html`
- XML 报告: `cartisan-core/build/test-results/test/`

### 4.3 构建验证

```bash
./gradlew :cartisan-core:clean :cartisan-core:build
BUILD SUCCESSFUL
```

### 4.4 PIT 变异测试结果

```bash
./gradlew :cartisan-core:pitest
```

| 指标 | 结果 |
|------|------|
| 变异生成数 | 16 |
| 变异杀死数 | 15 |
| 变异杀死率 | 94%（门禁 ≥70%） |
| 行覆盖率（被变异类） | 96% |
| 报告位置 | `cartisan-core/build/reports/pitest/` |

---

## 5. 已知限制

| 项 | 说明 | 计划 |
|----|------|------|
| PIT 插件版本 | 使用 RC 版 1.19.0-rc.3 以兼容 Gradle 9.0 | 待插件发布 1.19 正式版后可升级 |

---

## 6. 相关文档

- [01_requirement.md](./01_requirement.md) — 需求规格
- [02_interface.md](./02_interface.md) — 接口设计
- [03_implementation.md](./03_implementation.md) — 实现方案
