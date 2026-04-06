# 应用事件架构设计方案

> **版本**：v1.0
> **日期**：2026-04-06
> **作者**：Claude
> **状态**：待评审

---

## 一、背景与目标

### 1.1 背景

当前 cartisan-boot 框架使用**领域事件**机制，存在以下问题：

1. **技术复杂度高**：领域层需要维护事件注册、发布逻辑，违反了"领域层零外部依赖"原则
2. **过度设计**：上下文内部事件需求不大，真正的跨上下文事件才需要事件机制
3. **职责不清**：领域事件混杂在聚合根中，增加了领域模型的复杂性

### 1.2 设计目标

**核心目标**：
- ✅ 删除领域事件机制（`DomainEvent`、`AbstractAggregateRoot`）
- ✅ 引入应用事件机制（`ApplicationEvent`、`ApplicationEventPublisher`）
- ✅ 事件从应用服务发布，监听器放在 `endpoints/listener` 层
- ✅ 支持未来扩展到消息队列（RabbitMQ、Kafka）

**设计原则**：
- **简洁性**：事件机制简单易用，不增加技术复杂度
- **扩展性**：支持进程内（Spring）和跨进程（MQ）两种发布方式
- **一致性**：监听器统一放在 `endpoints/listener`，与MQ Listener一致

---

## 二、总体架构

### 2.1 架构分层

```
┌─────────────────────────────────────┐
│       北向接口（Driving Side）        │
│  REST API | GraphQL | MQ Listener    │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│      应用层（Application Layer）      │
│  AppService 发布 ApplicationEvent    │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│   应用事件机制（ApplicationEvent）    │
│  - 注解标记：@PublishTo              │
│  - 复合发布器：CompositePublisher    │
│  - 多实现：Spring / RabbitMQ / Kafka  │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│      监听器（endpoints/listener）    │
│  @TransactionalEventListener          │
│  - 进程内：Spring事件                 │
│  - 跨进程：RabbitMQ/Kafka（未来）      │
└─────────────────────────────────────┘
```

### 2.2 模块依赖关系

```
cartisan-core (零依赖，只有AggregateRoot接口)
    ↑
cartisan-event (依赖Spring)
    ↑
cartisan-web / cartisan-data-jpa (依赖event)
    ↑
业务项目
```

---

## 三、核心组件设计

### 3.1 应用事件接口

**位置**：`cartisan-event/src/main/java/com/cartisan/event/ApplicationEvent.java`

```java
/**
 * 应用事件基类。
 *
 * <p>应用事件用于跨上下文、跨系统的异步通信。</p>
 *
 * <p>当前实现：基于Spring事件机制（进程内）</p>
 * <p>未来扩展：支持RabbitMQ、Kafka等消息队列（跨进程）</p>
 *
 * <h3>事件元数据</h3>
 * <ul>
 *   <li>{@code eventId} - 事件的唯一标识符</li>
 *   <li>{@code occurredAt} - 事件发生时间</li>
 *   <li>{@code eventType} - 事件类型名称，用于路由</li>
 * </ul>
 *
 * <h3>实现建议</h3>
 * <p>推荐使用 Java Record 来实现事件类，确保不可变性和可序列化性。</p>
 */
public interface ApplicationEvent {

    /**
     * 获取事件唯一标识符。
     * @return 事件ID，格式为UUID字符串
     */
    String eventId();

    /**
     * 获取事件发生时间。
     * @return 事件发生时间（UTC）
     */
    Instant occurredAt();

    /**
     * 获取事件类型名称。
     * <p>用于消息队列的路由key或topic名称</p>
     * @return 事件类型，如 "order.created"
     */
    String eventType();
}
```

**示例实现**（推荐使用Record）：

```java
/**
 * 订单创建事件。
 */
@PublishTo({"spring", "rabbitmq"})
public record OrderCreatedEvent(
    String eventId,
    Instant occurredAt,
    Long orderId,
    String customerId,
    BigDecimal totalAmount
) implements ApplicationEvent {

    public OrderCreatedEvent(Long orderId, String customerId, BigDecimal totalAmount) {
        this(UUID.randomUUID().toString(), Instant.now(), orderId, customerId, totalAmount);
    }

    @Override
    public String eventType() {
        return "order.created";
    }
}
```

### 3.2 发布器接口

**位置**：`cartisan-event/src/main/java/com/cartisan/event/ApplicationEventPublisher.java`

```java
/**
 * 应用事件发布器接口。
 *
 * <p>定义了发布应用事件的契约，实现类负责将事件发布到具体的
 * 事件总线（如Spring Events、RabbitMQ、Kafka）。</p>
 */
public interface ApplicationEventPublisher {

    /**
     * 获取发布器类型标识。
     * @return 类型标识，如 "spring"、"rabbitmq"、"kafka"
     */
    String getType();

    /**
     * 发布应用事件。
     *
     * @param event 要发布的领域事件，不能为 null
     * @throws NullPointerException 如果 event 为 null
     */
    void publishApplicationEvent(ApplicationEvent event);
}
```

### 3.3 Spring实现

**位置**：`cartisan-event/src/main/java/com/cartisan/event/impl/SpringApplicationEventPublisher.java`

```java
/**
 * 基于Spring事件机制的应用事件发布器。
 */
@Component
@ConditionalOnProperty(
    name = "cartisan.event.publisher.spring.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SpringApplicationEventPublisher implements ApplicationEventPublisher {

    private final org.springframework.context.ApplicationEventPublisher publisher;

    public SpringApplicationEventPublisher(
            org.springframework.context.ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher cannot be null");
    }

    @Override
    public String getType() {
        return "spring";
    }

    @Override
    public void publishApplicationEvent(ApplicationEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        publisher.publishEvent(event);
    }
}
```

### 3.4 复合发布器（自动路由）

**位置**：`cartisan-event/src/main/java/com/cartisan/event/CompositeApplicationEventPublisher.java`

```java
/**
 * 复合应用事件发布器。
 *
 * <p>根据事件的 {@link PublishTo} 注解自动路由到对应的发布器。</p>
 */
@Component
public class CompositeApplicationEventPublisher implements ApplicationEventPublisher {

    private final Map<String, ApplicationEventPublisher> publishers;

    public CompositeApplicationEventPublisher(
            List<ApplicationEventPublisher> publisherList) {
        this.publishers = publisherList.stream()
            .collect(Collectors.toMap(
                ApplicationEventPublisher::getType,
                Function.identity()
            ));
    }

    @Override
    public String getType() {
        return "composite";
    }

    @Override
    public void publishApplicationEvent(ApplicationEvent event) {
        Objects.requireNonNull(event, "event cannot be null");

        PublishTo annotation = event.getClass().getAnnotation(PublishTo.class);
        if (annotation == null) {
            publishTo("spring", event);
            return;
        }

        for (String type : annotation.value()) {
            publishTo(type, event);
        }
    }

    private void publishTo(String type, ApplicationEvent event) {
        ApplicationEventPublisher publisher = publishers.get(type);
        if (publisher == null) {
            throw new IllegalArgumentException(
                "No publisher found for type: " + type +
                ". Available types: " + publishers.keySet()
            );
        }
        publisher.publishApplicationEvent(event);
    }
}
```

### 3.5 发布方式注解

**位置**：`cartisan-event/src/main/java/com/cartisan/event/PublishTo.java`

```java
/**
 * 标记事件发布方式。
 *
 * <p>支持同时发布到多个目标（如Spring + RabbitMQ）。</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishTo {
    /**
     * 发布目标类型。
     * @return 类型数组，如 {"spring", "rabbitmq"}
     */
    String[] value();
}
```

---

## 四、事件监听器设计

### 4.1 监听器位置

**位置**：`endpoints/listener/`（与MQ Listener统一）

**包结构**：
```
endpoints/
├── controller/              # REST API
│   └── OrderController.java
├── listener/                # 事件监听器
│   ├── OrderCreatedEventListener.java       # Spring应用事件
│   ├── OrderPaidEventListener.java
│   ├── UserCreatedListener.java             # RabbitMQ消息
│   └── PaymentCallbackListener.java         # 支付回调
└── api/                     # 外部API
    └── OrderApiV1Controller.java
```

### 4.2 监听器示例

```java
/**
 * 订单创建事件监听器。
 *
 * <p>监听应用事件，在事务提交后执行业务逻辑。</p>
 */
@Component
public class OrderCreatedEventListener {

    private final InventoryService inventoryService;

    /**
     * 处理订单创建事件。
     *
     * <p>事务提交后执行，确保订单已持久化。</p>
     * <p>在新事务中执行，失败不影响订单事务。</p>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("收到订单创建事件: orderId={}, eventId={}",
            event.orderId(), event.eventId());

        inventoryService.decreaseStock(event.orderId());
    }
}
```

---

## 五、聚合根改造

### 5.1 删除AbstractAggregateRoot

**删除前**：
```java
public abstract class AbstractAggregateRoot<T, ID> implements AggregateRoot<T, ID> {
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) { ... }
    public List<DomainEvent> getDomainEvents() { ... }
    public void clearDomainEvents() { ... }
    public abstract ID getId();
}
```

**删除后**：
```java
@Entity
@Table(name = "orders")
@Aggregate
public class Order implements AggregateRoot<Order, Long> {

    @Id
    @Getter
    private Long id;

    @Getter
    private OrderStatus status;

    // 不再有 registerEvent() / getDomainEvents() / clearDomainEvents()

    public void ship() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be shipped");
        }
        this.status = OrderStatus.SHIPPED;
        // 不再发布事件
    }
}
```

### 5.2 更新AggregateRoot接口

**位置**：`cartisan-core/src/main/java/com/cartisan/core/domain/AggregateRoot.java`

```java
/**
 * 聚合根标记接口。
 *
 * <p>所有聚合根必须实现此接口，用于类型约束和架构验证。</p>
 *
 * @param <T> 聚合根类型
 * @param <ID> 标识符类型
 */
public interface AggregateRoot<T, ID> {
}
```

---

## 六、需要删除和新增的内容

### 6.1 删除的内容

| 模块 | 文件/类 | 说明 |
|------|--------|------|
| **cartisan-core** | `AbstractAggregateRoot.java` | 包含事件管理逻辑 |
| **cartisan-core** | `DomainEvent.java` | 领域事件基类 |
| **cartisan-event** | `DomainEventPublisher.java` | 领域事件发布器 |
| **cartisan-event** | `SpringDomainEventPublisher.java` | 领域事件发布器实现 |
| **cartisan-data-jpa** | `BaseRepositoryImpl.save()` 中的事件发布逻辑 | 删除自动发布 |

### 6.2 新增的内容

| 模块 | 文件/类 | 说明 |
|------|--------|------|
| **cartisan-event** | `ApplicationEvent.java` | 应用事件接口 |
| **cartisan-event** | `ApplicationEventPublisher.java` | 发布器接口 |
| **cartisan-event** | `CompositeApplicationEventPublisher.java` | 复合发布器 |
| **cartisan-event** | `SpringApplicationEventPublisher.java` | Spring实现 |
| **cartisan-event** | `@PublishTo` 注解 | 发布方式标记 |
| **cartisan-core** | `AggregateRoot<T, ID>` | 更新泛型参数 |

---

## 七、迁移策略

### 7.1 框架改造步骤

1. **cartisan-core模块**
   - 删除 `AbstractAggregateRoot.java`
   - 删除 `DomainEvent.java`
   - 更新 `AggregateRoot.java` 泛型参数为 `<T, ID>`

2. **cartisan-event模块**
   - 删除 `DomainEventPublisher.java`
   - 删除 `SpringDomainEventPublisher.java`
   - 新增 `ApplicationEvent.java`
   - 新增 `ApplicationEventPublisher.java`
   - 新增 `CompositeApplicationEventPublisher.java`
   - 新增 `SpringApplicationEventPublisher.java`
   - 新增 `@PublishTo` 注解
   - 更新 `CartisanEventAutoConfiguration.java`

3. **cartisan-data-jpa模块**
   - 删除 `BaseRepositoryImpl.save()` 中的事件发布逻辑
   - 删除 `DomainEventPublisherHolder.java`

### 7.2 业务项目迁移步骤

**步骤1：聚合根改造**
```java
// 旧代码
public class Order extends AbstractAggregateRoot<Order> {
    registerEvent(new OrderCreatedEvent(...));
}

// 新代码
public class Order implements AggregateRoot<Order, Long> {
    // 删除 registerEvent() 调用
}
```

**步骤2：领域事件 → 应用事件**
```java
// 旧代码（领域事件）
public class OrderCreatedEvent extends DomainEvent {
    private final String customerId;
    public OrderCreatedEvent(String orderId, String customerId) {
        super(orderId);
        this.customerId = customerId;
    }
}

// 新代码（应用事件）
@PublishTo({"spring", "rabbitmq"})
public record OrderCreatedEvent(
    String eventId,
    Instant occurredAt,
    Long orderId,
    String customerId
) implements ApplicationEvent {
    public OrderCreatedEvent(Long orderId, String customerId) {
        this(UUID.randomUUID().toString(), Instant.now(), orderId, customerId);
    }
}
```

**步骤3：应用服务发布事件**
```java
@Service
public class OrderManagementAppService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createOrder(CreateOrderCommand command) {
        Order order = new Order(...);
        orderRepository.save(order);

        // 手动发布应用事件
        eventPublisher.publishApplicationEvent(
            new OrderCreatedEvent(order.getId(), command.customerId())
        );

        return order.getId();
    }
}
```

**步骤4：监听器迁移到endpoints/listener**
```java
// 旧代码：任意包
@Component
public class OrderEventHandler {
    @EventListener
    public void handle(OrderCreatedEvent event) { ... }
}

// 新代码：endpoints/listener包
@Component
public class OrderCreatedEventListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) { ... }
}
```

---

## 八、错误处理

### 8.1 发布失败处理

**原则**：发布失败不抛异常，避免影响主业务

```java
@Component
public class SpringApplicationEventPublisher implements ApplicationEventPublisher {

    @Override
    public void publishApplicationEvent(ApplicationEvent event) {
        try {
            publisher.publishEvent(event);
        } catch (Exception e) {
            log.error("发布Spring事件失败: eventId={}, eventType={}",
                event.eventId(), event.eventType(), e);
        }
    }
}
```

### 8.2 监听器异常处理

**原则**：监听器在新事务中执行，失败不影响发布者事务

```java
@Component
public class OrderCreatedEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            inventoryService.decreaseStock(event.orderId());
        } catch (Exception e) {
            log.error("处理订单创建事件失败: eventId={}, orderId={}",
                event.eventId(), event.orderId(), e);
            // 考虑重试或死信队列
        }
    }
}
```

---

## 九、ArchUnit规则更新

### 9.1 需要删除的规则

```java
// ❌ 删除
@ArchTest
static final ArchRule aggregates_should_extend_AbstractAggregateRoot = classes()
    .that().areAnnotatedWith(Aggregate.class)
    .should().beSubclassesOf(AbstractAggregateRoot.class);
```

### 9.2 需要新增的规则

```java
// ✅ 新增：聚合根必须实现AggregateRoot接口
@ArchTest
static final ArchRule aggregates_should_implement_AggregateRoot = classes()
    .that().areAnnotatedWith(Aggregate.class)
    .should().implementInterface(AggregateRoot.class);

// ✅ 新增：应用事件必须是Record
@ArchTest
static final ArchRule application_events_should_be_record = classes()
    .that().areAssignableTo(ApplicationEvent.class)
    .should().beRecords();
```

---

## 十、测试策略

### 10.1 单元测试

**复合发布器测试**：
```java
@ExtendWith(MockitoExtension.class)
class CompositeApplicationEventPublisherTest {

    @Test
    void should_publish_to_spring_when_annotation_has_single_type() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(1L, "customer", BigDecimal.valueOf(100));

        // When
        compositePublisher.publishApplicationEvent(event);

        // Then
        verify(springPublisher).publishApplicationEvent(event);
    }
}
```

### 10.2 集成测试

**应用服务发布事件测试**：
```java
@SpringBootTest
@Transactional
class OrderManagementAppServiceTest {

    @Autowired
    private OrderManagementAppService orderService;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void should_publish_event_when_create_order() {
        // Given
        CreateOrderCommand command = new CreateOrderCommand(...);

        // When
        Long orderId = orderService.createOrder(command);

        // Then - 验证事件是否被监听器处理
        verify(inventoryService).decreaseStock(orderId);
    }
}
```

---

## 十一、向后兼容性

### 11.1 破坏性变更

| 变更类型 | 影响范围 | 是否需要修改 |
|---------|---------|------------|
| 聚合根继承链 | 所有聚合根类 | ✅ 需要 |
| 领域事件类 | 所有领域事件 | ✅ 需要 |
| 事件发布方式 | 应用服务 | ✅ 需要 |
| 监听器位置 | 事件监听器 | ✅ 需要 |

### 11.2 迁移路径

**阶段1：框架改造**
- cartisan-core、cartisan-event、cartisan-data-jpa模块改造

**阶段2：业务项目迁移**
- 聚合根类修改
- 领域事件改造为应用事件
- 应用服务手动发布事件
- 监听器移到endpoints/listener

**阶段3：测试验证**
- ArchUnit规则验证
- 集成测试验证
- 回归测试验证

---

## 十二、未来扩展

### 12.1 支持RabbitMQ

```java
@Component
@ConditionalOnProperty(name = "cartisan.event.publisher.rabbitmq.enabled")
public class RabbitMQApplicationEventPublisher implements ApplicationEventPublisher {

    @Override
    public String getType() {
        return "rabbitmq";
    }

    @Override
    public void publishApplicationEvent(ApplicationEvent event) {
        rabbitTemplate.convertAndSend(event.eventType(), event);
    }
}
```

### 12.2 支持Kafka

```java
@Component
@ConditionalOnProperty(name = "cartisan.event.publisher.kafka.enabled")
public class KafkaApplicationEventPublisher implements ApplicationEventPublisher {

    @Override
    public String getType() {
        return "kafka";
    }

    @Override
    public void publishApplicationEvent(ApplicationEvent event) {
        kafkaTemplate.send(event.eventType(), event);
    }
}
```

---

## 十三、总结

### 13.1 核心变更

| 维度 | 旧设计（领域事件） | 新设计（应用事件） |
|------|-----------------|-----------------|
| **事件位置** | 领域层 | 应用层 |
| **事件基类** | `DomainEvent` | `ApplicationEvent` 接口 |
| **事件实现** | 普通类 | Record（推荐） |
| **发布位置** | 聚合根 | 应用服务 |
| **发布时机** | Repository.save()自动 | 手动调用 |
| **事务边界** | 事务内 | 事务提交后 |
| **监听器位置** | 任意包 | `endpoints/listener` |

### 13.2 主要优势

1. **简化设计**：删除领域层的复杂性，事件机制移到应用层
2. **上下文独立**：每个上下文维护自己的事件发布
3. **扩展性强**：支持进程内（Spring）和跨进程（MQ）两种发布方式
4. **架构清晰**：监听器统一放在 `endpoints/listener`

---

**文档结束**
