# F01-02: cartisan-core — domain 基础类型（接口设计）

## 元数据

| 属性 | 值 |
|------|-----|
| Epic | Epic 01: 项目骨架 + Core + Test |
| Feature | F01-02: cartisan-core — domain 基础类型 |
| 文档版本 | v0.1.0 |
| 日期 | 2026-03-13 |
| 作者 | Claude |
| 状态 | Phase 2: 接口设计 |
| 前置文档 | [01_requirement.md](./01_requirement.md) |

---

## 1. 包结构

```
com.cartisan.core.domain
├── AggregateRoot.java              (标记接口)
├── AbstractAggregateRoot.java      (事件暂存器)
├── Entity.java                     (实体接口)
├── ValueObject.java                (值对象接口)
├── Identity.java                   (标识符接口)
├── DomainEvent.java                (领域事件基类)
└── package-info.java               (包说明)
```

---

## 2. 接口定义

### 2.1 AggregateRoot（聚合根标记接口）

```java
package com.cartisan.core.domain;

/**
 * 聚合根标记接口
 *
 * <p>聚合根是 DDD 中的核心概念，代表一个一致性边界。
 * 只有聚合根才能被外部直接访问，聚合内部的实体必须通过聚合根来操作。</p>
 *
 * <p>本接口仅作为标记使用，实际功能由 {@link AbstractAggregateRoot} 提供。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class Order extends AbstractAggregateRoot<Order> implements AggregateRoot {
 *     // 订单实现
 * }
 * }</pre>
 *
 * @see AbstractAggregateRoot
 * @since 0.1.0
 */
public interface AggregateRoot {
}
```

---

### 2.2 AbstractAggregateRoot<T>（聚合根抽象基类）

```java
package com.cartisan.core.domain;

import java.util.*;

/**
 * 聚合根抽象基类，提供领域事件暂存能力
 *
 * <p>注意：本类只负责事件的暂存，不负责发布。
 * 事件发布由 cartisan-event 和 cartisan-data-jpa 模块接力处理。</p>
 *
 * <h3>线程安全：</h3>
 * <p>本类非线程安全。聚合根的修改应该在单线程内完成。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class Order extends AbstractAggregateRoot<Order> implements AggregateRoot {
 *     public void ship() {
 *         // 领域行为
 *         registerEvent(new OrderShippedEvent(this.id.toString()));
 *     }
 * }
 * }</pre>
 *
 * @param <T> 聚合根自身类型
 * @since 0.1.0
 */
public abstract class AbstractAggregateRoot<T extends AggregateRoot> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 注册领域事件
     *
     * <p>在领域行为中调用此方法，将事件添加到待发布列表。</p>
     *
     * @param event 要注册的领域事件，不能为 null
     * @throws NullPointerException 如果 event 为 null
     */
    protected void registerEvent(DomainEvent event) {
        Objects.requireNonNull(event, "domain event must not be null");
        domainEvents.add(event);
    }

    /**
     * 获取待发布的事件列表
     *
     * <p>返回不可修改的列表，防止外部直接修改事件列表。</p>
     *
     * @return 不可修改的事件列表
     */
    @SuppressWarnings("unchecked")
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * 清空已发布的事件
     *
     * <p>事件发布后调用此方法，清空事件列表。</p>
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
```

---

### 2.3 Entity<T, ID>（实体接口）

```java
package com.cartisan.core.domain;

import java.util.Objects;

/**
 * 实体接口
 *
 * <p>实体是具有唯一标识的领域对象，通过 ID 判断相等性。</p>
 *
 * <h3>类型参数：</h3>
 * <ul>
 *   <li>{@code T} - 实体自身类型</li>
 *   <li>{@code ID} - 标识符类型（Long、String、Identity&lt;?&gt; 等）</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class User implements Entity<User, UserId> {
 *     private UserId id;
 *
 *     public UserId getId() {
 *         return id;
 *     }
 * }
 * }</pre>
 *
 * @param <T> 实体自身类型
 * @param <ID> 标识符类型
 * @since 0.1.0
 */
public interface Entity<T extends Entity<T, ID>, ID> {

    /**
     * 获取实体 ID
     *
     * @return 实体 ID，可能为 null（新建未保存的实体）
     */
    ID getId();

    /**
     * 判断是否为同一实体（基于 ID 比较）
     *
     * <p>如果两个实体的 ID 相等，则认为是同一实体。</p>
     * <p>如果任一实体的 ID 为 null，则返回 false。</p>
     *
     * @param other 要比较的实体
     * @return 如果是同一实体返回 true，否则返回 false
     */
    @SuppressWarnings("unchecked")
    default boolean sameIdentityAs(T other) {
        if (other == null) {
            return false;
        }
        return Objects.equals(getId(), other.getId());
    }
}
```

---

### 2.4 ValueObject<T>（值对象接口）

```java
package com.cartisan.core.domain;

/**
 * 值对象接口
 *
 * <p>值对象通过其属性值来判断相等性，没有唯一标识。</p>
 *
 * <h3>推荐使用 Java Record 实现：</h3>
 * <pre>{@code
 * public record Email(String value) implements ValueObject<Email> {
 *     public Email {
 *         Assertions.requireNotBlank(value, "email must not be blank");
 *     }
 * }
 * }</pre>
 *
 * <h3>覆写 sameValueAs 的场景：</h3>
 * <p>某些业务场景需要"部分字段比较"的领域语义时，可以覆写此方法：</p>
 * <pre>{@code
 * public record Money(BigDecimal amount, Currency currency) implements ValueObject<Money> {
 *     // 只比较金额，忽略货币类型（用于同币种场景）
 *     public boolean sameValueAs(Money other) {
 *         return other != null && this.amount.equals(other.amount);
 *     }
 * }
 * }</pre>
 *
 * @param <T> 值对象自身类型
 * @since 0.1.0
 */
public interface ValueObject<T extends ValueObject<T>> {

    /**
     * 判断是否为相同值
     *
     * <p>默认实现委托给 {@code equals()} 方法。
     * 子类可以覆写此方法以支持"部分字段比较"的领域语义。</p>
     *
     * @param other 要比较的值对象
     * @return 如果值相同返回 true，否则返回 false
     */
    @SuppressWarnings("unchecked")
    default boolean sameValueAs(T other) {
        return this.equals(other);
    }
}
```

---

### 2.5 Identity<T>（标识符接口）

```java
package com.cartisan.core.domain;

/**
 * 类型安全的标识符接口
 *
 * <p>业务项目使用 Record 实现，编译器保证不同 ID 类型不会混用。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 定义类型安全的 ID
 * public record UserId(Long value) implements Identity<Long> {
 *     public UserId {
 *         Objects.requireNonNull(value, "userId must not be null");
 *     }
 * }
 *
 * public record OrderId(String value) implements Identity<String> {
 *     public OrderId {
 *         Objects.requireNonNull(value, "orderId must not be blank");
 *     }
 * }
 *
 * // 编译器保证类型安全
 * UserId userId = new UserId(1L);
 * OrderId orderId = new OrderId("ORDER-001");
 * // userId = orderId; // 编译错误！
 * }</pre>
 *
 * @param <T> 标识符值的类型
 * @since 0.1.0
 */
public interface Identity<T> {

    /**
     * 获取标识符值
     *
     * @return 标识符值，不应为 null
     */
    T value();
}
```

---

### 2.6 DomainEvent（领域事件基类）

```java
package com.cartisan.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 领域事件基类
 *
 * <p>eventId 和 occurredAt 自动生成，aggregateId 由子类提供。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class OrderShippedEvent extends DomainEvent {
 *     private final String orderId;
 *     private final String shippingAddress;
 *
 *     public OrderShippedEvent(String orderId, String shippingAddress) {
 *         super(orderId);
 *         this.orderId = orderId;
 *         this.shippingAddress = shippingAddress;
 *     }
 * }
 * }</pre>
 *
 * <h3>事件命名约定：</h3>
 * <p>事件名称使用过去式，表示"已经发生的事情"。</p>
 * <ul>
 *   <li>✅ OrderShippedEvent, PaymentCompletedEvent</li>
 *   <li>❌ OrderShippingEvent, PaymentCompletingEvent</li>
 * </ul>
 *
 * @since 0.1.0
 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant occurredAt;
    private final String aggregateId;

    /**
     * 创建领域事件
     *
     * @param aggregateId 聚合根 ID（必填，String 类型适配各种 ID 类型）
     * @throws NullPointerException 如果 aggregateId 为 null
     */
    protected DomainEvent(String aggregateId) {
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
    }

    /**
     * 获取事件 ID
     *
     * @return 事件 ID（UUID 字符串）
     */
    public String eventId() {
        return eventId;
    }

    /**
     * 获取事件发生时间
     *
     * @return 事件发生时间（UTC 时间戳）
     */
    public Instant occurredAt() {
        return occurredAt;
    }

    /**
     * 获取聚合根 ID
     *
     * @return 聚合根 ID
     */
    public String aggregateId() {
        return aggregateId;
    }

    /**
     * 获取事件类型名称
     *
     * <p>默认返回类名，子类可以覆写以提供自定义名称。</p>
     *
     * @return 事件类型名称
     */
    public String eventType() {
        return this.getClass().getSimpleName();
    }
}
```

---

### 2.7 package-info.java（包说明）

```java
/**
 * DDD 领域模型基础类型
 *
 * <p>本包提供领域驱动设计（DDD）的核心基础类型，包括：</p>
 * <ul>
 *   <li>{@link com.cartisan.core.domain.AggregateRoot} - 聚合根标记接口</li>
 *   <li>{@link com.cartisan.core.domain.AbstractAggregateRoot} - 聚合根抽象基类</li>
 *   <li>{@link com.cartisan.core.domain.Entity} - 实体接口</li>
 *   <li>{@link com.cartisan.core.domain.ValueObject} - 值对象接口</li>
 *   <li>{@link com.cartisan.core.domain.Identity} - 标识符接口</li>
 *   <li>{@link com.cartisan.core.domain.DomainEvent} - 领域事件基类</li>
 * </ul>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>零外部依赖 - 仅使用 JDK 标准库</li>
 *   <li>类型安全 - 通过泛型约束防止类型误用</li>
 *   <li>开箱即用 - 默认实现覆盖大部分场景</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.core.domain;
```

---

## 3. 使用示例

### 3.1 完整的聚合根示例

```java
// ============= ID 定义 =============
public record OrderId(String value) implements Identity<String> {
    public OrderId {
        Objects.requireNonNull(value, "orderId must not be null");
    }
}

// ============= 实体示例 =============
public class OrderItem implements Entity<OrderItem, Long> {
    private Long id;
    private String productId;
    private int quantity;

    public OrderItem(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}

// ============= 值对象示例 =============
public record ShippingAddress(String street, String city, String zipCode)
        implements ValueObject<ShippingAddress> {
    public ShippingAddress {
        Objects.requireNonNull(street, "street must not be null");
        Objects.requireNonNull(city, "city must not be null");
        Objects.requireNonNull(zipCode, "zipCode must not be null");
    }
}

// ============= 领域事件示例 =============
public class OrderCreatedEvent extends DomainEvent {
    private final String customerId;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(String orderId, String customerId, BigDecimal totalAmount) {
        super(orderId);
        this.customerId = customerId;
        this.totalAmount = totalAmount;
    }

    public String customerId() {
        return customerId;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }
}

public class OrderShippedEvent extends DomainEvent {
    private final String shippingAddress;

    public OrderShippedEvent(String orderId, String shippingAddress) {
        super(orderId);
        this.shippingAddress = shippingAddress;
    }

    public String shippingAddress() {
        return shippingAddress;
    }
}

// ============= 聚合根示例 =============
public class Order extends AbstractAggregateRoot<Order> implements AggregateRoot {

    private OrderId id;
    private String customerId;
    private OrderStatus status;
    private List<OrderItem> items;
    private ShippingAddress shippingAddress;

    public Order(String customerId, List<OrderItem> items) {
        this.id = new OrderId(UUID.randomUUID().toString());
        this.customerId = customerId;
        this.status = OrderStatus.PENDING;
        this.items = new ArrayList<>(items);

        // 发布领域事件
        BigDecimal totalAmount = calculateTotalAmount();
        registerEvent(new OrderCreatedEvent(id.value(), customerId, totalAmount));
    }

    public void ship(ShippingAddress address) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be shipped");
        }

        this.shippingAddress = address;
        this.status = OrderStatus.SHIPPED;

        // 发布领域事件
        registerEvent(new OrderShippedEvent(id.value(), address.toString()));
    }

    private BigDecimal calculateTotalAmount() {
        return items.stream()
            .map(item -> new BigDecimal(item.quantity() * 100)) // 简化计算
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public OrderId getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }
}

enum OrderStatus {
    PENDING, SHIPPED, DELIVERED, CANCELLED
}
```

---

## 4. 类型关系图

```
┌─────────────────────────────────────────────────────────────────┐
│                         com.cartisan.core.domain                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐       ┌─────────────────────────────┐    │
│  │  <<interface>>  │       │  <<abstract>>               │    │
│  │  AggregateRoot  │       │  AbstractAggregateRoot<T>   │    │
│  └────────┬────────┘       │  ---------------------------│    │
│           │                │  - domainEvents: List        │    │
│           │                │  + registerEvent(event)      │    │
│           │ implements     │  + getDomainEvents(): List   │    │
│           │                │  + clearDomainEvents()       │    │
│           ▼                └─────────────────────────────┘    │
│  ┌─────────────────────────────────────────────┐              │
│  │                 Order                       │              │
│  │  -------------------------------------------│              │
│  │  - id: OrderId                              │              │
│  │  - status: OrderStatus                      │              │
│  │  + ship(address): void                      │              │
│  └─────────────────────────────────────────────┘              │
│                                                                 │
│  ┌─────────────────┐       ┌─────────────────┐                │
│  │  <<interface>>  │       │  <<interface>>  │                │
│  │  Entity<T,ID>   │       │  ValueObject<T> │                │
│  │  -------------- │       │  -------------- │                │
│  │  + getId(): ID  │       │  + sameValueAs()│                │
│  │  + sameIdentityAs()│    │                 │                │
│  └─────────────────┘       └─────────────────┘                │
│           ▲                          ▲                        │
│           │                          │                        │
│  ┌────────┴────────┐       ┌─────────┴─────────┐             │
│  │   OrderItem     │       │  ShippingAddress  │             │
│  │  (Record 可选)   │       │     (Record)      │             │
│  └─────────────────┘       └───────────────────┘             │
│                                                                 │
│  ┌─────────────────┐       ┌─────────────────────────────┐    │
│  │  <<interface>>  │       │  <<abstract>>               │    │
│  │  Identity<T>    │       │  DomainEvent                │    │
│  │  -------------- │       │  ---------------------------│    │
│  │  + value(): T   │       │  - eventId: String          │    │
│  └─────────────────┘       │  - occurredAt: Instant       │    │
│           ▲                │  - aggregateId: String       │    │
│           │                └─────────────────────────────┘    │
│  ┌────────┴────────┐                    ▲                    │
│  │  OrderId        │                    │                    │
│  │  (Record)       │          ┌─────────┴─────────┐         │
│  └─────────────────┘          │ OrderCreatedEvent │         │
│                               │ OrderShippedEvent │         │
│                               └───────────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. 命名约定

| 类型 | 命名约定 | 示例 |
|------|----------|------|
| AggregateRoot | 名词（领域概念） | Order, User, Invoice |
| Entity | 名词（领域概念） | OrderItem, LineItem |
| ValueObject | 名词（属性描述） | Email, Money, Address |
| Identity | 名词 + Id | UserId, OrderId, ProductId |
| DomainEvent | 名词 + 过去分词 + Event | OrderCreatedEvent, PaymentCompletedEvent |

---

## 6. 相关文档

- [F01-02 需求文档](./01_requirement.md)
- [F01-02 实现方案](./03_implementation.md)
- [00_epic_backlog.md](../00_epic_backlog.md)

---

## 7. 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v0.1.0 | 2026-03-13 | 初始版本 | Claude |
