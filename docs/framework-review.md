# Cartisan-boot 框架深度 Review

> Review 日期：2026-04-09
> 范围：cartisan-core, cartisan-web, cartisan-data-jpa, cartisan-data-query, cartisan-event, cartisan-security, cartisan-ai, cartisan-test

---

## 一、架构设计问题

### [HIGH] 1. AggregateRoot 与 DomainEntity 缺乏继承关系

> **已处理**: AggregateRoot 现在继承 DomainEntity

**文件**: `cartisan-core/.../domain/AggregateRoot.java`

`AggregateRoot<T, ID>` 是独立的标记接口，与 `DomainEntity<T, ID>` 没有继承关系。但从 DDD 语义上，聚合根**就是**实体。同时 `@Aggregate` 注解和 `AggregateRoot` 接口功能重叠。

**问题**:
- 使用者需要同时实现两个接口 + 加注解，认知负担高
- `BaseRepository<T extends AggregateRoot<T, ID>>` 中的聚合根无法调用 `getId()`，因为 `AggregateRoot` 不提供该方法

**建议**: 让 `AggregateRoot` 继承 `DomainEntity`，或直接合并。

---

### [HIGH] 2. DomainService/@Adapter 引入 Spring 依赖违反核心层约束

> **已处理**: 更新文档，不再声称零依赖；stereotype 注解明确基于 Spring @Component（provided scope）

**文件**: `cartisan-core/.../stereotype/DomainService.java`, `Adapter.java`

CLAUDE.md 明确要求 **"领域层零外部依赖，不引入 Spring 注解"**，但 `@DomainService` 和 `@Adapter` 都 meta-annotate 了 `@Component`，导致 `cartisan-core` 对 Spring 有编译期依赖。

虽然 Spring 是 `provided` scope，但这让核心层无法在非 Spring 环境使用，违背六边形架构的端口-适配器原则。

**建议**: 将 `@DomainService`、`@Adapter`、`@Port` 等需要 Spring 的 stereotype 注解移到 `cartisan-web` 或新建 `cartisan-spring` 模块。`cartisan-core` 只保留纯接口。

---

### [HIGH] 3. SecurityContext 直接依赖 Sa-Token 静态方法

**文件**: `cartisan-security/.../context/SecurityContext.java`

`SecurityContext` 被设计为"隐藏 Sa-Token 实现细节"，但内部直接调用 `StpUtil.isLogin()`、`StpUtil.getLoginIdAsLong()` 等静态方法。静态调用不可替换、不可 Mock，和直接使用 Sa-Token 没有本质区别。

**建议**: `SecurityContext` 应委托给 `AuthenticationService` 实例方法，而非直接调 StpUtil。可以在 Filter/Interceptor 中将认证信息写入 ThreadLocal/ScopedValue，SecurityContext 从中读取。

---

### [MEDIUM] 4. TenantContext 用 ScopedValue，RequestContext 用 ThreadLocal

**文件**: `SecurityContext.java`, `TenantContext.java`, `RequestContext.java`

`TenantContext` 选择了 `ScopedValue`（兼容虚拟线程），但 `RequestContext` 和 `SecurityContext` 仍用 `ThreadLocal`/直接调 StpUtil。策略不一致，且 ThreadLocal 在虚拟线程传播中可能出问题。

**建议**: 统一上下文传递机制。要么全用 ScopedValue，要么全用 ThreadLocal。

---

### [MEDIUM] 5. BaseRepositoryImpl.save() 没有发布领域事件

> **已处理**: 移除领域事件引用，改为应用事件，由应用服务显式发布

**文件**: `cartisan-data-jpa/.../repository/impl/BaseRepositoryImpl.java`

类注释说"保存聚合根时自动发布领域事件"，但 `save()` 方法只是 `return super.save(entity)`，没有实际的事件发布逻辑。

**建议**: 要么在 save 后调用 `DomainEventPublisher` 发布事件，要么让聚合根继承 Spring 的 `AbstractAggregateRoot` 并在文档中明确说明。

---

### [MEDIUM] 6. JooqTenantSupport 造成 cartisan-data-query → cartisan-security 强依赖

**文件**: `cartisan-data-query/.../support/JooqTenantSupport.java`

读模块（data-query）依赖了安全模块（security）的 TenantContext。这意味着不想用安全模块的项目也无法使用 jOOQ 查询支持。

**建议**: 将 `JooqTenantSupport` 移到 `cartisan-security` 模块，或抽取多租户为独立模块，或改为 SPI 机制让使用方注入 tenantResolver。

---

## 二、编码质量问题

### [HIGH] 7. ConditionSpecifications 存在 SQL 注入风险

**文件**: `cartisan-data-jpa/.../specification/ConditionSpecifications.java:195-202`

`blurry` 属性的字段名直接传入 `buildPath(root, fieldName)` 构建查询路径，没有白名单校验。恶意用户如果能控制 DTO 的 `@Condition(blurry=...)` 配置（如通过动态配置），理论上可以构建任意路径查询。

```java
// 当前：字段名直接使用，无校验
String[] fields = blurryFields.split(",");
for (String field : fields) {
    Path<Object> path = buildPath(root, fieldName); // 未校验
}
```

**建议**: 对 blurry 字段名做合法性校验（如必须是实体声明的属性名），或限制只能通过编译时注解使用。

---

### [HIGH] 8. Jackson 全局将 Long 序列化为 String，影响范围过大

**文件**: `cartisan-web/.../config/JacksonConfiguration.java:43-44`

```java
.serializerByType(Long.class, new ToStringSerializer())
.serializerByType(Long.TYPE, new ToStringSerializer())
```

所有 Long 字段都被序列化为 String，包括非 ID 的数值字段（如金额、数量）。这会导致：
- 前端对接时类型混乱
- 内部服务间调用可能出问题
- third-party API 集成困难

**建议**: 只对 ID 字段做 String 序列化，可以用 `@JsonSerialize(using = ToStringSerializer.class)` 按需标注，或自定义注解。

---

### [HIGH] 9. BaseEnum.parseByCode() 线性扫描，无缓存

> **已处理**: 添加 ConcurrentHashMap 缓存

**文件**: `cartisan-core/.../domain/BaseEnum.java:82-92`

每次调用 `parseByCode()` 都遍历所有枚举值。对于高频场景（如每个请求的反序列化），性能不佳。

```java
for (T t : cls.getEnumConstants()) {
    if (t.getCode().equals(code)) { return t; }
}
```

**建议**: 用 `ConcurrentHashMap<Class, Map<Integer, Enum>>` 做缓存，首次使用时构建。

---

### [MEDIUM] 10. CompositeApplicationEventPublisher 吞异常

> **已处理**: 新增 EventPublishFailureHandler SPI，支持可配置的失败处理策略

**文件**: `cartisan-event/.../CompositeApplicationEventPublisher.java:62-67`

```java
try {
    publisher.publishApplicationEvent(event);
} catch (Exception e) {
    log.error("Failed to publish event to {}...", e); // 吞掉了
}
```

事件发布失败只记日志不抛出，调用方无法感知失败。对于关键业务事件（如订单创建），静默失败可能导致数据不一致。

**建议**: 提供配置项让使用方选择是"记日志继续"还是"抛出异常"。

---

### [MEDIUM] 11. ModelProviderRegistry.chatStream 的 usage 通知用了 request.model()

**文件**: `cartisan-ai/.../provider/ModelProviderRegistry.java:81`

```java
// 流式场景用的是 request.model()（客户端请求的模型名）
notifyListeners(provider.id(), request.model(), event.usage());

// 非流式场景用的是 response.model()（服务端确认的实际模型名）
notifyListeners(provider.id(), response.model(), response.usage());
```

在代理/路由场景下，实际使用的模型可能与请求不同（如 fallback），导致统计数据不准。

**建议**: 流式场景也应传递实际使用的模型名。

---

### [MEDIUM] 12. ResubmitAspect 防重 Key 缺少用户维度

> **已处理**: 在 Key 中加入 clientIp

**文件**: `cartisan-web/.../resubmit/ResubmitAspect.java`

Redis 锁 Key 仅基于 `prefix + argsHash`，没有包含用户标识。不同用户提交相同参数会互相阻塞。

**建议**: 在 Key 中加入当前用户 ID（从 SecurityContext 获取）。

---

### [MEDIUM] 13. RequestLogFilter 的排除路径用 contains 匹配

> **已处理**: 改为 startsWith 前缀匹配

**文件**: `cartisan-web/.../filter/RequestLogFilter.java:108`

```java
return EXCLUDE_PATHS.stream().anyMatch(uri::contains);
```

`String.contains()` 会误匹配包含子串的任何 URL，如 `/api/products/druid-sword` 会被排除。

**建议**: 改用 `startsWith` 前缀匹配。

---

### [MEDIUM] 14. RedisKey 缺少参数校验

> **已处理**: 添加 Objects.requireNonNull 和范围检查

**文件**: `cartisan-core/.../util/RedisKey.java`

- `of(prefix, expireSeconds)`: 不校验 prefix 非空、expireSeconds > 0
- `key(suffix)`: 不校验 suffix 非空
- `permanent(prefix)`: 不校验 prefix 非空

**建议**: 添加 `Objects.requireNonNull` 和范围检查。

---

### [MEDIUM] 15. OpenAiCompatibleClient 没有超时和重试配置

**文件**: `cartisan-ai/.../provider/openaicompat/OpenAiCompatibleClient.java`

新建 `HttpClient` 和 `WebClient` 时没有配置连接超时、读超时、重试策略。AI API 调用可能长时间不返回，影响系统稳定性。

**建议**: 配置合理的超时（如 connectTimeout=10s, readTimeout=60s），并支持外部配置。

---

### [LOW] 16. BaseCodeMessage 包含 SUCCESS

> **部分已处理**: 注释从"错误码"改为"状态码"

**文件**: `cartisan-core/.../exception/BaseCodeMessage.java`

`SUCCESS(200, "success", "Success")` 放在"错误码"枚举中，语义矛盾。而且 `ApiResponse.ok()` 已经硬编码使用它。

**建议**: 移除 SUCCESS，让 `ApiResponse.ok()` 直接使用常量。

---

### [LOW] 17. TenantContext.clear() 是空方法

**文件**: `cartisan-security/.../context/TenantContext.java:133-136`

```java
public static void clear() {
    // 不需要做任何操作，因为 ScopedValue 的作用域在方法调用结束后自动结束
}
```

一个名为 `clear()` 但什么都不做的方法很具误导性。

**建议**: 要么移除该方法，要么在 Javadoc 中用 `@deprecated` 标注并说明原因。

---

### [LOW] 18. Auditable.createdBy/updatedBy 类型硬编码为 Long

**文件**: `cartisan-data-jpa/.../domain/Auditable.java`

`createdBy` 和 `updatedBy` 类型是 `Long`，不适用于使用 String/UUID 作为用户 ID 的系统。

**建议**: 使用泛型 `Auditable<ID>` 或改为 String 类型（更通用）。

---

### [LOW] 19. AuthenticationService.authenticate() 返回 Long

**文件**: `cartisan-security/.../authentication/AuthenticationService.java`

`authenticate()` 返回 `Long`，硬编码了用户 ID 类型。使用 UUID 或 String ID 的系统无法使用。

**建议**: 改为泛型或使用 String（最通用）。

---

### [LOW] 20. JooqAutoConfiguration 硬编码 POSTGRES 方言

> **已处理**: 方言可配置，从 JooqProperties 读取

**文件**: `cartisan-data-query/.../config/JooqAutoConfiguration.java:62`

```java
return using(dataSource, SQLDialect.POSTGRES, settings);
```

不支持 MySQL、H2 等其他数据库。

**建议**: 从 `JooqProperties` 读取方言配置，默认 POSTGRES。

---

## 三、模块依赖问题

### [MEDIUM] 21. cartisan-core 对 Spring 的 provided 依赖

> **已处理**: 更新文档，明确 stereotype 注解基于 Spring @Component（provided scope），不再声称零依赖

`cartisan-core` 的 pom.xml 声明 `spring-context` 为 `provided`。这意味着：
- 编译时需要 Spring（因为 stereotype 注解的 `@Component`）
- 运行时不打包，由使用方提供

这违背了 CLAUDE.md "领域层零外部依赖" 的原则。至少 stereotype 包应该从 core 中移出。

---

### [LOW] 22. cartisan-web 直接依赖 fastjson2

> **已处理**: 改用 Jackson ObjectMapper

**文件**: `cartisan-web/.../resubmit/ResubmitAspect.java:3`

```java
import com.alibaba.fastjson2.JSON;
```

仅用了一次 JSON 序列化来计算参数哈希。引入一个完整的 JSON 库只为这一个用途，过于重量级。而且项目已经用了 Jackson。

**建议**: 使用 Jackson 的 ObjectMapper 或简单的 `Arrays.toString(args)` + MD5。

---

## 四、缺失设计

### [MEDIUM] 23. 没有统一的分页请求 DTO

有 `PageResponse` 但没有对应的分页请求 Record（如 `PageQuery` 含 page/size/sort）。每个业务项目需要自己定义。

**建议**: 在 cartisan-web 中提供标准的 `PageQuery` record。

---

### [LOW] 24. cartisan-ai model 类缺少 JavaDoc

`ChatMessage`、`ChatRequest`、`ChatResponse`、`TokenUsage`、`Role` 等 record 都没有类级别的 JavaDoc。与 cartisan-core 模块的 JavaDoc 规范不一致。

---

### [LOW] 25. 缺少日志追踪 ID 与 ApiResponse 的串联

> **已处理**: GlobalExceptionHandler 通过 withRequestId() 填入 requestId

`ApiResponse` 有 `requestId` 字段，但 `GlobalExceptionHandler` 构建错误响应时始终传 `null`。RequestContext 中已经有了 requestId，应填入。

---

## 优先级总结

| 优先级 | 编号 | 问题 |
|--------|------|------|
| **P0-必须修** | #1 | AggregateRoot 与 DomainEntity 继承关系 |
| **P0-必须修** | #2 | stereotype 注解对 Spring 的耦合 |
| **P0-必须修** | #7 | ConditionSpecifications 安全风险 |
| **P0-必须修** | #8 | Long 全局序列化为 String |
| **P1-重要** | #3, #4 | SecurityContext / RequestContext 统一上下文传递 |
| **P1-重要** | #5 | 领域事件发布缺失 |
| **P1-重要** | #6 | data-query 对 security 的依赖 |
| **P1-重要** | #9 | BaseEnum 缓存 |
| **P1-重要** | #10-15 | 编码质量（吞异常、缺超时、匹配不当等） |
| **P2-改进** | #16-25 | 低优先级改进 |
