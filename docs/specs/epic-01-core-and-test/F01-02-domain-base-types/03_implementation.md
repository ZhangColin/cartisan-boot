# F01-02: cartisan-core — domain 基础类型（实现方案）

## 元数据

| 属性 | 值 |
|------|-----|
| Epic | Epic 01: 项目骨架 + Core + Test |
| Feature | F01-02: cartisan-core — domain 基础类型 |
| 文档版本 | v0.1.0 |
| 日期 | 2026-03-13 |
| 作者 | Claude |
| 状态 | Phase 3: 实现方案 |
| 前置文档 | [02_interface.md](./02_interface.md) |

---

## 1. 实现策略

### 1.1 总体策略

遵循 TDD（测试驱动开发）流程：
1. **红**：先写测试，描述期望的行为
2. **绿**：编写最简代码让测试通过
3. **重构**：优化代码质量

### 1.2 任务粒度

- **每个任务**：50-150 行代码
- **每个文件**：不超过 200 行（不含 JavaDoc）
- **每个测试**：覆盖一个具体场景

---

## 2. 原子任务清单

### TASK-01: 创建包结构和 package-info.java

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 代码量 | ~30 行 |

**实现内容：**
1. 创建 `cartisan-core/src/main/java/com/cartisan/core/domain/` 目录
2. 创建 `package-info.java`，包含包级别 JavaDoc

**验收标准：**
- 目录结构正确
- package-info.java 编译通过
- JavaDoc 格式符合规范

---

### TASK-02: 实现 AggregateRoot 标记接口

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 代码量 | ~30 行 |

**实现内容：**
1. 创建 `AggregateRoot.java` 标记接口
2. 添加完整的 JavaDoc

**验收标准：**
- 接口编译通过
- JavaDoc 包含使用示例

---

### TASK-03: 实现 AbstractAggregateRoot<T> 抽象类

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | TASK-01, TASK-02 |
| 代码量 | ~80 行 |

**实现内容：**
1. 创建 `AbstractAggregateRoot.java` 抽象类
2. 实现 `registerEvent(DomainEvent)` 方法
3. 实现 `getDomainEvents()` 方法（返回不可修改列表）
4. 实现 `clearDomainEvents()` 方法

**验收标准：**
- 抽象类编译通过
- `registerEvent(null)` 抛出 NullPointerException
- `getDomainEvents()` 返回不可修改列表
- `clearDomainEvents()` 清空列表

**测试场景：**
- 注册单个事件
- 注册多个事件
- 注册 null 事件抛异常
- getDomainEvents 返回不可修改列表
- clearDomainEvents 清空后列表为空

---

### TASK-04: 实现 Identity<T> 标识符接口

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 依赖 | TASK-01 |
| 代码量 | ~50 行 |

**实现内容：**
1. 创建 `Identity.java` 接口
2. 定义 `value()` 方法
3. 添加完整的 JavaDoc 和使用示例

**验收标准：**
- 接口编译通过
- JavaDoc 包含 Record 实现示例

**测试场景：**
- 使用 Record 实现 Identity
- 不同 ID 类型编译器拒绝混用（手动验证）

---

### TASK-05: 实现 DomainEvent 领域事件基类

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | TASK-01 |
| 代码量 | ~100 行 |

**实现内容：**
1. 创建 `DomainEvent.java` 抽象类
2. 定义 `eventId`、`occurredAt`、`aggregateId` 字段
3. 构造函数中自动生成 eventId 和 occurredAt
4. 验证 aggregateId 不为 null
5. 添加 `eventType()` 方法（返回类名）

**验收标准：**
- 抽象类编译通过
- `DomainEvent(null)` 抛出 NullPointerException
- `eventId` 格式为 UUID
- `occurredAt` 为当前时间

**测试场景：**
- 创建事件自动生成 eventId
- 创建事件自动生成 occurredAt
- aggregateId 为 null 抛异常
- eventType() 返回类名

---

### TASK-06: 实现 Entity<T, ID> 实体接口

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | TASK-01 |
| 代码量 | ~80 行 |

**实现内容：**
1. 创建 `Entity.java` 接口
2. 定义 `getId()` 方法
3. 实现 `sameIdentityAs(T)` 默认方法
4. 添加完整的 JavaDoc

**验收标准：**
- 接口编译通过
- `sameIdentityAs(null)` 返回 false
- `sameIdentityAs()` 正确比较 ID

**测试场景：**
- 相同 ID 的实体 sameIdentityAs 返回 true
- 不同 ID 的实体 sameIdentityAs 返回 false
- null 参数返回 false
- ID 为 null 的实体比较返回 false

---

### TASK-07: 实现 ValueObject<T> 值对象接口

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 依赖 | TASK-01 |
| 代码量 | ~60 行 |

**实现内容：**
1. 创建 `ValueObject.java` 接口
2. 实现 `sameValueAs(T)` 默认方法（委托 equals）
3. 添加完整的 JavaDoc 和 Record 示例

**验收标准：**
- 接口编译通过
- Record 实现零成本使用 sameValueAs

**测试场景：**
- Record 实现的值对象 sameValueAs 等价于 equals
- 可覆写 sameValueAs 支持自定义比较逻辑

---

### TASK-08: 编写 AggregateRootTest

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | TASK-02, TASK-03, TASK-05 |
| 代码量 | ~120 行 |

**实现内容：**
1. 创建测试目录 `cartisan-core/src/test/java/com/cartisan/core/domain/`
2. 创建 `AggregateRootTest.java`
3. 创建测试用事件 `TestDomainEvent`

**测试场景：**
- `registerEvent()` 正确添加事件
- `registerEvent(null)` 抛出 NullPointerException
- `getDomainEvents()` 返回不可修改列表
- 尝试修改 getDomainEvents() 返回列表抛出异常
- `clearDomainEvents()` 清空后列表为空

---

### TASK-09: 编写 IdentityTest

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | TASK-04 |
| 代码量 | ~80 行 |

**实现内容：**
1. 创建 `IdentityTest.java`
2. 创建测试用 ID 类型 `TestUserId`
3. 验证类型安全性

**测试场景：**
- Identity 接口可被 Record 实现
- value() 方法返回正确值
- 不同 ID 类型是不同类型（注释说明需要手动验证）

---

### TASK-10: 编写 DomainEventTest

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | TASK-05 |
| 代码量 | ~100 行 |

**实现内容：**
1. 创建 `DomainEventTest.java`
2. 创建测试用事件类 `TestDomainEvent`
3. 测试事件元数据自动生成

**测试场景：**
- eventId 自动生成且为 UUID 格式
- occurredAt 自动生成且为当前时间
- aggregateId 为 null 抛出 NullPointerException
- aggregateId 正确存储
- eventType() 返回类名

---

### TASK-11: 编写 EntityTest

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | TASK-06 |
| 代码量 | ~100 行 |

**实现内容：**
1. 创建 `EntityTest.java`
2. 创建测试实体类 `TestEntity`
3. 测试 sameIdentityAs 各种场景

**测试场景：**
- 相同 ID 的实体 sameIdentityAs 返回 true
- 不同 ID 的实体 sameIdentityAs 返回 false
- sameIdentityAs(null) 返回 false
- 两个 null ID 的实体 sameIdentityAs 返回 true
- 一个 null ID 一个非 null ID 的实体 sameIdentityAs 返回 false

---

### TASK-12: 编写 ValueObjectTest

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | TASK-07 |
| 代码量 | ~100 行 |

**实现内容：**
1. 创建 `ValueObjectTest.java`
2. 创建测试值对象类 `TestValueObject`
3. 测试 sameValueAs 默认行为

**测试场景：**
- 相同值的值对象 sameValueAs 返回 true
- 不同值的值对象 sameValueAs 返回 false
- sameValueAs(null) 返回 false
- 默认 sameValueAs 等价于 equals
- 覆写 sameValueAs 自定义比较逻辑

---

### TASK-13: 创建使用示例文档

| 属性 | 值 |
|------|-----|
| 优先级 | P1 |
| 预估工时 | 0.5h |
| 依赖 | TASK-01 ~ TASK-12 |
| 代码量 | ~200 行 |

**实现内容：**
1. 创建 `cartisan-core/src/test/java/com/cartisan/examples/domain/` 目录
2. 创建完整的领域模型示例（Order 聚合根）
3. 包含：OrderId、Order、OrderItem、ShippingAddress、OrderCreatedEvent、OrderShippedEvent

**验收标准：**
- 示例代码编译通过
- 覆盖所有 domain 类型
- 展示典型使用场景

---

### TASK-14: ArchUnit 架构验证

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | TASK-01 ~ TASK-13 |
| 代码量 | ~80 行 |

**实现内容：**
1. 创建 `cartisan-core/src/test/java/com/cartisan/core/arch/` 目录
2. 创建 `ArchitectureTest.java`
3. 验证 domain 包零外部依赖

**验证规则：**
- domain 包不依赖任何第三方库
- domain 包不依赖 Spring
- domain 包仅依赖 JDK 标准库

**验收标准：**
- ArchUnit 测试通过
- 规则失败时有清晰的错误消息

---

### TASK-15: 模块完整性验证

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | 所有 TASK |
| 代码量 | ~50 行 |

**实现内容：**
1. 运行 `./gradlew :cartisan-core:build`
2. 验证构建成功
3. 验证测试覆盖率 > 90%
4. 验证 JavaDoc 生成

**验收标准：**
- Gradle 构建成功
- 所有测试通过
- 测试覆盖率 > 90%
- JavaDoc 生成无警告

---

## 3. 任务依赖关系

```
TASK-01 (包结构)
    │
    ├──→ TASK-02 (AggregateRoot)
    │
    ├──→ TASK-04 (Identity) ──→ TASK-09 (IdentityTest)
    │
    ├──→ TASK-05 (DomainEvent) ──→ TASK-10 (DomainEventTest)
    │
    ├──→ TASK-06 (Entity) ──→ TASK-11 (EntityTest)
    │
    ├──→ TASK-07 (ValueObject) ──→ TASK-12 (ValueObjectTest)
    │
    └──→ TASK-03 (AbstractAggregateRoot) ──→ TASK-08 (AggregateRootTest)
              │
              └──→ TASK-13 (使用示例)
                      │
                      └──→ TASK-14 (ArchUnit 验证)
                              │
                              └──→ TASK-15 (模块完整性验证)
```

---

## 4. 推荐执行顺序

### 第一批：基础骨架（0.5h）
1. TASK-01: 创建包结构和 package-info.java
2. TASK-02: 实现 AggregateRoot 标记接口
3. TASK-04: 实现 Identity<T> 标识符接口

### 第二批：事件体系（1h）
4. TASK-05: 实现 DomainEvent 领域事件基类
5. TASK-10: 编写 DomainEventTest

### 第三批：实体和值对象（1h）
6. TASK-06: 实现 Entity<T, ID> 实体接口
7. TASK-11: 编写 EntityTest
8. TASK-07: 实现 ValueObject<T> 值对象接口
9. TASK-12: 编写 ValueObjectTest

### 第四批：聚合根（1h）
10. TASK-03: 实现 AbstractAggregateRoot<T> 抽象类
11. TASK-09: 编写 IdentityTest
12. TASK-08: 编写 AggregateRootTest

### 第五批：验证和文档（1.5h）
13. TASK-13: 创建使用示例文档
14. TASK-14: ArchUnit 架构验证
15. TASK-15: 模块完整性验证

**总计预估：5 小时**

---

## 5. 测试策略

### 5.1 单元测试覆盖

| 类型 | 测试类 | 关键场景 |
|------|--------|----------|
| AggregateRoot | AggregateRootTest | 事件注册、获取、清空 |
| Identity | IdentityTest | 类型安全、value() |
| DomainEvent | DomainEventTest | 元数据自动生成 |
| Entity | EntityTest | sameIdentityAs 各种场景 |
| ValueObject | ValueObjectTest | sameValueAs 默认和覆写 |

### 5.2 架构测试

- ArchUnit 验证零外部依赖
- ArchUnit 验证不依赖 Spring

### 5.3 覆盖率目标

- 行覆盖率：> 90%
- 分支覆盖率：> 80%

---

## 6. Phase 5 验证结果

### 5.1 Code Review
- ✅ 通过 superpowers:code-reviewer 审查
- ✅ 发现并修复 ValueObject.sameValueAs() 简化
- ✅ 验证 Entity.sameIdentityAs() 类型检查必要性（Java 类型擦除）

### 5.2 ArchUnit 架构验证
| 规则 | 状态 |
|------|------|
| domain 零第三方依赖 | ✅ 通过 |
| domain 仅依赖 JDK | ✅ 通过 |
| domain 不访问测试类 | ✅ 通过 |
| domain 接口为 public | ✅ 通过 |

### 5.3 单元测试覆盖
| 测试类 | 测试数 | 通过 |
|--------|--------|------|
| IdentityTest | 4 | ✅ |
| DomainEventTest | 7 | ✅ |
| AbstractAggregateRootTest | 8 | ✅ |
| EntityTest | 7 | ✅ |
| ValueObjectTest | 8 | ✅ |
| ArchitectureTest | 6 | ✅ |
| **总计** | **40** | **✅** |

### 5.4 PIT 变异测试
| 状态 | 说明 |
|------|------|
| ⚠️ 跳过 | Gradle 9.0 与 PIT 插件 (info.solidsoft.pitest) 不兼容 |

**限制说明：**
- 项目使用 Gradle 9.0（2025-07-31 发布）
- PIT 插件最新版本 1.15.0 尚不支持 Gradle 9.0
- 替代方案：使用 IntelliJ IDEA 的 PIT 插件，或降级 Gradle 到 8.x

**后续改进选项：**
1. 等待 PIT 插件更新支持 Gradle 9.0
2. 使用命令行方式运行 PIT（需要手动配置 classpath）
3. 暂时降级 Gradle 到 8.10 版本以运行 PIT

---

## 7. 相关文档

- [F01-02 需求文档](./01_requirement.md)
- [F01-02 接口设计](./02_interface.md)
- [00_epic_backlog.md](../00_epic_backlog.md)

---

## 7. 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v0.1.0 | 2026-03-13 | 初始版本 | Claude |
