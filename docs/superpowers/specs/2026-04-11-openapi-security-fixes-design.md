# OpenAPI + Security 关键问题修复设计

> 日期：2026-04-11
> 范围：cartisan-openapi, cartisan-security
> 对应 Review：docs/framework-review.md 关键发现 #1、#2

---

## 修复清单

| # | 问题 | 优先级 | 模块 |
|---|------|--------|------|
| #1 | SignatureVerificationInterceptor caller 信息未写入 RequestContext | P0 | openapi |
| #2 | SecurityFilter userName 取的是 loginId | P0 | security |
| #5 | OpenApiClient 不检查 HTTP 状态码 | P1 | openapi |
| #6 | OpenApiClient GET 请求 query 参数未参与签名 | P1 | openapi |
| #8 | CachingRequestBodyFilter 无 body 大小限制 | P1 | openapi |

---

## #1 SignatureVerificationFilter + Interceptor 拆分

### 问题

`SignatureVerificationInterceptor` 在 Spring MVC Interceptor 层执行，此时 RequestContext 已通过 ScopedValue 绑定（在 RequestContextFilter 中），无法重新绑定。caller 信息只能存到 request attribute，下游 `RequestContext.getCallerAppId()` 返回 null。

### 方案

拆分为 Filter + Interceptor 两层：

#### SignatureVerificationFilter（新增）

- **Order**: HIGHEST_PRECEDENCE + 15（在 TenantFilter 之后）
- **触发条件**: 请求包含 `X-App-Id` header
- **职责**:
  1. 提取签名相关 headers（X-App-Id, X-Timestamp, X-X-Nonce, X-Body-Digest, X-Sign）
  2. 校验 timestamp 容差
  3. 校验 nonce 唯一性
  4. 查询 ApiKeyInfo 并检查 isActive
  5. 计算并校验 body digest
  6. 构建签名串并校验 HMAC-SHA256
  7. 验签成功：将 `ApiKeyInfo` 存入 request attribute（key = `openapi.apiKeyInfo`）
  8. 用 `RequestContext.run(enriched, () -> chain.doFilter(...))` 写入 callerAppId/callerAppName
  9. 验签失败：写错误响应（401），中断 Filter 链
- **无 X-App-Id header 时**: 直接 `chain.doFilter()` 放行

#### SignatureVerificationInterceptor（精简）

- **职责**: 仅处理注解逻辑和权限检查
  1. 检查 `@NoSignature` → 放行
  2. 检查 `@RequireSignature`（方法级 / 类级）
     - 无注解 → 放行
     - 有注解但 request attribute 中无 `ApiKeyInfo` → 返回 401 "Signature required"
  3. 权限检查：从 `ApiKeyInfo` 验证 `permission`，不匹配 → 返回 403

#### Filter 链顺序

```
RequestContextFilter     (HIGHEST_PRECEDENCE)
CachingRequestBodyFilter (HIGHEST_PRECEDENCE + 2)
SecurityFilter           (HIGHEST_PRECEDENCE + 5)
TenantFilter             (HIGHEST_PRECEDENCE + 10)
SignatureVerificationFilter (HIGHEST_PRECEDENCE + 15)  ← 新增
```

#### 重构细节

- 将 `SignatureException`（内部类）提升为 Filter 的内部类，或提取为包级私有异常
- Filter 和 Interceptor 共享的常量（request attribute key）定义在 Filter 中
- AutoConfiguration 中注册 Filter（FilterRegistrationBean），Interceptor 通过 WebMvcConfigurer 注册

#### 涉及文件

- 新增：`cartisan-openapi/.../filter/SignatureVerificationFilter.java`
- 修改：`cartisan-openapi/.../interceptor/SignatureVerificationInterceptor.java`（精简，移除验签逻辑）
- 修改：`cartisan-openapi/.../config/CartisanOpenapiAutoConfiguration.java`（注册 Filter）
- 新增测试：`SignatureVerificationFilterTest.java`
- 修改测试：`SignatureVerificationInterceptorTest.java`（如有）

---

## #2 SecurityFilter userName 修复

### 问题

```java
String userName = StpUtil.getLoginIdAsString();  // 返回 "12345" 而非真实用户名
```

### 方案

改为从 Sa-Token Session 读取：

```java
String userName = (String) StpUtil.getSession().get("userName");
```

- userName 为 null 时合法（某些场景无用户名），`withUser(userId, null)` 可正常工作
- 业务层需在调用 `authenticationService.login()` 之前将 userName 存入 Session：
  ```java
  StpUtil.getSession().set("userName", user.getNickname());
  authenticationService.login(user.getId());
  ```
- 框架层在 SaTokenAuthenticationService 的 login 文档中说明此约定

### 涉及文件

- 修改：`cartisan-security/.../context/SecurityFilter.java`（一行改动）
- 修改：`cartisan-security/.../authentication/SaTokenAuthenticationService.java`（JavaDoc 说明 Session 约定）
- 修改/新增测试：SecurityFilter 相关测试

---

## #5 OpenApiClient HTTP 状态码检查

### 问题

`post()` 和 `get()` 方法不检查 `response.statusCode()`，4xx/5xx 响应直接尝试 JSON 反序列化，报难以理解的解析错误。

### 方案

1. 新增 `OpenApiClientException`（`RuntimeException` 子类）：
   - 字段：`int statusCode`、`String body`
   - 便于调用方根据状态码和错误体做不同处理

2. 在 `post()` 和 `get()` 中，`httpClient.send()` 返回后检查状态码：
   ```
   if (response.statusCode() >= 400) {
       throw new OpenApiClientException(response.statusCode(), response.body());
   }
   ```

### 涉及文件

- 新增：`cartisan-openapi/.../client/OpenApiClientException.java`
- 修改：`cartisan-openapi/.../client/OpenApiClient.java`（post/get 方法加状态码检查）
- 新增测试

---

## #6 GET 请求 query 参数参与签名

### 问题

客户端 `get()` 方法传入 `new byte[0]` 和 `null`（queryParams），服务端 `extractQueryParams(request)` 提取了 query 参数并参与验签。客户端签名不包含 query，服务端验签包含，导致 GET 请求有 query 参数时验签失败。

### 方案

在 `get()` 方法中，从 URL 解析 query string 并传入 `buildHeaders`：

```java
URI uri = URI.create(url);
Map<String, String> queryParams = extractQueryParams(uri.getQuery());
Map<String, String> headers = buildHeaders("GET", new byte[0], queryParams);
```

新增 `extractQueryParams(String query)` 方法，复用服务端 `SignatureVerificationInterceptor` 中相同的参数解析逻辑。

### 涉及文件

- 修改：`cartisan-openapi/.../client/OpenApiClient.java`
- 修改/新增测试：`OpenApiClientHeaderTest.java`

---

## #8 CachingRequestBodyFilter body 大小限制

### 问题

`readAllBytes()` 无大小限制，恶意客户端发送超大请求体可导致 OOM。

### 方案

1. 在 `CartisanOpenapiProperties` 新增配置：
   - `maxBodySize`（`DataSize` 类型，默认 1MB）
   - 配置项：`cartisan.openapi.max-body-size=1MB`

2. 在 `CachingRequestBodyFilter.doFilterInternal()` 中：
   - 检查 `Content-Length` header，超限返回 413 Payload Too Large
   - 注意：`Content-Length` 可能不存在（chunked transfer），此时仍用 `readAllBytes()` 但应添加运行时大小检查

3. 实现策略：
   - 先检查 `Content-Length`，超限直接拒绝
   - 无 `Content-Length` 时读取 body，边读边检查累计大小

### 涉及文件

- 修改：`cartisan-openapi/.../config/CartisanOpenapiProperties.java`（新增 maxBodySize）
- 修改：`cartisan-openapi/.../filter/CachingRequestBodyFilter.java`
- 新增/修改测试

---

## 测试策略

每个修复点独立测试：

| # | 测试类型 | 说明 |
|---|----------|------|
| #1 | 单元测试 | Filter 验签 + caller 写入；Interceptor 注解 + 权限检查 |
| #2 | 单元测试 | SecurityFilter userName 从 Session 读取 |
| #5 | 单元测试 | OpenApiClient 状态码检查（mock HttpClient 或用 WireMock） |
| #6 | 单元测试 | GET 方法 query 参数签名计算 |
| #8 | 单元测试 | body 超限返回 413 |

---

## 不在本次范围内

- #3 ConditionSpecifications blurry 校验（data-jpa 模块，独立处理）
- #4 Long 全局序列化为 String（web 模块，独立处理）
- #7 防重 Key 缺 userId（web 模块，独立处理）
- #9 OpenApiClient 异步方法（P2 改进）
- #10 NonceRepository rate limiting（P2 改进）
- #14 SecurityFilter 委托 AuthenticationService（P1，但不属于关键发现）
- #15 TenantFilter 严格模式（P1，但不属于关键发现）
