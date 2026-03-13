# Feature: F01-05 — util 工具类 — 需求规格

> 版本：v0.1 | 日期：2026-03-13
> 依赖：F01-03 (exception)

---

## 背景

在领域模型和应用服务中，存在大量防御式编程场景：
- 聚合根构造函数需要校验不变量
- 领域方法需要校验前置条件（如"订单已发货不能取消"）
- 应用服务需要从 Repository 加载聚合根，不存在时返回 404

当前开发者需要手动编写 `if (!condition) throw new DomainException(...)` 样板代码。这不仅重复，而且容易遗漏校验，导致不一致的错误处理。

需要一个统一的断言工具类，简化这些场景。

---

## 目标

1. 提供 Design by Contract 风格的断言工具（前置条件、后置条件、存在性断言）
2. 减少样板代码，提高代码可读性
3. 统一异常类型和错误码规范
4. 通过 API 约束引导正确的架构分层

---

## 范围

### 包含（In Scope）

| 功能 | 说明 |
|------|------|
| `require(boolean, CodeMessage)` | 前置条件断言，失败抛出 `DomainException` |
| `ensure(boolean, String)` | 后置条件断言，失败抛出 `IllegalStateException` |
| `requirePresent(Optional<T>)` | 存在性断言快捷版，使用标准 404 |
| `requirePresent(Optional<T>, CodeMessage)` | 存在性断言完整版，自定义错误码 |

### 不包含（Out of Scope）

| 功能 | 原因 |
|------|------|
| 对 `null` 值的断言 | 使用 `Optional.ofNullable()` 在调用点转换 |
| 集合/数组断言（非空、非包含 null） | 非核心功能，后续需求可扩展 |
| `Supplier<Optional>` 懒加载重载 | `findById()` 已返回 Optional，无实际收益 |
| 断言失败时的日志记录 | 由全局异常处理器统一处理 |

---

## 验收标准（Acceptance Criteria）

### AC1: require 前置条件断言

**GIVEN** 条件为 false
**WHEN** 调用 `Assertions.require(condition, codeMessage)`
**THEN** 抛出 `DomainException`，携带指定的 `codeMessage`

### AC2: require 成功路径

**GIVEN** 条件为 true
**WHEN** 调用 `Assertions.require(condition, codeMessage)`
**THEN** 静默通过，不抛出异常

### AC3: ensure 后置条件断言

**GIVEN** 条件为 false
**WHEN** 调用 `Assertions.ensure(condition, message)`
**THEN** 抛出 `IllegalStateException`，消息包含 "Postcondition violated: " + message

### AC4: ensure 成功路径

**GIVEN** 条件为 true
**WHEN** 调用 `Assertions.ensure(condition, message)`
**THEN** 静默通过，不抛出异常

### AC5: requirePresent 快捷版 — Optional 为空

**GIVEN** `Optional.empty()`
**WHEN** 调用 `Assertions.requirePresent(optional)`
**THEN** 抛出 `DomainException`，错误码为 `BaseCodeMessage.NOT_FOUND`

### AC6: requirePresent 快捷版 — Optional 有值

**GIVEN** `Optional.of(value)`
**WHEN** 调用 `Assertions.requirePresent(optional)`
**THEN** 返回 value

### AC7: requirePresent 完整版 — Optional 为空

**GIVEN** `Optional.empty()` 和自定义 `codeMessage`
**WHEN** 调用 `Assertions.requirePresent(optional, codeMessage)`
**THEN** 抛出 `DomainException`，携带指定的 `codeMessage`

### AC8: requirePresent 完整版 — Optional 有值

**GIVEN** `Optional.of(value)` 和自定义 `codeMessage`
**WHEN** 调用 `Assertions.requirePresent(optional, codeMessage)`
**THEN** 返回 value（忽略 `codeMessage`）

### AC9: 类设计约束

**GIVEN** `Assertions` 类
**THEN** 应满足：
- 类为 `final`
- 无公共构造函数（不可实例化）
- 所有方法为 `static`
- 无实例字段

### AC10: 零外部依赖

**GIVEN** cartisan-core 模块
**THEN** `Assertions` 类仅依赖：
- JDK 标准库（`java.util.Optional`, `java.lang.IllegalStateException`）
- cartisan-core.exception 模块（`DomainException`, `CodeMessage`）

---

## 约束

### 架构约束

| 约束 | 说明 |
|------|------|
| 零 Spring 依赖 | cartisan-core 是纯领域层，不能依赖 Spring |
| 零第三方库依赖 | 仅依赖 JDK 和本模块的 exception 包 |
| 方法可见性 | 所有公开方法为 public static |

### 质量约束

| 指标 | 要求 |
|------|------|
| 单元测试覆盖率 | ≥ 80% |
| ArchUnit 验证 | 通过（零外部依赖） |

---

## 使用场景

### 场景 1：聚合根前置条件校验

```java
public class Order extends AbstractAggregateRoot {
    public void cancel() {
        Assertions.require(this.status != OrderStatus.SHIPPED,
            OrderError.CANNOT_CANCEL_SHIPPED);
        this.status = OrderStatus.CANCELLED;
    }
}
```

### 场景 2：后置条件校验（内部不变量）

```java
public void addItem(OrderItem item) {
    Assertions.require(item != null, OrderError.ITEM_REQUIRED);
    this.items.add(item);
    Assertions.ensure(this.items.contains(item),
        "item should be present after add");
}
```

### 场景 3：应用服务加载聚合根（标准 404）

```java
public OrderDto getOrder(Long id) {
    Order order = Assertions.requirePresent(
        orderRepository.findById(id));
    return OrderDto.from(order);
}
```

### 场景 4：应用服务加载聚合根（区分资源类型）

```java
public OrderDto getUserOrder(Long userId, Long orderId) {
    User user = Assertions.requirePresent(
        userRepository.findById(userId),
        UserError.USER_NOT_FOUND);

    Order order = Assertions.requirePresent(
        orderRepository.findById(orderId),
        OrderError.ORDER_NOT_FOUND);

    Assertions.require(order.belongsTo(user),
        OrderError.ORDER_NOT_BELONG_TO_USER);

    return OrderDto.from(order);
}
```

---

## 技术要点

1. **异常类型语义**
   - `require` → `DomainException`：调用者责任 = 业务规则违反 = 4xx
   - `ensure` → `IllegalStateException`：实现者责任 = 代码 bug = 500

2. **API 约束即架构引导**
   - `requirePresent` 仅接受 `Optional<T>`，推动代码库使用 Optional 表达缺失语义
   - 不提供 `@Nullable T` 重载，避免类型系统语义模糊

3. **快捷方式原则**
   - `requirePresent(Optional<T>)` 快捷版覆盖 80% 场景
   - 完整版保留给需要区分资源类型的少数场景
