# cartisan-boot 使用手册

> **版本**：v0.1 | **日期**：2026-03-15
> **基于 Epic**：Epic 01 - 项目骨架 + Core + Test

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

---

## 四、注意事项

### 4.1 DDD 相关

| 规则 | 说明 |
|------|------|
| **DDD-001** | Entity 接口泛型方法中调泛型参数方法，必须先 `getClass()` 检查再强转 |
| **DDD-002** | ValueObject 的 `sameValueAs` 可直接委托 `equals` |
| **DDD-003** | 领域事件应自动生成 `eventId` 和 `occurredAt`，`aggregateId` 由子类提供 |
| **STYLE-003** | 使用 Record 实现 ValueObject 和 Identity |

### 4.2 工具配置

| 规则 | 说明 |
|------|------|
| **TOOL-005** | Testcontainers 与 Docker Engine 29 需要版本 1.21.4+ |
| **TOOL-006** | PIT 变异测试是 Phase 5 必跑门禁，杀死率 ≥ 70% |
| **TOOL-004** | `@TestConfiguration` 不能使用工具类模式（私有构造抛异常） |
| **TEST-003** | Spring Boot Test 依赖分层：`api` 暴露给业务，`implementation` 本模块使用 |

### 4.3 代码风格

| 规则 | 说明 |
|------|------|
| **STYLE-001** | 领域接口应包含完整 JavaDoc 和使用示例 |
| **STYLE-002** | JavaDoc 中必须转义 HTML 特殊字符：`<` → `&lt;`，`>` → `&gt;` |

### 4.4 测试

| 规则 | 说明 |
|------|------|
| **TEST-001** | 使用 AssertJ 而非 JUnit 断言 |
| **TEST-002** | 测试方法命名遵循 `given_{条件}_when_{操作}_then_{预期结果}` |
| **ASRT-001** | `require()` 抛 DomainException（4xx），`ensure()` 抛 IllegalStateException（500） |
| **ASRT-002** | 工具类私有构造函数应抛出异常，而非返回 null |

### 4.5 JPA / 数据访问

| 规则 | 说明 |
|------|------|
| **DATA-001** | JPA `save()` 后必须用原始 entity 发布事件，而非返回值 |
| **DATA-002** | Repository 不是 Spring Bean，依赖注入用静态持有者模式 |
| **DATA-003** | `@MappedSuperclass` 需要添加 `@EntityListeners(AuditingEntityListener.class)` |
| **DATA-005** | JPQL `@Query` 查询不受 `@SQLRestriction` 影响，需手动添加软删除条件 |

### 4.6 Spring Boot

| 规则 | 说明 |
|------|------|
| **BOOT-001** | 使用 `JpaRepositoryFactoryEntryCustomizer` 全局配置 `repositoryBaseClass` |

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

---

**文档结束** | 如有疑问请参考 [docs/specs/epic-01-core-and-test/](../specs/epic-01-core-and-test/)
