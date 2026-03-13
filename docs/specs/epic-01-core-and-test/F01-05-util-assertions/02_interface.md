# Feature: F01-05 — util 工具类 — 接口契约

> 版本：v0.1 | 日期：2026-03-13
> 依赖：F01-03 (exception)

---

## 接口定义

### 1. 类：Assertions

**包路径：** `com.cartisan.core.util.Assertions`

**类型：** `final class`（不可继承，工具类模式）

**职责：** 提供 Design by Contract 断言工具，简化防御式编程

---

### 2. 方法签名（伪代码）

#### 2.1 前置条件断言

```
方法：require(condition: boolean, codeMessage: CodeMessage, args: Object...) → void

前置条件：无
后置条件：无
副作用：当 condition 为 false 时抛出 DomainException

异常：
  - 当 condition 为 false 时：抛出 DomainException(codeMessage, args)

参数说明：
  - condition: 前置条件布尔值
  - codeMessage: 错误码信息
  - args: 可变参数，用于格式化 codeMessage 中的占位符（如 {0}）

使用场景：
  - 聚合根/实体构造函数校验不变量
  - 领域方法校验前置条件
  - 应用服务校验业务规则
```

#### 2.2 后置条件断言

```
方法：ensure(condition: boolean, message: String) → void

前置条件：无
后置条件：无
副作用：当 condition 为 false 时抛出 IllegalStateException

异常：
  - 当 condition 为 false 时：抛出 IllegalStateException("Postcondition violated: " + message)

使用场景：
  - 领域方法内部状态一致性校验
  - 复杂操作后验证不变量
  - 失败表示代码 bug（非业务规则违反）
```

#### 2.3 存在性断言（快捷版）

```
方法：requirePresent<T>(optional: Optional<T>) → T

前置条件：无
后置条件：返回值非 null
副作用：当 optional 为空时抛出 DomainException

异常：
  - 当 optional.isEmpty() 时：抛出 DomainException(BaseCodeMessage.NOT_FOUND)

返回值：optional 包含的值（保证非 null）

使用场景：
  - 应用服务加载聚合根（标准 404 场景）
```

#### 2.4 存在性断言（完整版）

```
方法：requirePresent<T>(optional: Optional<T>, codeMessage: CodeMessage) → T

前置条件：无
后置条件：返回值非 null
副作用：当 optional 为空时抛出 DomainException

异常：
  - 当 optional.isEmpty() 时：抛出 DomainException(codeMessage)

返回值：optional 包含的值（保证非 null）

使用场景：
  - 应用服务加载聚合根，需要区分资源类型（订单不存在 vs 用户不存在）
```

---

## 核心流程（伪代码）

### require 实现逻辑

```
IF condition IS false THEN
    CREATE DomainException WITH codeMessage
    THROW exception
END IF
// 条件为 true 时，静默通过
```

### ensure 实现逻辑

```
IF condition IS false THEN
    CREATE IllegalStateException WITH message "Postcondition violated: " + message
    THROW exception
END IF
```

### requirePresent 实现逻辑（快捷版）

```
IF optional.isEmpty() THEN
    CREATE DomainException WITH BaseCodeMessage.NOT_FOUND
    THROW exception
ELSE
    RETURN optional.get()
END IF
```

### requirePresent 实现逻辑（完整版）

```
IF optional.isEmpty() THEN
    CREATE DomainException WITH codeMessage
    THROW exception
ELSE
    RETURN optional.get()
END IF
```

---

## 异常类型映射表

| 方法 | 异常类型 | 语义 | HTTP 映射 |
|------|---------|------|----------|
| `require` | `DomainException` | 调用者责任 = 业务规则违反 | 4xx (400/409/422 等) |
| `ensure` | `IllegalStateException` | 实现者责任 = 代码 bug | 500 |
| `requirePresent` (快捷版) | `DomainException(BaseCodeMessage.NOT_FOUND)` | 资源不存在 | 404 |
| `requirePresent` (完整版) | `DomainException(codeMessage)` | 特定资源不存在 | 4xx (由 codeMessage 决定) |

---

## 与现有代码的集成点

### 依赖关系

```
Assertions (util)
    ├──> DomainException (com.cartisan.core.exception)
    ├──> CodeMessage (com.cartisan.core.exception)
    ├──> Optional (java.util)
    └──> IllegalStateException (java.lang)
```

### 导入声明

```java
import com.cartisan.core.exception.CodeMessage;
import com.cartisan.core.exception.DomainException;
import java.util.Optional;
```

---

## 使用示例

### 示例 1：聚合根前置条件校验

```
CLASS Order EXTENDS AbstractAggregateRoot:
    FIELD status: OrderStatus
    FIELD items: List<OrderItem>

    METHOD cancel(): void
        // 前置条件：已发货订单不能取消
        Assertions.require(
            this.status != OrderStatus.SHIPPED,
            OrderError.CANNOT_CANCEL_SHIPPED
        )

        // 业务逻辑
        this.status = OrderStatus.CANCELLED
        REGISTER_EVENT(OrderCancelledEvent(this.id))
    END METHOD
END CLASS
```

### 示例 2：后置条件校验

```
CLASS Order EXTENDS AbstractAggregateRoot:
    METHOD addItem(item: OrderItem): void
        // 前置条件
        Assertions.require(item != null, OrderError.ITEM_REQUIRED)

        // 业务逻辑
        INTERNAL_addItem(item)
        recalculateTotal()

        // 后置条件：不变量验证
        Assertions.ensure(
            this.items.contains(item),
            "item should be present after add"
        )
    END METHOD
END CLASS
```

### 示例 3：应用服务加载聚合根（快捷版）

```
CLASS OrderApplicationService:
    FIELD orderRepository: OrderRepository

    METHOD getOrder(orderId: Long): OrderDto
        // 标准的"加载或 404"场景
        ORDER order = Assertions.requirePresent(
            orderRepository.findById(orderId)
        )
        RETURN OrderDto.from(order)
    END METHOD
END CLASS
```

### 示例 4：应用服务加载聚合根（完整版）

```
CLASS OrderApplicationService:
    FIELD orderRepository: OrderRepository
    FIELD userRepository: UserRepository

    METHOD getUserOrder(userId: Long, orderId: Long): OrderDto
        // 需要区分"用户不存在"和"订单不存在"
        USER user = Assertions.requirePresent(
            userRepository.findById(userId),
            UserError.USER_NOT_FOUND
        )

        ORDER order = Assertions.requirePresent(
            orderRepository.findById(orderId),
            OrderError.ORDER_NOT_FOUND
        )

        // 业务校验
        Assertions.require(
            order.belongsTo(user),
            OrderError.ORDER_NOT_BELONG_TO_USER
        )

        RETURN OrderDto.from(order)
    END METHOD
END CLASS
```

---

## 不包含的功能（Out of Scope）

| 功能 | 原因 |
|------|------|
| 对 `null` 值的断言 | 使用 `Optional.ofNullable()` 在调用点转换，保持 API 语义清晰 |
| 对集合/数组的断言（如非空、非包含 null 元素） | 不是核心功能，可在后续需求中扩展 |
| `Supplier<Optional<T>>` 懒加载重载 | `findById()` 已返回 Optional，额外包装没有实际收益 |
| 断言失败时的日志记录 | 由异常处理器统一处理，不在断言层添加副作用 |

---

## 实现约束

1. **零外部依赖**：仅依赖 JDK 和 cartisan-core.exception 模块
2. **无状态**：所有方法为静态方法，无实例字段
3. **不可实例化**：私有构造函数，防止反射创建实例
4. **final 类**：禁止继承
5. **方法可见性**：所有 public 方法为 static
