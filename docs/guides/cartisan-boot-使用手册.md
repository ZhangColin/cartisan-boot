# cartisan-boot 使用手册

> **版本**：v0.4 | **日期**：2026-03-15
> **基于 Epic**：Epic 01 + Epic 02 + Epic 03 + Epic 04 - Core + Test + Web + Data-JPA + Event + Security + Data-Query

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

### 1.6 cartisan-security 模块

| 能力 | 说明 |
|------|------|
| **权限注解** | `@RequireAuth`、`@RequireRole`、`@RequirePermission` |
| **MVC 拦截器** | `SecurityInterceptor` 处理鉴权逻辑 |
| **异常处理** | `SecurityExceptionHandler` 处理 Sa-Token 异常（401/403） |
| **安全上下文** | `SecurityContext` 获取当前用户信息 |
| **多租户上下文** | `TenantContext` 获取租户 ID（Header > Session 优先级） |
| **租户过滤器** | `TenantContextFilter` 解析租户 ID，兼容 Virtual Threads |
| **认证服务** | `AuthenticationService` 接口 + Sa-Token 实现 |
| **自动配置** | Spring Boot AutoConfiguration 零配置启用 |

### 1.7 cartisan-data-query 模块

| 能力 | 说明 |
|------|------|
| **分页查询参数** | `PageQuery` 分页参数类（page、size、offset） |
| **jOOQ 自动配置** | `DSLContext` Bean 自动配置（PostgreSQL 方言、SQL 日志） |
| **多租户查询** | `JooqTenantSupport.eqTenantId()` 租户过滤条件生成 |
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

### 2.15 权限注解（com.cartisan.security.annotation）

| 注解 | 目标 | 说明 |
|------|------|------|
| `@RequireAuth` | TYPE/METHOD | 需要登录 |
| `@RequireRole` | TYPE/METHOD | 需要指定角色（OR 逻辑） |
| `@RequirePermission` | TYPE/METHOD | 需要指定权限（OR 逻辑） |

### 2.16 SecurityContext（com.cartisan.security.context）

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `getCurrentUserId()` | `Long` / `null` | 获取当前用户 ID |
| `getCurrentUsername()` | `String` / `null` | 获取当前用户名（登录 ID） |
| `hasRole(String role)` | `boolean` | 判断是否拥有角色 |
| `hasPermission(String permission)` | `boolean` | 判断是否拥有权限 |
| `isAuthenticated()` | `boolean` | 判断是否已登录 |

### 2.17 TenantContext（com.cartisan.security.context）

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `getCurrentTenantId()` | `Long` / `null` | 获取当前租户 ID |
| `hasTenant()` | `boolean` | 判断是否有租户上下文 |
| `requireTenant()` | `Long` | 获取租户 ID，不存在抛异常 |

**存储机制**：使用 `ScopedValue`（Java 21+），兼容 Virtual Threads，作用域结束自动清理。

### 2.18 AuthenticationService（com.cartisan.security.authentication）

| 接口 | 方法 | 说明 |
|------|------|------|
| `AuthenticationService` | `login(Long loginId)` | 创建登录会话 |
| | `logout()` | 销毁当前会话 |
| | `getTokenInfo()` | 获取当前 Token 信息 |
| | `authenticate(username, password)` | 业务层扩展点（默认抛异常） |

### 2.19 TokenInfo（com.cartisan.security.authentication）

| 字段 | 类型 | 说明 |
|------|------|------|
| `token` | `String` | Token 值 |
| `loginId` | `Long` | 用户标识 |
| `expireTime` | `Instant` | 过期时间 |

### 2.20 异常处理器（com.cartisan.security.config）

| 异常类型 | HTTP 状态码 | 响应消息 |
|---------|------------|---------|
| `NotLoginException` | 401 UNAUTHORIZED | 未登录或登录已过期 |
| `NotRoleException` | 403 FORBIDDEN | 无权限访问 |
| `NotPermissionException` | 403 FORBIDDEN | 无权限访问 |

### 2.21 配置属性（com.cartisan.security.config.properties）

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `cartisan.security.interceptor.path-patterns` | `List<String>` | `["/**"]` | 拦截器生效路径 |
| `cartisan.security.interceptor.exclude-path-patterns` | `List<String>` | `["/error", "/actuator/**"]` | 排除路径 |

### 2.22 PageQuery（com.cartisan.data.query.page）

| 字段/方法 | 类型/返回值 | 说明 |
|-----------|------------|------|
| `page` | `int` | 当前页码（最小 1） |
| `size` | `int` | 每页大小（范围 1-100） |
| `offset()` | `long` | 计算 OFFSET 值：`(page - 1) * size` |
| `of(int, int)` | `PageQuery` | 静态工厂方法，创建实例 |

**参数校验**：
- `page < 1` 时自动修正为 1
- `size < 1` 时修正为 20
- `size > 100` 时修正为 100

### 2.23 DSLContext 自动配置（com.cartisan.data.query.config）

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `cartisan.data-query.jooq.sql-logging` | `boolean` | `false` | 是否启用 SQL 执行日志 |

**自动配置类**：`JooqAutoConfiguration`
- 条件：存在 `DataSource` 且无用户自定义 `DSLContext`
- 方言：固定为 `SQLDialect.POSTGRES`
- Bean：可被用户自定义配置覆盖

### 2.24 JooqTenantSupport（com.cartisan.data.query.support）

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `eqTenantId(TableField<?, Long>)` | `Condition` | 生成租户等值过滤条件 |

**行为**：
- 有租户上下文时：返回 `tenantIdField.eq(tenantId)`
- 无租户上下文时：返回 `DSL.noCondition()`（不添加过滤）

**依赖说明**：需要 `cartisan-security` 模块（可选依赖）

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

### 3.13 使用权限注解

```java
// 类级别注解
@RestController
@RequireAuth  // 类内所有方法都需要登录
@RequestMapping("/api/v1/users")
public class UserController {
    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser() { ... }
}

// 方法级别注解
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @RequireRole({"admin"})
    @PostMapping("/users")
    public ApiResponse<Void> createUser() { ... }

    @RequirePermission({"user:delete"})
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) { ... }
}

// 方法覆盖类注解
@RestController
@RequireAuth  // 默认需要登录
@RequestMapping("/api/v1/public")
public class PublicController {

    @GetMapping("/info")
    public ApiResponse<Info> getInfo() { ... }  // 需要登录

    @RequireAuth(false)  // 覆盖类注解，允许匿名访问
    @GetMapping("/ping")
    public ApiResponse<String> ping() { ... }
}
```

### 3.14 使用 SecurityContext

```java
@Service
public class OrderService {

    public void createOrder(CreateOrderRequest request) {
        // 推荐用法：先检查是否登录
        if (SecurityContext.isAuthenticated()) {
            Long userId = SecurityContext.getCurrentUserId();
            String username = SecurityContext.getCurrentUsername();

            // 判断角色/权限
            boolean isAdmin = SecurityContext.hasRole("admin");
            boolean canCreate = SecurityContext.hasPermission("order:create");

            // 使用用户信息...
        }
    }

    // 或者：对返回值做 null 检查
    public void updateOrder(Long orderId, UpdateOrderRequest request) {
        Long userId = SecurityContext.getCurrentUserId();
        if (userId != null) {
            // 使用 userId...
        }
    }
}
```

### 3.15 使用 TenantContext

```java
@Service
public class OrderService {

    public void createOrder(CreateOrderRequest request) {
        // 获取租户 ID（可能为 null）
        Long tenantId = TenantContext.getCurrentTenantId();

        // 判断是否有租户上下文
        if (TenantContext.hasTenant()) {
            // 使用租户 ID...
            Order order = new Order(tenantId, request);
            orderRepository.save(order);
        }
    }

    // 强制必须有租户上下文
    public void deleteOrder(Long orderId) {
        Long tenantId = TenantContext.requireTenant();  // 无租户抛异常
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
            .orElseThrow();
        orderRepository.delete(order);
    }
}
```

### 3.16 使用 AuthenticationService

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authService;

    // 业务层验证密码后调用 login
    @PostMapping("/login")
    public ApiResponse<TokenInfo> login(@RequestBody LoginRequest request) {
        // 1. 业务层验证密码
        User user = userService.validatePassword(request.getUsername(), request.getPassword());

        // 2. 调用认证服务创建会话
        TokenInfo tokenInfo = authService.login(user.getId());

        // 3. 可选：设置租户 ID 到 Session
        StpUtil.getSession().set("tenantId", user.getTenantId());

        return ApiResponse.ok(tokenInfo);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok();
    }

    @GetMapping("/token-info")
    public ApiResponse<TokenInfo> getTokenInfo() {
        TokenInfo tokenInfo = authService.getTokenInfo();
        if (tokenInfo == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.ok(tokenInfo);
    }
}
```

### 3.17 配置拦截器路径

```yaml
# 仅保护 API 路径（默认是 /**）
cartisan:
  security:
    interceptor:
      path-patterns:
        - "/api/**"
        - "/admin/**"

# 完整配置示例
cartisan:
  security:
    interceptor:
      path-patterns:
        - "/api/**"
        - "/admin/**"
        - "/internal/**"
      exclude-path-patterns:
        - "/api/public/**"
        - "/api/health"
        - "/error"
        - "/actuator/**"
```

### 3.18 使用 PageQuery

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    // 接收前端分页参数
    @GetMapping
    public ApiResponse<PageResponse<UserDto>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageQuery pageQuery = PageQuery.of(page, size);
        // pageQuery 自动校验参数：
        // - page < 1 → 修正为 1
        // - size < 1 → 修正为 20
        // - size > 100 → 修正为 100

        long offset = pageQuery.offset();  // (page - 1) * size

        // 用于 jOOQ 查询
        List<User> users = dsl.selectFrom(USER)
            .limit(pageQuery.size())
            .offset(pageQuery.offset())
            .fetchInto(User.class);

        return ApiResponse.ok(PageResponse.of(users, total, page, size));
    }
}
```

### 3.19 使用 jOOQ 自动配置

```java
// 引入依赖后，DSLContext 自动注入可用
@Service
public class UserService {
    private final DSLContext dsl;

    public UserService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<User> findActiveUsers() {
        return dsl.selectFrom(USER)
            .where(USER.STATUS.eq("ACTIVE"))
            .orderBy(USER.CREATED_AT.desc())
            .fetchInto(User.class);
    }

    // 复杂查询示例：JOIN + 聚合
    public List<OrderSummary> getOrderSummaries(LocalDate startDate) {
        return dsl.select(
                USER.ID,
                USER.NAME,
                DSL.count.ORDER_ID().as("orderCount"),
                DSL.sum(ORDER.TOTAL_AMOUNT).as("totalAmount")
            )
            .from(USER)
            .leftJoin(ORDER).on(ORDER.USER_ID.eq(USER.ID))
            .where(ORDER.CREATED_AT.ge(startDate))
            .groupBy(USER.ID, USER.NAME)
            .fetchInto(OrderSummary.class);
    }
}
```

### 3.20 启用 SQL 日志

```yaml
# application.yml
cartisan:
  data-query:
    jooq:
      sql-logging: true  # 启用 SQL 执行日志
```

### 3.21 使用多租户查询

```java
import static com.cartisan.data.query.support.JooqTenantSupport.eqTenantId;

@Service
public class UserService {
    private final DSLContext dsl;

    // 查询时自动添加租户过滤
    public List<User> listUsers() {
        return dsl.selectFrom(USER)
            .where(eqTenantId(USER.TENANT_ID))  // 自动根据当前租户过滤
            .fetchInto(User.class);
    }

    // 组合条件查询
    public List<User> listActiveUsers() {
        return dsl.selectFrom(USER)
            .where(
                USER.STATUS.eq("ACTIVE")
                .and(eqTenantId(USER.TENANT_ID))  // 租户过滤 + 其他条件
            )
            .fetchInto(User.class);
    }

    // 无租户上下文时，eqTenantId 返回 noCondition()，不影响查询
    public List<User> listAllUsersForAdmin() {
        return dsl.selectFrom(USER)
            .where(eqTenantId(USER.TENANT_ID))  // 管理员可能无租户限制
            .fetchInto(User.class);
    }
}
```

### 3.22 jOOQ 代码生成配置

在业务项目 `build.gradle.kts` 中添加：

```kotlin
plugins {
    id("nu.studer.jooq") version "8.2.1"
}

dependencies {
    // jOOQ 代码生成器依赖
    jooqGenerator("org.jooq:jooq-codegen")
    jooqGenerator("org.jooq:jooq-meta")
    jooqGenerator("org.postgresql:postgresql")
}

jooq {
    configuration {
        generator {
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
            }
            generate {
                isJavaTimeTypes = true  // 使用 java.time 类型
            }
            target {
                packageName = "com.example.db"  // 生成代码的包名
                directory = "build/generated/jooq"
            }
        }
    }
}

// 关键：先执行 Flyway 迁移，再生成 jOOQ 代码
tasks.named<nu.studer.jooq.GenerateJooqTask>("generateJooq") {
    dependsOn("flywayMigrate")
}
```

生成后使用：

```java
import static com.example.db.Tables.*;

// 类型安全的 DSL 查询
List<UserRecord> users = dsl.selectFrom(USER)
    .where(USER.AGE.gt(18))
    .fetch();
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

### 4.8 Security

| 规则 | 说明 |
|------|------|
| **SECURITY-001** | Sa-Token 包路径是 `cn.dev33.satoken`，不是 `cn.dev33.sa-token` |
| **SECURITY-002** | Sa-Token Session 类是 `SaSession`，不是 `Session` |
| **SECURITY-003** | TenantContext 使用 `ScopedValue`，先 `isBound()` 再 `get()` |
| **SECURITY-004** | MockMvc 集成测试需要测试专用 Controller，不能直接调用 `StpUtil.login()` |
| **SECURITY-005** | `@Component` Bean 名称需显式指定（如 `@Component("cartisanXxx")`）避免冲突 |

### 4.9 jOOQ / Data-Query

| 规则 | 说明 |
|------|------|
| **QUERY-001** | `generateJooq` 任务必须依赖 `flywayMigrate`，确保先生成 schema 再生成代码 |
| **QUERY-002** | jOOQ 代码生成目录为 `build/generated/jooq`，需在 IDEA 中标记为 Generated Sources Root |
| **QUERY-003** | `PageQuery` 参数在 compact constructor 中自动校验，调用方无需手动处理边界情况 |
| **QUERY-004** | `JooqTenantSupport` 需要 `cartisan-security` 可选依赖，无租户上下文时返回 `noCondition()` |
| **QUERY-005** | jOOQ 版本由 `cartisan-dependencies` BOM 管理，业务项目无需显式指定版本 |

#### QUERY-001：代码生成任务依赖

```kotlin
// ❌ 错误：缺少任务依赖，可能生成与当前 schema 不一致的代码
tasks.named<nu.studer.jooq.GenerateJooqTask>("generateJooq") {
    // 空配置
}

// ✅ 正确：先生成 schema，再生成代码
tasks.named<nu.studer.jooq.GenerateJooqTask>("generateJooq") {
    dependsOn("flywayMigrate")
}
```

#### QUERY-002：IDEA 识别生成目录

```bash
# 方式一：通过 Gradle 同步
./gradlew cleanIdea idea

# 方式二：IDEA 中手动标记
# 右键 build/generated/jooq → Mark Directory as → Generated Sources Root
```

#### QUERY-004：JooqTenantSupport 可选依赖

```kotlin
// cartisan-data-query/build.gradle.kts
dependencies {
    // 可选依赖：运行时由使用方提供
    compileOnly(project(":cartisan-security"))
}
```

使用时需引入 security：

```kotlin
// 业务项目/build.gradle.kts
dependencies {
    implementation(project(":cartisan-data-query"))
    implementation(project(":cartisan-security"))  // 使用 JooqTenantSupport 时需要
}
```

---

## 五、CQRS 架构说明

### 5.1 读写分离设计

| 模块 | 职责 | 技术 |
|------|------|------|
| **cartisan-data-jpa** | 写侧（Command） | JPA + Hibernate |
| **cartisan-data-query** | 读侧（Query） | jOOQ + DSL |

### 5.2 典型使用场景

```java
// 写：使用 JPA 保存聚合根
@Service
public class OrderService {
    private final OrderRepository orderRepository;  // JPA

    public void createOrder(CreateOrderRequest request) {
        Order order = new Order(request.getCustomerId(), request.getItems());
        orderRepository.save(order);  // 自动发布领域事件
    }
}

// 读：使用 jOOQ 高效查询
@Service
public class OrderQueryService {
    private final DSLContext dsl;  // jOOQ

    public PageResponse<OrderDto> queryOrders(OrderQuery query, PageQuery pageQuery) {
        // 类型安全的 DSL 查询
        List<OrderDto> orders = dsl.select(
                ORDER.ID,
                ORDER.CUSTOMER_ID,
                ORDER.STATUS,
                ORDER.TOTAL_AMOUNT
            )
            .from(ORDER)
            .where(buildConditions(query))
            .orderBy(OrderConstant)
            .limit(pageQuery.size())
            .offset(pageQuery.offset())
            .fetchInto(OrderDto.class);

        long total = dsl.fetchCount(ORDER);
        return PageResponse.of(orders, total, pageQuery.page(), pageQuery.size());
    }
}
```

---

#### TOOL-008 / SECURITY-001：Sa-Token 包路径

```java
// ❌ 错误：包路径不是 cn.dev33.sa-token
import cn.dev33.sa-token.stp.StpUtil;

// ✅ 正确：包路径是 cn.dev33.satoken
import cn.dev33.satoken.stp.StpUtil;
```

#### TOOL-009 / SECURITY-002：Sa-Token Session 类

```java
// ❌ 错误：没有 cn.dev33.satoken.session.Session
import cn.dev33.satoken.session.Session;

// ✅ 正确：Session 类是 SaSession
import cn.dev33.satoken.session.SaSession;
```

#### SECURITY-003：ScopedValue 使用方式

```java
// ❌ 错误：直接 get() 可能抛 NoSuchElementException
public static Long getCurrentTenantId() {
    return TENANT_ID.get();
}

// ✅ 正确：先检查 isBound()，再 get()
public static Long getCurrentTenantId() {
    if (!TENANT_ID.isBound()) {
        return null;
    }
    return TENANT_ID.get();
}

// ✅ 或使用 getOrDefault()
public static Long getCurrentTenantId() {
    return ScopedValue.getOrDefault(TENANT_ID, null);
}
```

#### TEST-004 / SECURITY-004：MockMvc 集成测试方式

```java
// ❌ 错误：直接调用 StpUtil.login()，Sa-Token 上下文未初始化
@Test
void test() {
    StpUtil.login(100L);
    mvc.perform(get("/api/users"))
        .andExpect(status().isOk());
}

// ✅ 正确：创建测试专用 Controller，通过 HTTP 请求触发登录
@RestController
@RequestMapping("/test/auth")
class TestAuthController {
    @PostMapping("/login")
    public ApiResponse<Void> login(@RequestParam Long userId) {
        StpUtil.login(userId);
        return ApiResponse.ok();
    }
}

@Test
void test() throws Exception {
    mvc.perform(post("/test/auth/login?userId=100"))
        .andExpect(status().isOk());
    // 现在 Sa-Token 上下文已正确初始化
}
```

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

### 5.6 cartisan-security

```
api 依赖：
- cartisan-core
- cartisan-web

implementation 依赖：
- Sa-Token 1.45.0（sa-token-spring-boot3-starter）
```

### 5.7 cartisan-data-query

```
api 依赖：
- cartisan-web

implementation 依赖：
- jOOQ 3.19.29

compileOnly 依赖：
- cartisan-security（可选，用于 JooqTenantSupport）
```

---

## 六、参考文档

### 6.1 模块设计

- [cartisan-boot-设计文档.md](../cartisan-boot-设计文档.md)
- [AI协作开发SOP.md](../sop/AI协作开发SOP.md)

### 6.2 Epic 规格

- [Epic 01: Core + Test](../specs/epic-01-core-and-test/)
- [Epic 02: Web + Data-JPA + Event](../specs/epic-02-web-data-jpa-event/)
- [Epic 03: Security](../specs/epic-03-security/)
- [Epic 04: Data-Query](../specs/epic-04-data-query/)

### 6.3 配置指南

- [jOOQ 代码生成配置指南](./jooq-code-generation.md)

---

**文档结束** | 更新日期：2026-03-15
