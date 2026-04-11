# OpenAPI + Security 关键问题修复 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 framework-review 关键发现 #1（openapi 模块多个严重问题）和 #2（SecurityFilter userName bug）

**Architecture:** 5 个独立修复点，按依赖和复杂度排序：先修简单独立的 bug，再修 openapi client 问题，最后做 Filter/Interceptor 拆分重构。

**Tech Stack:** Java 21, Spring Boot 3.4.x, JUnit 5, AssertJ, Mockito, Sa-Token

**Spec:** `docs/superpowers/specs/2026-04-11-openapi-security-fixes-design.md`

---

## File Map

| 操作 | 文件 | 职责 |
|------|------|------|
| 新增 | `cartisan-openapi/src/main/java/com/cartisan/openapi/filter/SignatureVerificationFilter.java` | 验签 Filter，写入 caller 信息到 RequestContext |
| 新增 | `cartisan-openapi/src/main/java/com/cartisan/openapi/client/OpenApiClientException.java` | HTTP 非 2xx 响应异常 |
| 修改 | `cartisan-security/src/main/java/com/cartisan/security/context/SecurityFilter.java` | userName 改从 Session 读取 |
| 修改 | `cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java` | login() JavaDoc 说明 Session 约定 |
| 修改 | `cartisan-openapi/src/main/java/com/cartisan/openapi/config/CartisanOpenapiProperties.java` | 新增 maxBodySize 配置 |
| 修改 | `cartisan-openapi/src/main/java/com/cartisan/openapi/filter/CachingRequestBodyFilter.java` | 添加 body 大小限制，注入 Properties |
| 修改 | `cartisan-openapi/src/main/java/com/cartisan/openapi/client/OpenApiClient.java` | 状态码检查 + GET query 签名 |
| 修改 | `cartisan-openapi/src/main/java/com/cartisan/openapi/interceptor/SignatureVerificationInterceptor.java` | 精简为只处理注解和权限 |
| 修改 | `cartisan-openapi/src/main/java/com/cartisan/openapi/config/CartisanOpenapiAutoConfiguration.java` | 注册 Filter，调整 Interceptor 构造参数 |
| 新增 | `cartisan-security/src/test/java/com/cartisan/security/context/SecurityFilterTest.java` | SecurityFilter 单元测试 |
| 新增 | `cartisan-openapi/src/test/java/com/cartisan/openapi/filter/CachingRequestBodyFilterTest.java` | body 大小限制测试 |
| 新增 | `cartisan-openapi/src/test/java/com/cartisan/openapi/filter/SignatureVerificationFilterTest.java` | 验签 Filter 测试 |
| 新增 | `cartisan-openapi/src/test/java/com/cartisan/openapi/interceptor/SignatureVerificationInterceptorTest.java` | 精简后 Interceptor 测试 |

---

## Task 1: SecurityFilter userName 修复（#2）

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/context/SecurityFilter.java:72`
- Modify: `cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java`
- Create: `cartisan-security/src/test/java/com/cartisan/security/context/SecurityFilterTest.java`

- [ ] **Step 1: 写 SecurityFilterTest**

新建 `cartisan-security/src/test/java/com/cartisan/security/context/SecurityFilterTest.java`

测试类用 `@ExtendWith(MockitoExtension.class)`，使用 `mockStatic(StpUtil.class)` 模拟静态方法。

需要的测试用例：
- `shouldWriteUserIdAndUserName_whenLoggedIn` — 模拟 `StpUtil.isLogin()` 返回 true，`StpUtil.getLoginIdAsLong()` 返回 42L，`StpUtil.getSession().get("userName")` 返回 "Alice"。验证下游能通过 `RequestContext.run()` 读到 userId=42, userName="Alice"
- `shouldWriteUserIdWithNullUserName_whenSessionHasNoUserName` — 模拟已登录但 Session 中无 userName。验证 userId 正确，userName 为 null
- `shouldWriteUserIdWithNullUserName_whenSessionIsNull` — 模拟 `StpUtil.getSession()` 返回 null。验证 userId 正确，userName 为 null（不抛 NPE）
- `shouldPassThrough_whenNotLoggedIn` — 模拟未登录，验证 Filter 直接调用 chain.doFilter 不修改 RequestContext

需要 mock 的依赖：
- `HttpServletRequest` / `HttpServletResponse` / `FilterChain`（Mockito mock）
- `StpUtil`（mockStatic）
- `SaSession`（mock，用于 `StpUtil.getSession()` 返回值）

实现要点：SecurityFilter 在 RequestContext.CONTEXT 已有值时用 `withUser()`，无值时 new RequestContext。测试需在 `RequestContext.run(baseCtx, () -> filter.doFilterInternal(...))` 中包裹调用，模拟 RequestContextFilter 已设置基础上下文的场景。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -pl cartisan-security -Dtest=SecurityFilterTest -Dsurefire.useFile=false`
Expected: FAIL — userName 仍然是 loginId 字符串

- [ ] **Step 3: 修改 SecurityFilter**

修改 `SecurityFilter.java:72`：

将 `String userName = StpUtil.getLoginIdAsString();` 改为 `String userName = StpUtil.getSession() != null ? (String) StpUtil.getSession().get("userName") : null;`

用一行简洁写法：先 `getSession()` 赋局部变量，再 `.get("userName")`。处理 session 为 null 的情况。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -pl cartisan-security -Dtest=SecurityFilterTest -Dsurefire.useFile=false`
Expected: PASS

- [ ] **Step 5: 更新 AuthenticationService JavaDoc**

在 `AuthenticationService.java` 的 `login(Long loginId)` 方法 JavaDoc 中添加说明：调用方应在 login() 之后通过 `StpUtil.getSession().set("userName", ...)` 将用户名存入 Session，SecurityFilter 会自动读取。

- [ ] **Step 6: 提交**

```
git add cartisan-security/src/main/java/com/cartisan/security/context/SecurityFilter.java
git add cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java
git add cartisan-security/src/test/java/com/cartisan/security/context/SecurityFilterTest.java
git commit -m "fix: SecurityFilter reads userName from Sa-Token Session instead of loginId"
```

---

## Task 2: CachingRequestBodyFilter body 大小限制（#8）

**Files:**
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/config/CartisanOpenapiProperties.java`
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/filter/CachingRequestBodyFilter.java`
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/config/CartisanOpenapiAutoConfiguration.java`
- Create: `cartisan-openapi/src/test/java/com/cartisan/openapi/filter/CachingRequestBodyFilterTest.java`

- [ ] **Step 1: 写 CachingRequestBodyFilterTest（先写测试，TDD）**

新建 `cartisan-openapi/src/test/java/com/cartisan/openapi/filter/CachingRequestBodyFilterTest.java`

测试用例：
- `shouldRejectRequest_whenBodyExceedsMaxSize` — 构造 content-type 为 application/json、Content-Length 超过 maxBodySize 的请求，验证 response 状态码为 413
- `shouldRejectRequest_whenChunkedBodyExceedsMaxSize` — 无 Content-Length header，body 实际内容超过 maxBodySize，验证 413
- `shouldAllowRequest_whenBodyWithinLimit` — body 大小在限制内，验证正常放行

测试中直接构造 `CartisanOpenapiProperties` 并设置 maxBodySize（Properties 类有 setter，无需改构造函数即可使用）。Filter 构造暂用 new CachingRequestBodyFilter()，后续 Step 3 改为接收 Properties。

用 `MockHttpServletRequest` / `MockHttpServletResponse` / `MockFilterChain`（Spring test 提供）。

> 注：此步测试会编译失败（因为 Filter 还没接收 Properties），或运行失败（因为 Filter 没做大小检查）。两者都算 TDD red。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -pl cartisan-openapi -Dtest=CachingRequestBodyFilterTest -Dsurefire.useFile=false`
Expected: FAIL — Filter 尚未实现大小检查

- [ ] **Step 3: 在 CartisanOpenapiProperties 中添加 maxBodySize**

在 `CartisanOpenapiProperties` 类中新增字段：
- `maxBodySize`（`org.springframework.util.unit.DataSize` 类型，默认 1MB）
- getter/setter 方法

配置项路径：`cartisan.openapi.max-body-size`

- [ ] **Step 4: 修改 CachingRequestBodyFilter**

修改要点：
1. 构造函数接收 `CartisanOpenapiProperties` 参数
2. 在 `doFilterInternal()` 中，缓存 body 前检查大小：
   - 先检查 `Content-Length` header，如果存在且超过 maxBodySize → 返回 413
   - 读取 body 后检查实际长度，超过 maxBodySize → 返回 413
3. 返回 413 时设置 response status 和错误消息

- [ ] **Step 5: 更新 AutoConfiguration**

修改 `CartisanOpenapiAutoConfiguration.cachingRequestBodyFilter()` 方法，传入 `properties` 到 `CachingRequestBodyFilter` 构造函数。

- [ ] **Step 6: 运行测试确认通过**

Run: `mvn test -pl cartisan-openapi -Dtest=CachingRequestBodyFilterTest -Dsurefire.useFile=false`
Expected: PASS

- [ ] **Step 7: 提交**

```
git add cartisan-openapi/src/main/java/com/cartisan/openapi/config/CartisanOpenapiProperties.java
git add cartisan-openapi/src/main/java/com/cartisan/openapi/filter/CachingRequestBodyFilter.java
git add cartisan-openapi/src/main/java/com/cartisan/openapi/config/CartisanOpenapiAutoConfiguration.java
git add cartisan-openapi/src/test/java/com/cartisan/openapi/filter/CachingRequestBodyFilterTest.java
git commit -m "feat: add maxBodySize limit to CachingRequestBodyFilter"
```

---

## Task 3: OpenApiClient HTTP 状态码检查（#5）

**Files:**
- Create: `cartisan-openapi/src/main/java/com/cartisan/openapi/client/OpenApiClientException.java`
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/client/OpenApiClient.java:58-61, 81-84`

- [ ] **Step 1: 写 OpenApiClientException 测试**

新建 `cartisan-openapi/src/test/java/com/cartisan/openapi/client/OpenApiClientExceptionTest.java`

测试用例：
- `shouldCreateException_withStatusCodeAndBody` — 构造异常，验证 statusCode 和 body 字段
- `shouldTruncateLongBody_inGetMessage` — body 超过 200 字符时，getMessage() 截断显示

测试会编译失败（Exception 类不存在），这是 TDD red。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -pl cartisan-openapi -Dtest=OpenApiClientExceptionTest -Dsurefire.useFile=false`
Expected: FAIL — 类不存在

- [ ] **Step 3: 创建 OpenApiClientException**

新建 `cartisan-openapi/src/main/java/com/cartisan/openapi/client/OpenApiClientException.java`

RuntimeException 子类，包含：
- `int statusCode` 字段（final）
- `String body` 字段（final）
- 构造函数：`OpenApiClientException(int statusCode, String body)`
- getter 方法
- `getMessage()` 返回格式化的错误信息，包含 statusCode 和 body（截断到前 200 字符）

- [ ] **Step 4: 运行 Exception 测试确认通过**

Run: `mvn test -pl cartisan-openapi -Dtest=OpenApiClientExceptionTest -Dsurefire.useFile=false`
Expected: PASS

- [ ] **Step 5: 写 OpenApiClient 状态码检查测试**

在 `OpenApiClientHeaderTest.java` 或新建 `OpenApiClientResponseTest.java` 中添加测试。

提取状态码检查为 package-private 方法 `validateResponse(HttpResponse<String> response)`，通过反射测试：

- `shouldThrowException_whenStatusCodeIs4xx` — mock HttpResponse statusCode=400，验证抛 OpenApiClientException
- `shouldThrowException_whenStatusCodeIs5xx` — mock HttpResponse statusCode=500，验证抛 OpenApiClientException
- `shouldNotThrow_whenStatusCodeIs2xx` — mock HttpResponse statusCode=200，验证不抛异常

> 注：Java HttpClient 默认不自动跟随重定向（3xx 不会出现），无需处理。

- [ ] **Step 6: 运行测试确认失败**

Run: `mvn test -pl cartisan-openapi -Dtest=OpenApiClientResponseTest -Dsurefire.useFile=false`
Expected: FAIL — validateResponse 方法不存在

- [ ] **Step 7: 在 OpenApiClient 中添加 validateResponse 并在 post/get 中调用**

在 `OpenApiClient.java` 中：
1. 新增 package-private 方法 `validateResponse(HttpResponse<String> response)`：检查 statusCode >= 400 时抛 OpenApiClientException
2. 在 `post()` 和 `get()` 方法中，`httpClient.send()` 返回后调用 `validateResponse(response)`，在 `return objectMapper.readValue(...)` 之前

- [ ] **Step 8: 运行全部 openapi 测试确认无破坏**

Run: `mvn test -pl cartisan-openapi -Dsurefire.useFile=false`
Expected: PASS

- [ ] **Step 9: 提交**

```
git add cartisan-openapi/src/main/java/com/cartisan/openapi/client/OpenApiClientException.java
git add cartisan-openapi/src/main/java/com/cartisan/openapi/client/OpenApiClient.java
git add cartisan-openapi/src/test/java/com/cartisan/openapi/client/OpenApiClientExceptionTest.java
git add cartisan-openapi/src/test/java/com/cartisan/openapi/client/OpenApiClientResponseTest.java
git commit -m "feat: OpenApiClient checks HTTP status code, throws OpenApiClientException on error"
```

---

## Task 4: GET 请求 query 参数参与签名（#6）

**Files:**
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/client/OpenApiClient.java:70-88`
- Modify: `cartisan-openapi/src/test/java/com/cartisan/openapi/client/OpenApiClientHeaderTest.java`

- [ ] **Step 1: 写测试**

在 `OpenApiClientHeaderTest.java` 中添加测试。测试思路：对比有 query 参数和无 query 参数时生成的签名不同，验证 query 参数参与了签名计算。

测试用例：
- `shouldGenerateDifferentSign_whenUrlHasQueryParams` — 用反射调用 `buildHeaders` 两次，一次传入 `Map.of("key", "value")` 作为 queryParams，一次传入 null。验证两次生成的 X-Sign 值不同
- `shouldHandleEmptyQueryString` — 传入空 string 作为 query，验证不抛异常（extractQueryParams 应返回空 Map）
- `shouldHandleNullQueryParams` — 传入 null，验证正常工作（现有行为）

- [ ] **Step 2: 运行测试确认状态**

Run: `mvn test -pl cartisan-openapi -Dtest=OpenApiClientHeaderTest -Dsurefire.useFile=false`

`shouldGenerateDifferentSign` 应该通过（buildHeaders 已支持 queryParams），但 `shouldHandleEmptyQueryString` 可能失败（extractQueryParams 方法还不存在）。确认测试状态。

- [ ] **Step 3: 修改 OpenApiClient.get() 方法**

在 `get()` 方法中：
1. 用 `URI.create(url)` 解析 URL
2. 调用新增的 `extractQueryParams(String query)` 方法解析 query string
3. 将解析结果传入 `buildHeaders("GET", new byte[0], queryParams)` 替换原来的 `null`

新增私有方法 `extractQueryParams(String query)`：
- 输入：query string（如 `"key1=val1&key2=val2"`）
- 输出：`Map<String, String>`（TreeMap 保持排序）
- 逻辑：与 `SignatureVerificationInterceptor.extractQueryParams()` 保持一致（简单 split，不做 URL 解码）
- query 为 null 或空时返回空 Map

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -pl cartisan-openapi -Dtest=OpenApiClientHeaderTest -Dsurefire.useFile=false`
Expected: PASS

- [ ] **Step 5: 提交**

```
git add cartisan-openapi/src/main/java/com/cartisan/openapi/client/OpenApiClient.java
git add cartisan-openapi/src/test/java/com/cartisan/openapi/client/OpenApiClientHeaderTest.java
git commit -m "fix: OpenApiClient GET request includes query params in signature"
```

---

## Task 5: SignatureVerificationFilter + Interceptor 拆分（#1）

这是最复杂的改动。核心思路：将验签逻辑从 Interceptor 移到 Filter，Interceptor 精简为只处理注解和权限。

**Files:**
- Create: `cartisan-openapi/src/main/java/com/cartisan/openapi/filter/SignatureVerificationFilter.java`
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/interceptor/SignatureVerificationInterceptor.java`
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/config/CartisanOpenapiAutoConfiguration.java`
- Create: `cartisan-openapi/src/test/java/com/cartisan/openapi/filter/SignatureVerificationFilterTest.java`
- Create: `cartisan-openapi/src/test/java/com/cartisan/openapi/interceptor/SignatureVerificationInterceptorTest.java`

- [ ] **Step 1: 写 SignatureVerificationFilterTest**

新建 `cartisan-openapi/src/test/java/com/cartisan/openapi/filter/SignatureVerificationFilterTest.java`

用 `@ExtendWith(MockitoExtension.class)`，mock 依赖：`SignatureCalculator`、`ApiKeyProvider`、`NonceRepository`、`CartisanOpenapiProperties`、`ObjectMapper`。

构造 SignatureVerificationFilter 实例（构造注入上述依赖）。

测试用例：
- `shouldPassThrough_whenNoAppIdHeader` — 请求无 X-App-Id header，验证直接放行 chain.doFilter()
- `shouldReject_whenTimestampExpired` — 有签名 headers 但 timestamp 超出容差，验证返回 401
- `shouldReject_whenNonceDuplicated` — nonce 校验失败，验证返回 401
- `shouldReject_whenInvalidAppId` — ApiKeyProvider 返回 null 或 inactive，验证返回 401
- `shouldReject_whenBodyDigestMismatch` — body digest 不匹配，验证返回 401
- `shouldReject_whenSignatureMismatch` — HMAC 签名不匹配，验证返回 401
- `shouldEnrichRequestContext_whenSignatureValid` — 验签成功，验证 RequestContext 中 callerAppId 和 callerAppName 正确，且 request attribute `openapi.apiKeyInfo` 包含 ApiKeyInfo
- `shouldStoreApiKeyInfoInRequestAttribute_whenSignatureValid` — 验证 `request.setAttribute("openapi.apiKeyInfo", apiKeyInfo)` 被调用
- `shouldNotEnrichContext_whenSignatureInvalid` — 验签失败，验证 RequestContext 未被修改

测试实现要点：
- 用 `MockHttpServletRequest` / `MockHttpServletResponse` / `MockFilterChain`
- Filter 执行需包裹在 `RequestContext.run(baseCtx, () -> filter.doFilter(...))` 中模拟 Filter 链
- 在 `filterChain.doFilter()` 的 answer 中通过 `RequestContext.CONTEXT.orElse(null)` 验证 caller 信息

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -pl cartisan-openapi -Dtest=SignatureVerificationFilterTest -Dsurefire.useFile=false`
Expected: FAIL — 类不存在

- [ ] **Step 3: 创建 SignatureVerificationFilter**

新建 `cartisan-openapi/src/main/java/com/cartisan/openapi/filter/SignatureVerificationFilter.java`

- 继承 `OncePerRequestFilter`，实现 `Ordered`（HIGHEST_PRECEDENCE + 15）
- 构造注入：`SignatureCalculator`、`ApiKeyProvider`、`NonceRepository`、`CartisanOpenapiProperties`、`ObjectMapper`
- 定义常量：`public static final String API_KEY_INFO_ATTR = "openapi.apiKeyInfo";`
- `doFilterInternal()` 逻辑（从现有 Interceptor 的 `preHandle` 迁移）：
  1. 检查 X-App-Id header，不存在直接放行
  2. 提取其他签名 headers
  3. 校验 timestamp
  4. 校验 nonce
  5. 查询 ApiKeyInfo
  6. 计算并校验 body digest
  7. 构建签名串并校验
  8. 成功：存 ApiKeyInfo 到 request attribute，用 `RequestContext.run(enriched, () -> chain.doFilter(...))` 写入 caller 信息
  9. 失败：写错误响应，不继续 chain
- 内部类 `SignatureException`（从 Interceptor 迁移）
- `writeError()` 私有方法（从 Interceptor 迁移）
- `getCachedBody()` 私有方法（从 Interceptor 迁移）
- `calculateBodyDigest()` 私有方法（从 Interceptor 迁移）
- `extractQueryParams()` 私有方法（从 Interceptor 迁移）
- `buildStringToSign()` 私有方法（从 Interceptor 迁移）

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -pl cartisan-openapi -Dtest=SignatureVerificationFilterTest -Dsurefire.useFile=false`
Expected: PASS

- [ ] **Step 5: 写 SignatureVerificationInterceptorTest**

新建 `cartisan-openapi/src/test/java/com/cartisan/openapi/interceptor/SignatureVerificationInterceptorTest.java`

精简后的 Interceptor 只接收 `ObjectMapper` 构造注入。

测试用例：
- `shouldPassThrough_whenNoAnnotation` — handler 无 @RequireSignature 和 @NoSignature，放行
- `shouldPassThrough_whenNoSignatureAnnotation` — handler 有 @NoSignature，放行
- `shouldReject_whenRequireSignatureButNoApiKeyInfo` — handler 有 @RequireSignature，但 request attribute 无 ApiKeyInfo，返回 false + 401
- `shouldAllow_whenRequireSignatureWithValidApiKeyInfo` — handler 有 @RequireSignature，request attribute 有有效 ApiKeyInfo，返回 true
- `shouldReject_whenPermissionDenied` — handler 有 @RequireSignature(permission="admin")，ApiKeyInfo 无 admin 权限，返回 false + 403
- `shouldAllow_whenPermissionMatched` — handler 有 @RequireSignature(permission="read")，ApiKeyInfo 有 read 权限，返回 true

测试实现要点：
- Mock `HandlerMethod`，设置方法/类注解
- Mock `HttpServletRequest`，设置 attribute `SignatureVerificationFilter.API_KEY_INFO_ATTR`
- Mock `HttpServletResponse`

- [ ] **Step 6: 运行测试确认失败**

Run: `mvn test -pl cartisan-openapi -Dtest=SignatureVerificationInterceptorTest -Dsurefire.useFile=false`
Expected: FAIL — Interceptor 构造函数尚未精简

- [ ] **Step 7: 精简 SignatureVerificationInterceptor**

修改 `SignatureVerificationInterceptor.java`：
- 构造函数只接收 `ObjectMapper`
- 移除字段：`signatureCalculator`、`apiKeyProvider`、`nonceRepository`、`properties`
- 移除所有验签逻辑（timestamp、nonce、body digest、signature 校验）
- 移除 `callerAppId`/`callerAppName` request attribute 设置
- 保留 `preHandle()` 方法，逻辑简化为：
  1. 检查 @NoSignature → return true
  2. 检查 @RequireSignature → 无注解 return true
  3. 从 request attribute 读取 `ApiKeyInfo`（key = `SignatureVerificationFilter.API_KEY_INFO_ATTR`）
  4. 无 ApiKeyInfo → writeError(401, "Signature required")，return false
  5. 检查 permission → 不匹配 writeError(403, "Permission denied")，return false
  6. return true
- 保留 `writeError()` 方法
- 移除不再需要的私有方法：`getRequiredHeader`、`getCachedBody`、`calculateBodyDigest`、`extractQueryParams`、`buildStringToSign`
- 移除 `SignatureException` 内部类

- [ ] **Step 8: 运行 Interceptor 测试确认通过**

Run: `mvn test -pl cartisan-openapi -Dtest=SignatureVerificationInterceptorTest -Dsurefire.useFile=false`
Expected: PASS

- [ ] **Step 9: 更新 AutoConfiguration**

修改 `CartisanOpenapiAutoConfiguration.java`：

1. 新增 `SignatureVerificationFilter` bean 和 `FilterRegistrationBean`：
   - `signatureVerificationFilter()` 方法，注入所有验签依赖
   - `signatureVerificationFilterRegistration()` 方法，order = HIGHEST_PRECEDENCE + 15

2. 修改 `signatureVerificationInterceptor()` 方法：
   - 构造参数只传 `ObjectMapper`

3. 实现 `addInterceptors()` 方法：
   - `registry.addInterceptor(signatureVerificationInterceptor).addPathPatterns("/**")`

- [ ] **Step 10: 运行全部 openapi 测试确认无破坏**

Run: `mvn test -pl cartisan-openapi -Dsurefire.useFile=false`
Expected: ALL PASS

- [ ] **Step 11: 提交**

```
git add cartisan-openapi/src/main/java/com/cartisan/openapi/filter/SignatureVerificationFilter.java
git add cartisan-openapi/src/main/java/com/cartisan/openapi/interceptor/SignatureVerificationInterceptor.java
git add cartisan-openapi/src/main/java/com/cartisan/openapi/config/CartisanOpenapiAutoConfiguration.java
git add cartisan-openapi/src/test/java/com/cartisan/openapi/filter/SignatureVerificationFilterTest.java
git add cartisan-openapi/src/test/java/com/cartisan/openapi/interceptor/SignatureVerificationInterceptorTest.java
git commit -m "refactor: split SignatureVerification into Filter (verification) + Interceptor (annotations)"
```

---

## Final Verification

- [ ] **全量编译验证**

Run: `mvn compile -pl cartisan-openapi,cartisan-security`
Expected: BUILD SUCCESS

- [ ] **全量测试验证**

Run: `mvn test -pl cartisan-openapi,cartisan-security`
Expected: ALL TESTS PASS

- [ ] **更新 framework-review.md 状态**

修改 `docs/framework-review.md` 中 #1、#2、#5、#6、#8 的状态为"已处理"。
