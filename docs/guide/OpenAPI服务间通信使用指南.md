# OpenAPI 服务间通信使用指南

> **适用版本**：cartisan-boot 0.1.0+
> **最后更新**：2026-04-12

## 一、模块概述

cartisan-openapi 为微服务间的 HTTP 调用提供**签名验签 + 权限控制**能力，解决以下问题：

| 问题 | 解决方案 |
|------|----------|
| 服务间调用身份不明 | appId/appSecret 签名机制 |
| 请求被篡改或重放 | HMAC-SHA256 签名 + nonce + timestamp |
| 接口权限控制 | @RequireSignature 注解 + 权限检查 |
| 上下文丢失 | OpenApiClient 自动传递 RequestContext headers |

### 架构设计

采用 **Filter + Interceptor** 分层设计：

```
请求进入
  ↓
SignatureVerificationFilter（验签 + 写入 RequestContext caller 信息）
  ↓
SignatureVerificationInterceptor（注解解析 + 权限检查）
  ↓
Controller
```

**为什么拆两层？** 因为 ScopedValue 不可 rebind，只有 Filter 层能用 `RequestContext.run(enriched, ...)` 写入 caller 信息，Interceptor 层做不到。

### Filter 链顺序

```
RequestContextFilter         (HIGHEST_PRECEDENCE)
CachingRequestBodyFilter     (HIGHEST_PRECEDENCE + 2)
SecurityFilter               (HIGHEST_PRECEDENCE + 5)
TenantFilter                 (HIGHEST_PRECEDENCE + 10)
SignatureVerificationFilter  (HIGHEST_PRECEDENCE + 15)
```

---

## 二、快速开始

### 2.1 引入依赖

```xml
<dependency>
    <groupId>com.cartisan</groupId>
    <artifactId>cartisan-openapi</artifactId>
</dependency>
```

### 2.2 配置

```yaml
cartisan:
  openapi:
    # 本服务作为客户端的身份
    self:
      app-id: "order-service"
      app-secret: "${OPENAPI_SECRET}"
    # 远端 API Key 查询服务（可选，也可自定义 ApiKeyProvider）
    apikey-service-url: "http://auth-service/api/apikeys"
    # 安全参数
    timestamp-tolerance: 300    # 签名时间戳容差（秒）
    nonce-ttl: 300             # nonce 有效期（秒）
    # 请求体大小限制
    max-body-size: 1MB
    # API Key 缓存
    cache:
      expire-after-access: 30m
      maximum-size: 1000
```

### 2.3 客户端调用

```java
@Service
@RequiredArgsConstructor
public class OrderClient {
    private final OpenApiClient openApiClient;

    public UserInfo getUser(Long userId) {
        return openApiClient.get(
            "http://user-service/api/users/" + userId,
            UserInfo.class
        );
    }

    public OrderDTO createOrder(CreateOrderRequest request) {
        return openApiClient.post(
            "http://order-service/api/orders",
            request,
            OrderDTO.class
        );
    }
}
```

### 2.4 服务端验签 + 权限控制

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @RequireSignature(permission = "order:read")
    @GetMapping("/{id}")
    public ApiResponse<OrderDTO> getOrder(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getOrder(id));
    }

    @RequireSignature(permission = "order:write")
    @PostMapping
    public ApiResponse<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.create(request));
    }

    @NoSignature  // 跳过验签
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("ok");
    }
}
```

---

## 三、核心 API

### 3.1 OpenApiClient（客户端）

| 方法 | 说明 |
|------|------|
| `post(url, body, responseType)` | 发送签名 POST 请求 |
| `get(url, responseType)` | 发送签名 GET 请求（query 参数参与签名） |

**自动行为**：
- 自动计算 HMAC-SHA256 签名
- 自动添加签名 headers（X-App-Id, X-Timestamp, X-Nonce, X-Body-Digest, X-Sign）
- 自动传递 RequestContext headers（requestId, clientIp, userId 等，不含 caller 字段）
- 非 2xx 响应抛出 `OpenApiClientException`（含 statusCode 和 body）

### 3.2 OpenApiClientException

```java
public class OpenApiClientException extends RuntimeException {
    private final int statusCode;   // HTTP 状态码
    private final String body;      // 响应体（截断到 200 字符）
}
```

### 3.3 服务端注解

| 注解 | 目标 | 说明 |
|------|------|------|
| `@RequireSignature` | TYPE/METHOD | 需要验签，可选 permission 属性 |
| `@NoSignature` | TYPE/METHOD | 跳过验签（用于 @RequireSignature 类中个别放行接口） |

### 3.4 RequestContext caller 信息

验签成功后，Filter 通过 `RequestContext.run(enriched, ...)` 写入：

| 字段 | 来源 | 获取方式 |
|------|------|----------|
| callerAppId | X-App-Id header | `RequestContext.getCallerAppId()` |
| callerAppName | ApiKeyInfo.appName | `RequestContext.getCallerAppName()` |

### 3.5 ApiKeyProvider（接口）

| 实现 | 说明 |
|------|------|
| `RemoteApiKeyProvider` | 从远程服务获取 API Key，Caffeine 缓存 |
| 自定义 | 实现 `ApiKeyProvider.findByAppId(appId)` |

### 3.6 NonceRepository（接口）

| 方法 | 说明 |
|------|------|
| `tryAcquire(nonce, ttl)` | 获取 nonce 锁，防重放 |

需要业务项目提供 Redis 实现。

---

## 四、配置属性

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `cartisan.openapi.self.app-id` | String | "" | 本服务 appId |
| `cartisan.openapi.self.app-secret` | String | "" | 本服务 appSecret |
| `cartisan.openapi.apikey-service-url` | String | "" | API Key 查询地址 |
| `cartisan.openapi.timestamp-tolerance` | int | 300 | 时间戳容差（秒） |
| `cartisan.openapi.nonce-ttl` | int | 300 | nonce 有效期（秒） |
| `cartisan.openapi.max-body-size` | DataSize | 1MB | 请求体大小限制 |
| `cartisan.openapi.cache.expire-after-access` | Duration | 30m | 缓存过期时间 |
| `cartisan.openapi.cache.maximum-size` | long | 1000 | 最大缓存条目数 |

---

## 五、设计取舍

### 5.1 为什么 Filter + Interceptor 拆分？

ScopedValue 不可在已绑定的作用域中 rebind。Interceptor 运行在 Spring MVC 层，此时 RequestContext 已绑定，无法写入 caller 信息。所以验签在 Filter 层（可用 `RequestContext.run()` 创建新绑定），注解和权限检查在 Interceptor 层。

### 5.2 为什么 GET 请求的 query 参数参与签名？

防止攻击者修改 URL query 参数。客户端从 URL 解析 query，服务端从 `request.getQueryString()` 提取，双方使用相同的规范化策略参与签名计算。

### 5.3 为什么 body 大小限制默认 1MB？

`CachingRequestBodyFilter` 将请求体缓存在内存中。无限制的 body 可能导致 OOM。只对 `application/json` 请求生效（已有行为）。

### 5.4 为什么 OpenApiClientException 包含 body？

非 2xx 响应的 body 可能包含有用的错误信息（如业务错误码），方便调用方诊断问题。body 在 getMessage() 中截断到 200 字符，避免日志爆炸。

---

## 六、注意事项

| 规则 | 说明 |
|------|------|
| **OPENAPI-001** | `@NoSignature` 仅对有 `@RequireSignature` 的类或方法有意义，无条件放行的接口不需要标注 |
| **OPENAPI-002** | `OpenApiClient` 使用同步 `HttpClient.send()`，高并发场景考虑异步改造 |
| **OPENAPI-003** | `NonceRepository` 需要业务项目提供 Redis 实现 |
| **OPENAPI-004** | `RemoteApiKeyProvider` 的缓存是本地 Caffeine，多实例部署时有短暂不一致（默认 30 分钟） |
| **OPENAPI-005** | GET 请求签名包含 query 参数，URL 变更会影响签名验证 |

---

## 七、签名机制详解

### 签名 Headers

| Header | 说明 |
|--------|------|
| X-App-Id | 调用方 appId |
| X-Timestamp | 请求时间戳（秒） |
| X-Nonce | 随机字符串（防重放） |
| X-Body-Digest | 请求体 SHA-256 摘要 |
| X-Sign | HMAC-SHA256 签名 |

### 签名串构建

```
HTTP_METHOD\n
REQUEST_PATH\n
SORTED_QUERY_PARAMS\n
TIMESTAMP\n
NONCE\n
BODY_DIGEST
```

各部分用 `\n` 连接，query params 按 key 字典序排列。
