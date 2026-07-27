# Cartisan-boot 框架深度 Review

> Review 日期：2026-04-11（第二轮）
> 范围：cartisan-core, cartisan-web, cartisan-data-jpa, cartisan-data-query, cartisan-event, cartisan-security, cartisan-test, cartisan-openapi

---

## 上轮 Review 处理情况

> 上轮日期：2026-04-09，共 25 个问题

| # | 问题 | 状态 | 处理方式 |
|---|------|------|----------|
| 1 | AggregateRoot 继承 DomainEntity | **已处理** | cdea019 |
| 2 | stereotype 注解 Spring 依赖 | **已处理** | 更新文档，不再声称零依赖 |
| 3 | SecurityContext 直接依赖 Sa-Token | **已转换** | SecurityContext 已删除，改为 SecurityFilter 写 RequestContext，但 Filter 仍直接调 StpUtil |
| 4 | 上下文传递不一致 | **已处理** | 统一为 RequestContext + ScopedValue |
| 5 | 领域事件发布缺失 | **已处理** | 改为应用事件，由应用服务显式发布 |
| 6 | data-query → security 依赖 | **已处理** | 271668f 移除，改为依赖 cartisan-core |
| 7 | ConditionSpecifications 安全风险 | **未处理** | blurry 字段名仍无校验 |
| 8 | Long 全局序列化为 String | **未处理** | JacksonConfiguration 仍然全局转换 |
| 9 | BaseEnum 缓存 | **已处理** | 40a5a62 添加 ConcurrentHashMap |
| 10 | 吞异常 | **已处理** | EventPublishFailureHandler SPI |
| 11 | chatStream usage model 名 | **未处理** | 仍用 request.model() |
| 12 | 防重 Key 缺用户维度 | **部分处理** | 加了 IP，但未加 userId |
| 13 | RequestLogFilter contains 匹配 | **已处理** | 改为 startsWith |
| 14 | RedisKey 校验 | **已处理** | 7559731 |
| 15 | OpenAiCompatibleClient 无超时 | **未处理** | 仍未配置超时 |
| 16 | BaseCodeMessage 含 SUCCESS | **部分处理** | 注释改为"状态码"，但 SUCCESS 仍在枚举中 |
| 17 | TenantContext.clear() 空方法 | **已处理** | TenantContext 已删除，统一用 RequestContext |
| 18 | Auditable Long 类型 | **未处理** | 仍为 Long |
| 19 | AuthenticationService 返回 Long | **未处理** | 仍为 Long |
| 20 | jOOQ 方言硬编码 | **已处理** | 75ec8c9 可配置 |
| 21 | cartisan-core Spring provided | **已处理** | 文档已更新 |
| 22 | fastjson2 依赖 | **已处理** | 改用 Jackson |
| 23 | 没有统一分页请求 DTO | **未处理** | 仍缺失 |
| 25 | requestId 未填入 ApiResponse | **已处理** | 9d2a631 |

---

## 一、架构设计问题

### [HIGH] 1. SignatureVerificationInterceptor caller 信息未写入 RequestContext

**文件**: `cartisan-openapi/.../interceptor/SignatureVerificationInterceptor.java:132-140`

```java
// 10. Rebind RequestContext with caller info
RequestContext current = RequestContext.CONTEXT.orElse(null);
if (current != null) {
    RequestContext enriched = current.withCaller(appId, apiKeyInfo.appName());
    // We need to rebind - but ScopedValue can't be rebound in the same scope
    // Store in request attribute for downstream use
    request.setAttribute("callerAppId", appId);
    request.setAttribute("callerAppName", apiKeyInfo.appName());
}
```

**问题**: ScopedValue 不可 rebind，导致 caller 信息只能存到 request attribute。下游代码通过 `RequestContext.getCallerAppId()` 拿不到值。这是 **openapi 模块最核心的功能之一**，但实际不工作。

**建议**: 方案一：改用 `RequestContext.run(enriched, () -> chain.doFilter(...))`，在 Filter 层处理（类似 SecurityFilter/TenantFilter 的做法），而非 Interceptor 层。方案二：改用 InheritableThreadLocal 或自定义 Scope。

---

### [HIGH] 2. SecurityFilter 用 getLoginIdAsString() 作为 userName

**文件**: `cartisan-security/.../context/SecurityFilter.java:45-46`

```java
Long userId = StpUtil.getLoginIdAsLong();
String userName = StpUtil.getLoginIdAsString();  // ← 这不是用户名！
```

`getLoginIdAsString()` 返回的是 loginId 的字符串形式，不是用户名。`userName` 字段会被填充为类似 `"12345"` 而不是真实用户名（如 "张三"）。

**建议**: 应从 Sa-Token Session 或额外接口获取用户名，如 `StpUtil.getSession().getString("userName")`。

---

### [HIGH] 3. ConditionSpecifications blurry 字段名无校验

**文件**: `cartisan-data-jpa/.../specification/ConditionSpecifications.java:194-202`

`@Condition(blurry="field1,field2")` 中的字段名直接传入 `buildPath(root, fieldName)` 构建查询路径，无白名单校验。如果 DTO 的 blurry 配置可被外部控制（如动态配置），理论上可构建任意路径查询。

```java
String[] fields = blurryFields.split(",");
for (String field : fields) {
    Path<Object> path = buildPath(root, fieldName); // 未校验
}
```

**建议**: 对 blurry 字段名做合法性校验（如必须是实体声明的属性名），或限制只能通过编译时注解使用。

---

### [HIGH] 4. Jackson 全局将 Long 序列化为 String

**文件**: `cartisan-web/.../config/JacksonConfiguration.java:43-44`

```java
.serializerByType(Long.class, new ToStringSerializer())
.serializerByType(Long.TYPE, new ToStringSerializer())
```

所有 Long 字段都被序列化为 String，包括非 ID 的数值字段（如金额、数量）。前端对接时类型混乱，内部服务间调用可能出问题。

**建议**: 只对 ID 字段做 String 序列化，用 `@JsonSerialize(using = ToStringSerializer.class)` 按需标注，或自定义 `@LongId` 注解。

---

### [MEDIUM] 5. OpenApiClient 不检查 HTTP 响应状态码

**文件**: `cartisan-openapi/.../client/OpenApiClient.java:58-61, 81-84`

`post()` 和 `get()` 方法不检查 HTTP 状态码，4xx/5xx 响应会直接尝试 JSON 反序列化，导致难以理解的解析错误而非清晰的业务错误。

```java
HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
return objectMapper.readValue(response.body(), responseType);  // 不检查 status code
```

**建议**: 检查 `response.statusCode()`，非 2xx 时抛出包含原始错误信息的异常。

---

### [MEDIUM] 6. OpenApiClient GET 请求 query 参数未参与签名

**文件**: `cartisan-openapi/.../client/OpenApiClient.java:72`

```java
Map<String, String> headers = buildHeaders("GET", new byte[0], null);  // queryParams = null
```

GET 请求的 query string 未包含在签名计算中，服务端验签时 `extractQueryParams(request)` 提取了 query 参数并参与签名。这意味着 **GET 请求的签名机制实际上不完整**——客户端签名不包含 query，但服务端验签会包含，导致 GET 请求验签可能失败（如果有 query 参数的话）。

**建议**: GET 方法应解析 URL 中的 query 参数并传入 `buildHeaders`。

---

### [MEDIUM] 7. ResubmitAspect 仅用 IP 做身份标识

**文件**: `cartisan-web/.../resubmit/ResubmitAspect.java:81-82`

```java
String clientIp = RequestContext.getClientIp();
String identity = clientIp != null ? clientIp : "unknown";
```

防重 Key 只有 IP + 参数哈希，没有 userId 维度。同一 NAT 后的不同用户提交相同参数会互相阻塞。而且已登录用户的 userId 可以从 RequestContext 获取。

**建议**: 优先用 userId（`RequestContext.getUserId()`），没有时 fallback 到 clientIp。

---

## 二、新模块（cartisan-openapi）问题

### [HIGH] 8. CachingRequestBodyFilter 无 body 大小限制

**文件**: `cartisan-openapi/.../filter/CachingRequestBodyFilter.java:34`

```java
byte[] body = request.getInputStream().readAllBytes();
```

`readAllBytes()` 无大小限制，恶意客户端发送超大请求体可导致 OOM。

**建议**: 添加 `maxBodySize` 配置项（默认如 1MB），超过限制时拒绝请求。

---

### [MEDIUM] 9. OpenApiClient 使用同步 HttpClient.send()

**文件**: `cartisan-openapi/.../client/OpenApiClient.java:58`

```java
HttpResponse<String> response = httpClient.send(request.build(), ...);
```

使用同步阻塞的 `send()` 方法，在网关/高并发场景下会阻塞线程。Java 21 的 `HttpClient` 原生支持 `sendAsync()`。

**建议**: 提供 `postAsync()` / `getAsynce()` 异步方法，返回 `CompletableFuture`。

---

### [MEDIUM] 10. NonceRepository 接口缺少防滥用设计

**文件**: `cartisan-openapi/.../nonce/NonceRepository.java`

`tryAcquire(nonce, ttl)` 接口无 rate limiting，恶意客户端可以 flood 大量 nonce，Redis 实现下可能导致内存膨胀。

**建议**: 在 NonceRepository 或调用层添加基于 appId 的 rate limit。

---

## 三、编码质量问题（遗留 + 新发现）


### [MEDIUM] 13. data-query 依赖 cartisan-web（传递依赖过重）

**文件**: `cartisan-data-query/pom.xml:22-26`

jOOQ 查询模块依赖了 `cartisan-web`，间接引入了 Spring MVC、Jackson 等大量 web 依赖。实际上 `JooqTenantSupport` 只需要 `cartisan-core` 的 `RequestContext`。

**建议**: 将依赖从 `cartisan-web` 改为 `cartisan-core`。

---

### [MEDIUM] 14. SecurityFilter 仍直接调 StpUtil 静态方法

**文件**: `cartisan-security/.../context/SecurityFilter.java:44-46`

SecurityFilter 直接调用 `StpUtil.isLogin()`、`StpUtil.getLoginIdAsLong()` 等静态方法。虽然上轮 #3 的 SecurityContext 类已被删除，但问题本质仍在——静态调用不可替换、不利于测试。

```java
if (StpUtil.isLogin()) {
    Long userId = StpUtil.getLoginIdAsLong();
```

**建议**: 注入 `AuthenticationService` 实例，委托其获取当前用户信息。

---

### [MEDIUM] 15. TenantFilter 解析租户 ID 时静默失败

**文件**: `cartisan-security/.../context/TenantFilter.java:87-103`

```java
private Long parseTenantIdFromSession() {
    try {
        // ...
    } catch (Exception e) {
        log.warn("Unexpected error reading tenant from session: {}", e.getMessage());
        return null;  // 静默返回 null
    }
}
```

如果系统要求必须有租户上下文，静默返回 null 可能导致数据泄漏（跨租户）。无租户时查询不加租户过滤条件，返回全部数据。

**建议**: 提供配置选项——"严格模式"下无租户上下文直接拒绝请求，"宽松模式"下允许无租户。

---

## 四、低优先级问题

### [LOW] 16. X-Forwarded-For IP 提取无可信代理配置

**文件**: `cartisan-web/.../context/RequestContextFilter.java:97-106`

直接取 X-Forwarded-For 第一个 IP，可被客户端伪造。生产环境通常有反向代理链，应从最后一个可信代理写入的位置开始读取。

**建议**: 提供可信代理数量配置（如 `cartisan.web.trusted-proxies=2`），从 X-Forwarded-For 的倒数第 N 个 IP 开始取。

---

### [LOW] 17. BaseCodeMessage 含 SUCCESS

**文件**: `cartisan-core/.../exception/BaseCodeMessage.java`

`SUCCESS(200, "success", "Success")` 放在"状态码"枚举中，语义虽已调整但仍有些奇怪——这个枚举主要用于错误场景。`ApiResponse.ok()` 硬编码使用它。

**建议**: 低优先级，可保持现状。如要优化，让 `ApiResponse.ok()` 直接用常量。

---

### [LOW] 18. Auditable.createdBy/updatedBy 类型硬编码为 Long

**文件**: `cartisan-data-jpa/.../domain/Auditable.java`

不适用于使用 String/UUID 作为用户 ID 的系统。当前 RequestContext.userId 也是 Long，整体一致，暂不需要改。

---

### [LOW] 19. 没有统一分页请求 DTO

有 `PageResponse` 但没有对应的分页请求 Record（如 `PageQuery` 含 page/size/sort）。每个业务项目需要自己定义。

---


## 优先级总结

| 优先级 | 编号 | 问题 | 模块 |
|--------|------|------|------|
| **P0-必须修** | #1 | OpenAPI caller 信息未写入 RequestContext | ~~已修复 e5e3bb5~~ |
| **P0-必须修** | #2 | SecurityFilter userName 取的是 loginId | ~~已修复 c5ddfdd~~ |
| **P0-必须修** | #3 | ConditionSpecifications blurry 无校验 | data-jpa |
| **P0-必须修** | #4 | Long 全局序列化为 String | web |
| **P1-重要** | #5 | OpenApiClient 不检查 HTTP 状态码 | ~~已修复 de510af~~ |
| **P1-重要** | #6 | GET 请求 query 参数未参与签名 | ~~已修复 4dfb607~~ |
| **P1-重要** | #7 | 防重 Key 缺 userId 维度 | web |
| **P1-重要** | #8 | CachingRequestBodyFilter 无大小限制 | ~~已修复 7626a86~~ |
| **P1-重要** | #13 | data-query 依赖 web（传递依赖过重） | data-query |
| **P1-重要** | #14 | SecurityFilter 直接调 StpUtil 静态方法 | security |
| **P1-重要** | #15 | TenantFilter 无租户时静默放行 | security |
| **P2-改进** | #9 | OpenApiClient 同步阻塞 | openapi |
| **P2-改进** | #10 | NonceRepository 缺 rate limiting | openapi |
| **P2-改进** | #16-19 | IP 可信代理、BaseCodeMessage、分页 DTO 等 | 多模块 |

### 本轮关键发现

1. ~~**openapi 模块是新创建的，存在多个严重问题**（caller 信息丢失、无状态码检查、GET 签名不完整、无 body 大小限制），不建议直接用于生产。~~ → **已修复** (#1: e5e3bb5, #5: de510af, #6: 4dfb607, #8: 7626a86)
2. ~~**SecurityFilter 的 userName bug** 是功能性错误，会导致所有已登录用户的 userName 字段为 loginId 的字符串形式。~~ → **已修复** (#2: c5ddfdd)
3. 上轮 P0 问题中 **#3（blurry 注入）和 #4（Long 序列化）仍未修复**。
