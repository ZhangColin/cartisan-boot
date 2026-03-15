# cartisan-boot 使用手册

> **版本**：v0.2 | **日期**：2026-03-15
> **基于 Epic**：Epic 01 + Epic 02 - Core + Test + Web + Data-JPA + Event

---

## 一、模块能力清单

### 1.1 cartisan-core 模块

| 能力 | 说明 |
|------|------|
| **DDD 基础类型** | 聚合根、实体、值对象、领域事件、标识符 |
| **异常体系** | 统一错误码接口 + 业务异常层次 |
| **架构注解** | DDD 分层标记注解（限界上下文、聚合、端口、适配器） |
| **断言工具** | Design by Contract 风格的前置/后置条件断言 |

### 1.2 cartisan-test 模块

| 能力 | 说明 |
|------|------|
| **ArchUnit 规则** | DDD 分层、命名规范、禁止规则的自动验证 |
| **Testcontainers** | PostgreSQL + Redis 集成测试基类 |
| **API 测试** | MockMvc 测试基类 + 断言辅助 |
| **Fixture 工具** | 随机数据生成器 + 对象构建器 |

### 1.3 cartisan-web 模块

| 能力 | 说明 |
|------|------|
| **统一响应体** | `ApiResponse<T>`、`PageResponse<T>`、`FieldError` |
| **全局异常处理** | `@ControllerAdvice` 自动捕获异常并转换为响应 |
| **请求上下文** | `RequestContext` 存储 requestId、clientIp（ThreadLocal） |
| **自动配置** | Spring Boot AutoConfiguration 零配置启用 |

### 1.4 cartisan-data-jpa 模块

| 能力 | 说明 |
|------|------|
| **BaseRepository** | 约束 T 必须是 `AggregateRoot<?>`，继承 JPA + Specification |
| **事件自动发布** | Repository save() 时自动发布领域事件 |
| **审计支持** | `@CreatedDate`、`@LastModifiedDate`、`@CreatedBy`、`@LastModifiedBy` |
| **软删除** | `@SQLRestriction` 自动过滤已删除记录 |
| **分布式 ID** | TSID 生成器（42 位时间戳 + 22 位随机数） |

### 1.5 cartisan-event 模块

| 能力 | 说明 |
|------|------|
| **事件发布器** | `DomainEventPublisher` 接口 + Spring 实现 |
| **事务监听** | 支持 `@TransactionalEventListener(phase=AFTER_COMMIT)` |
| **自动配置** | Spring Boot AutoConfiguration 零配置启用 |

---

## 二、核心概念和 API

### 2.1 DDD 基础类型（com.cartisan.core.domain）

| 接口/类 | 方法 | 说明 |
|---------|------|------|
| `AggregateRoot` | - | 聚合根标记接口 |
| `AbstractAggregateRoot<T>` | `registerEvent(event)` | 注册领域事件 |
| | `getDomainEvents()` | 获取待发布事件列表 |
| | `clearDomainEvents()` | 清空事件列表 |
| `Entity<T, ID>` | `getId()` | 获取实体 ID |
| | `sameIdentityAs(other)` | 判断是否为同一实体 |
| `ValueObject<T>` | `sameValueAs(other)` | 判断值是否相等 |
| `Identity<T>` | `value()` | 获取标识符值 |
| `DomainEvent` | `eventId()` | 事件 ID（UUID） |
| | `occurredAt()` | 发生时间 |
| | `aggregateId()` | 聚合根 ID |
| | `eventType()` | 事件类型名 |

### 2.2 异常体系（com.cartisan.core.exception）

| 类 | 说明 |
|----|------|
| `CodeMessage` | 错误码接口：`code()`, `message()`, `httpStatus()` |
| `BaseCodeMessage` | HTTP 规范错误码枚举（11 个）+ 通用业务错误码（4 个） |
| `CartisanException` | 异常基类，支持参数化消息 |
| `DomainException` | 领域层异常（业务规则违反） |
| `ApplicationException` | 应用层异常（用例/流程问题） |

### 2.3 架构注解（com.cartisan.core.stereotype）

| 注解 | 目标 | 用途 |
|------|------|------|
| `@BoundedContext` | PACKAGE | 标注限界上下文 |
| `@Aggregate` | TYPE | 标注聚合根 |
| `@DomainService` | TYPE | 标注领域服务 |
| `@Port(PortType)` | TYPE | 标注端口接口 |
| `@Adapter(PortType)` | TYPE | 标注适配器实现 |

### 2.4 断言工具（com.cartisan.core.util.Assertions）

| 方法 | 异常类型 | 用途 |
|------|---------|------|
| `require(condition, codeMessage, args)` | `DomainException` | 前置条件断言 |
| `ensure(condition, message)` | `IllegalStateException` | 后置条件断言 |
| `requirePresent(optional)` | `DomainException` | Optional 存在性断言（快捷版） |
| `requirePresent(optional, codeMessage)` | `DomainException` | Optional 存在性断言（完整版） |

### 2.5 ArchUnit 规则（com.cartisan.test.archunit）

| 类 | 规则数 | 说明 |
|----|--------|------|
| `CartesianLayeringRules` | 4 | DDD 分层规则 |
| `CartesianNamingRules` | 4 | 命名规范规则 |
| `CartesianProhibitionRules` | 3 | 禁止规则 |
| `CartesianArchRules` | 11 | 聚合全部规则 |

### 2.6 Testcontainers（com.cartisan.test.container）

| 类 | 容器 | 说明 |
|----|------|------|
| `PostgresTestContainer` | PostgreSQL 16 | `@ServiceConnection` 自动注入 |
| `RedisTestContainer` | Redis 7 | `@ServiceConnection` 自动注入 |

### 2.7 测试基类（com.cartisan.test.base）

| 类 | 继承关系 | 提供能力 |
|----|----------|----------|
| `IntegrationTestBase` | - | 容器启动 + 数据清理 |
| `ApiTestBase` | `IntegrationTestBase` | + MockMvc |

### 2.8 Fixture 工具（com.cartisan.test.fixture）

| 类 | 方法示例 | 说明 |
|----|----------|------|
| `FixtureStrings` | `randomString()`, `randomEmail()` | 字符串随机生成 |
| `FixtureNumbers` | `randomInt()`, `randomAmount()` | 数字/金额随机生成 |
| `FixtureDates` | `pastDays(7)`, `futureDays(3)` | 日期随机生成 |
| `FixtureBuilder<T>` | `of(clazz).with(name, value).build()` | 对象构建器 |

### 2.9 Web 响应体（com.cartisan.web.response）

| 类/Record | 方法/字段 | 说明 |
|-----------|----------|------|
| `ApiResponse<T>` | `code`, `message`, `data`, `requestId`, `errors` | 统一响应字段 |
| | `ok(T data)` | 成功响应（带数据） |
| | `ok()` | 成功响应（无数据） |
| | `error(CodeMessage)` | 错误响应（枚举） |
| | `error(CodeMessage, Object...)` | 错误响应（参数化） |
| | `error(int, String)` | 错误响应（自定义） |
| | `validationError(List<FieldError>)` | 校验失败响应 |
| `PageResponse<T>` | `items`, `total`, `page`, `size` | 分页响应字段 |
| `FieldError` | `field`, `message`, `errorCode` | 字段级错误 |

### 2.10 请求上下文（com.cartisan.web.context）

| 类 | 方法 | 说明 |
|----|------|------|
| `RequestContext` | `getRequestId()` | 获取请求追踪 ID（可能为 null） |
| | `getClientIp()` | 获取客户端 IP（可能为 null） |
| `RequestContextFilter` | - | 自动初始化 RequestContext（@Component） |

### 2.11 BaseRepository（com.cartisan.data.jpa.repository）

| 接口 | 约束 | 说明 |
|----|------|------|
| `BaseRepository<T, ID>` | `T extends AggregateRoot<?>` | 继承 JpaRepository + JpaSpecificationExecutor |
| | `ID extends Serializable` | ID 类型约束 |
| `BaseRepositoryImpl` | 重写 `save()` | JPA save 后自动发布领域事件 |

### 2.12 审计与软删除（com.cartisan.data.jpa.domain）

| 类 | 字段/注解 | 说明 |
|----|----------|------|
| `Auditable` | `@CreatedDate createdAt` | 创建时间（自动填充） |
| | `@LastModifiedDate lastModifiedDate` | 修改时间（自动更新） |
| | `@CreatedBy createdBy` | 创建人（需 AuditorAware） |
| | `@LastModifiedBy lastModifiedBy` | 修改人（需 AuditorAware） |
| `SoftDeletable` | `boolean deleted` | 软删除标记 |
| | `@SQLRestriction("deleted = false")` | 查询自动过滤 |

### 2.13 TSID 生成器（com.cartisan.data.jpa.id）

| 类 | 方法 | 说明 |
|----|------|------|
| `TsidGenerator` | `generate()` | 生成时间排序的全局唯一 Long ID |
| | `toInstant(long tsid)` | 从 TSID 提取生成时间 |
| | `newInstance()` | 创建默认实例（ThreadLocalRandom） |
| | `withRandom(Random)` | 测试用：指定随机数源 |

### 2.14 领域事件发布器（com.cartisan.event）

| 接口/类 | 方法 | 说明 |
|---------|------|------|
| `DomainEventPublisher` | `publish(DomainEvent)` | 发布领域事件 |
| `SpringDomainEventPublisher` | - | 委托给 Spring ApplicationEventPublisher |

---

## 三、使用示例

### 3.1 定义聚合根

```java
// ID 定义（推荐使用 Record）
public record OrderId(String value) implements Identity<String> {
    public OrderId {
        Objects.requireNonNull(value, "orderId cannot be null");
    }
}

// 领域事件
public class OrderCreatedEvent extends DomainEvent {
    private final String customerId;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(String orderId, String customerId, BigDecimal totalAmount) {
        super(orderId);
        this.customerId = customerId;
        this.totalAmount = totalAmount;
    }
}

// 聚合根
@Aggregate
public class Order extends AbstractAggregateRoot<Order> implements AggregateRoot {
    private OrderId id;
    private OrderStatus status;
    private List<OrderItem> items;

    public Order(String customerId, List<OrderItem> items) {
        this.id = new OrderId(UUID.randomUUID().toString());
        this.status = OrderStatus.PENDING;
        this.items = new ArrayList<>(items);

        BigDecimal totalAmount = calculateTotal();
        registerEvent(new OrderCreatedEvent(id.value(), customerId, totalAmount));
    }

    public void ship() {
        Assertions.require(
            this.status != OrderStatus.SHIPPED,
            OrderError.CANNOT_SHIP_SHIPPED
        );

        this.status = OrderStatus.SHIPPED;
        registerEvent(new OrderShippedEvent(id.value()));
    }

    public OrderId getId() {
        return id;
    }
}
```

### 3.2 使用异常体系

```java
// 领域层 - 业务规则违反
public class Order extends AbstractAggregateRoot<Order> {
    public void cancel() {
        Assertions.require(
            this.status != OrderStatus.COMPLETED,
            OrderError.CANNOT_CANCEL_COMPLETED
        );
        this.status = OrderStatus.CANCELLED;
    }
}

// 应用层 - 用例前置条件
public class OrderApplicationService {
    public OrderDto getOrder(Long orderId) {
        // 快捷版：标准 404 场景
        Order order = Assertions.requirePresent(
            orderRepository.findById(orderId)
        );
        return OrderDto.from(order);
    }

    public void cancelOrder(Long orderId, Long userId) {
        // 完整版：区分不同资源类型
        Order order = Assertions.requirePresent(
            orderRepository.findById(orderId),
            OrderError.ORDER_NOT_FOUND
        );

        Assertions.require(
            order.belongsToUser(userId),
            OrderError.NOT_ORDER_OWNER
        );

        order.cancel();
    }
}
```

### 3.3 使用架构注解

```java
// package-info.java - 标注限界上下文
@BoundedContext(name = "OrderManagement", subDomain = SubDomain.CORE)
package com.cartisan.order;

// 端口接口
@Port(PortType.REPOSITORY)
public interface OrderRepository extends BaseRepository<Order, OrderId> {
}

// 适配器实现
@Adapter(PortType.REPOSITORY)
public class JpaOrderRepository implements OrderRepository {
    // ...
}

// 领域服务
@DomainService
public class OrderPricingService {
    // 不属于任何聚合根的定价逻辑
}
```

### 3.4 使用 ArchUnit 规则

```java
// 业务项目中继承即可获得全部规则
@AnalyzeClasses(packages = "com.aieducenter")
public class ArchitectureTest extends CartisanArchRules {
    // 完成！所有规则自动生效
}

// 或选择性使用
@AnalyzeClasses(packages = "com.aieducenter")
public class ArchitectureTest {
    @ArchTest
    static final ArchRules layering = ArchRules.in(CartisanLayeringRules.class);

    @ArchTest
    static final ArchRules prohibition = ArchRules.in(CartisanProhibitionRules.class);
    // 不要 naming 规则
}
```

### 3.5 使用 Testcontainers 基类

```java
// Repository 集成测试
class OrderRepositoryTest extends IntegrationTestBase {
    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldSaveOrder() {
        // 数据库已清理（@BeforeEach TRUNCATE）
        Order order = new Order("customer-123", List.of());
        orderRepository.save(order);

        assertThat(orderRepository.findById(order.getId())).isPresent();
    }
}

// Controller API 测试
class OrderControllerTest extends ApiTestBase {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("customer-123");
        String json = objectMapper.writeValueAsString(request);

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").exists());
    }
}
```

### 3.6 使用 Fixture 工具

```java
class OrderServiceTest {
    @Test
    void shouldCreateOrder() {
        // 字符串生成
        String orderId = FixtureStrings.randomString("ORDER-");
        String email = FixtureStrings.randomEmail();

        // 数字生成
        Long customerId = FixtureNumbers.randomId();
        BigDecimal amount = FixtureNumbers.randomAmount();

        // 日期生成
        LocalDateTime orderDate = FixtureDates.now();
        LocalDateTime dueDate = FixtureDates.futureDays(7);

        // 对象构建
        Order order = FixtureBuilder.of(Order.class)
            .with("id", new OrderId(orderId))
            .with("customerId", customerId)
            .build();
    }

    @Test
    void shouldGenerateRepeatableData_whenSeedSet() {
        // 设置种子，测试可重复
        FixtureSeeds.setGlobalSeed(12345L);

        String str1 = FixtureStrings.randomString();
        String str2 = FixtureStrings.randomString();

        assertThat(str1).isEqualTo(str2);  // 相同种子 → 相同序列

        FixtureSeeds.resetSeed();
    }
}
```

### 3.7 使用 ApiResponse 响应体

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    // 成功响应（带数据）
    @GetMapping("/{id}")
    public ApiResponse<OrderDto> getOrder(@PathVariable Long id) {
        Order order = orderService.findById(id);
        return ApiResponse.ok(OrderDto.from(order));
    }

    // 成功响应（无数据）
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteOrder(@PathVariable Long id) {
        orderService.delete(id);
        return ApiResponse.ok();
    }
}
```

### 3.8 使用 RequestContext

```java
// 在任何地方获取请求上下文
@Service
public class OrderService {

    public void createOrder(CreateOrderRequest request) {
        String requestId = RequestContext.getRequestId();
        String clientIp = RequestContext.getClientIp();

        log.info("Creating order, requestId={}, clientIp={}", requestId, clientIp);
        // ...
    }
}

// requestId 生成逻辑（RequestContextFilter 自动执行）：
// 1. 优先从 X-Request-Id Header 读取
// 2. 否则生成 UUID
```

### 3.9 定义 Repository（泛型约束）

```java
// 聚合根
@Entity
public class Order extends AbstractAggregateRoot<Order> {
    @Id
    private Long id;

    public void ship() {
        registerEvent(new OrderShippedEvent(id));
    }
}

// Repository 接口（T 必须是 AggregateRoot<?>）
public interface OrderRepository extends BaseRepository<Order, Long> {
    // 继承全部 JPA 方法 + Specification
    // save() 时自动发布领域事件
}

// 使用
@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public void createOrder(Order order) {
        order.registerEvent(new OrderCreatedEvent(order.getId()));
        orderRepository.save(order);  // 自动发布事件
    }
}
```

### 3.10 使用审计和软删除基类

```java
// 仅审计
@Entity
public class Product extends Auditable {
    @Id private Long id;
    private String name;
    // 自动拥有：createdAt, lastModifiedDate, createdBy, lastModifiedBy
}

// 审计 + 软删除
@Entity
@SQLRestriction("deleted = false")  // 查询时自动过滤
public class Order extends SoftDeletable {
    @Id private Long id;
    private String status;
    // 自动拥有：审计字段 + deleted
}

// 软删除操作
orderRepository.delete(order);  // UPDATE SET deleted = true
orderRepository.findAll();      // 自动过滤 deleted = true
```

### 3.11 使用 TSID 生成器

```java
@Entity
public class Order extends AbstractAggregateRoot<Order> {

    @Id
    private Long id;

    @PrePersist
    void generateId() {
        if (id == null) {
            id = tsidGenerator.generate();
        }
    }
}

// 或在 Service 层生成
@Service
public class OrderService {
    private final TsidGenerator tsidGenerator;

    public Long createOrder() {
        Long orderId = tsidGenerator.generate();
        Instant createTime = tsidGenerator.toInstant(orderId);
        // ...
        return orderId;
    }
}
```

### 3.12 监听领域事件

```java
@Component
public class OrderEventHandler {

    // 事务提交后执行（推荐）
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCreatedEvent event) {
        // 发送通知、调用外部服务等
        notificationService.sendOrderCreated(event);
    }

    // 事务内同步执行
    @EventListener
    public void handle2(OrderShippedEvent event) {
        // 同库操作，如更新其他聚合根
    }
}
```

---

## 四、注意事项

### 4.1 DDD 相关

| 规则 | 说明 |
|------|------|
| **DDD-001** | Entity 接口泛型方法中调泛型参数方法，必须先 `getClass()` 检查再强转 |
| **DDD-002** | ValueObject 的 `sameValueAs` 可直接委托 `equals` |
| **DDD-003** | 领域事件应自动生成 `eventId` 和 `occurredAt`，`aggregateId` 由子类提供 |
| **STYLE-003** | 使用 Record 实现 ValueObject 和 Identity |

### 4.2 JPA / 数据访问

| 规则 | 说明 |
|------|------|
| **DATA-001** | JPA `save()` 后必须用原始 entity 发布事件，而非返回值 |
| **DATA-002** | Repository 不是 Spring Bean，依赖注入用静态持有者模式 |
| **DATA-003** | `@MappedSuperclass` 需要添加 `@EntityListeners(AuditingEntityListener.class)` |
| **DATA-004** | `@SQLRestriction` 在 `@MappedSuperclass` 上可能无法正确继承，子类重复声明才保险 |
| **DATA-005** | JPQL `@Query` 查询不受 `@SQLRestriction` 影响，需手动添加软删除条件 |

### 4.3 Spring Boot / 自动配置

| 规则 | 说明 |
|------|------|
| **BOOT-001** | 使用 `JpaRepositoryFactoryEntryCustomizer` 全局配置 `repositoryBaseClass` |
| **TOOL-007** | `@Component` 默认 bean 名称可能与自动配置冲突，需显式指定如 `@Component("cartisanXxx")` |

### 4.4 分布式 ID / TSID

| 规则 | 说明 |
|------|------|
| **ID-001** | 纯随机 TSID 测试需要容忍小量重复（≤0.2%），不应要求 100% 唯一 |
| **ID-002** | 无锁随机数生成使用 `ThreadLocalRandom`，不用 `synchronized` |

### 4.5 工具配置

| 规则 | 说明 |
|------|------|
| **TOOL-005** | Testcontainers 与 Docker Engine 29 需要版本 1.21.4+ |
| **TOOL-006** | PIT 变异测试是 Phase 5 必跑门禁，杀死率 ≥ 70% |
| **TOOL-004** | `@TestConfiguration` 不能使用工具类模式（私有构造抛异常） |
| **TEST-003** | Spring Boot Test 依赖分层：`api` 暴露给业务，`implementation` 本模块使用 |

### 4.6 代码风格

| 规则 | 说明 |
|------|------|
| **STYLE-001** | 领域接口应包含完整 JavaDoc 和使用示例 |
| **STYLE-002** | JavaDoc 中必须转义 HTML 特殊字符：`<` → `&lt;`，`>` → `&gt;` |

### 4.7 测试

| 规则 | 说明 |
|------|------|
| **TEST-001** | 使用 AssertJ 而非 JUnit 断言 |
| **TEST-002** | 测试方法命名遵循 `given_{条件}_when_{操作}_then_{预期结果}` |
| **ASRT-001** | `require()` 抛 DomainException（4xx），`ensure()` 抛 IllegalStateException（500） |
| **ASRT-002** | 工具类私有构造函数应抛出异常，而非返回 null |

---

## 五、依赖说明

### 5.1 cartisan-core

```
零外部依赖，仅使用 JDK 标准库
```

### 5.2 cartisan-test

```
api 依赖：
- JUnit 5
- AssertJ
- Mockito
- ArchUnit
- Spring Boot Test
- Testcontainers

implementation 依赖：
- Spring Test
- Spring Boot Starter Data Redis
```

### 5.3 cartisan-web

```
api 依赖：
- cartisan-core

implementation 依赖：
- Spring Boot Starter Web
- Spring Boot Starter Validation
```

### 5.4 cartisan-data-jpa

```
api 依赖：
- cartisan-core

implementation 依赖：
- Spring Boot Starter Data JPA
- Hibernate Core（传递）
```

### 5.5 cartisan-event

```
api 依赖：
- cartisan-core

implementation 依赖：
- Spring Context
- Spring Boot AutoConfigure
```

---

**文档结束** | 如有疑问请参考：
- [docs/specs/epic-01-core-and-test/](../specs/epic-01-core-and-test/)
- [docs/specs/epic-02-web-data-jpa-event/](../specs/epic-02-web-data-jpa-event/)
