# OpenAPI 签名层面 appId → appKey 全量重命名

> **For agentic workers:** 本计划可直接按任务顺序执行，每任务包含明确的文件清单和变更说明。

**Goal:** 将 cartisan-openapi 模块中所有签名层面 `appId` 概念统一重命名为 `appKey`，消除与 app 管理模块 `appId`（Long 主键）的命名混淆。

**Architecture:** 纯重命名重构，不改变任何行为逻辑。变更范围：配置属性键/字段、HTTP header 名称、签名算法参数 key、接口方法名、Java Record 字段名、测试代码、文档。

**Tech Stack:** Java 21 / Spring Boot 3.4.x / JUnit 5

## Global Constraints

- 只改 OpenAPI 签名层面的 `appId`，不改 `RequestContext.callerAppId()`（那是上下文字段，不是签名概念）
- 签名参数 key 从 `"appId"` 改为 `"appKey"` —— 这是**协议级变更**，影响所有已部署服务的互操作，需协调升级
- 配置键名 `cartisan.openapi.self.app-id` → `cartisan.openapi.self.app-key` —— 所有使用方需同步更新配置

---

### Task 1: CartisanOpenapiProperties.Self 重命名

**Files:**
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/config/CartisanOpenapiProperties.java`

**变更内容:**
- `Self.appId` 字段 → `appKey`
- `getAppId()` → `getAppKey()`
- `setAppId()` → `setAppKey()`
- Spring Boot 配置键自动从 `cartisan.openapi.self.app-id` 变为 `cartisan.openapi.self.app-key`（`@ConfigurationProperties` 基于字段名推导 kebab-case）

---

### Task 2: OpenApiClient 重命名

**Files:**
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/client/OpenApiClient.java`

**变更内容:**
- `properties.getSelf().getAppId()` → `getAppKey()`
- 签名参数 map key `"appId"` → `"appKey"`
- 局部变量 `appId` → `appKey`
- HTTP header `"X-App-Id"` → `"X-App-Key"`

---

### Task 3: SignatureVerificationFilter 重命名

**Files:**
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/filter/SignatureVerificationFilter.java`

**变更内容:**
- 读 header `"X-App-Id"` → `"X-App-Key"`
- 局部变量 `appId` → `appKey`
- `apiKeyProvider.getByAppId()` → `getByAppKey()`
- 签名参数 map key `"appId"` → `"appKey"`
- `buildStringToSign` 方法参数 `appId` → `appKey`，签名参数 key 同步
- `withCaller(appId, ...)` → `withCaller(appKey, ...)`（仅变量名，`withCaller` 方法签名不变）
- 注释中的 `X-App-Id` → `X-App-Key`

---

### Task 4: ApiKeyInfo record 重命名

**Files:**
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/provider/ApiKeyInfo.java`

**变更内容:**
- record 字段 `appId` → `appKey`
- javadoc `@param appId` → `@param appKey`

---

### Task 5: ApiKeyProvider 接口重命名

**Files:**
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/provider/ApiKeyProvider.java`

**变更内容:**
- 方法名 `getByAppId` → `getByAppKey`
- javadoc 参数名和描述同步更新

---

### Task 6: RemoteApiKeyProvider 实现类重命名

**Files:**
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/provider/RemoteApiKeyProvider.java`

**变更内容:**
- 方法名 `getByAppId` → `getByAppKey`，参数 `appId` → `appKey`
- `fetchFromRemote` 方法参数 `appId` → `appKey`
- URL query 参数 `"?appId="` → `"?appKey="`
- JSON 解析字段 `data.get("appId")` → `data.get("appKey")`
- 日志文本中的 `appId` → `appKey`

---

### Task 7: 测试文件同步重命名

**Files:**
- Modify: `cartisan-openapi/src/test/java/com/cartisan/openapi/signature/HmacSha256SignatureCalculatorTest.java`
- Modify: `cartisan-openapi/src/test/java/com/cartisan/openapi/client/OpenApiClientHeaderTest.java`
- Modify: `cartisan-openapi/src/test/java/com/cartisan/openapi/client/OpenApiClientResponseTest.java`
- Modify: `cartisan-openapi/src/test/java/com/cartisan/openapi/filter/SignatureVerificationFilterTest.java`

**变更内容:**

HmacSha256SignatureCalculatorTest:
- 3 处硬编码签名字符串 `"appId=test&..."` → `"appKey=test&..."`

OpenApiClientHeaderTest:
- 所有 `props.getSelf().setAppId("test-app")` → `setAppKey("test-app")`
- `headers.get("X-App-Id")` → `headers.get("X-App-Key")`
- `containsKey("X-App-Id")` → `containsKey("X-App-Key")`
- `doesNotContainKey("X-Caller-App-Id")` → `doesNotContainKey("X-Caller-App-Key")`

OpenApiClientResponseTest:
- `props.getSelf().setAppId("test-app")` → `setAppKey("test-app")`

SignatureVerificationFilterTest:
- 所有 `apiKeyProvider.getByAppId(APP_ID)` → `getByAppKey(APP_ID)`
- `request.addHeader("X-App-Id", APP_ID)` → `addHeader("X-App-Key", APP_ID)`

---

### Task 8: 文档同步更新

**Files:**
- Modify: `docs/guide/OpenAPI服务间通信使用指南.md`
- Modify: `docs/guide/cartisan-boot-使用手册.md`

**变更内容:**

OpenAPI服务间通信使用指南.md:
- 第 63 行: `app-id: "order-service"` → `app-key: "order-service"`
- 第 143 行: `X-App-Id` → `X-App-Key`
- 第 169 行: `X-App-Id header` → `X-App-Key header`, `getCallerAppId()` 保持不变（不是签名概念）
- 第 177 行: `findByAppId(appId)` → `getByAppKey(appKey)`
- 第 193 行: `cartisan.openapi.self.app-id` → `cartisan.openapi.self.app-key`
- 第 242 行: `X-App-Id` → `X-App-Key`, `调用方 appId` → `调用方 appKey`
- 第 12 行表格: `appId/appSecret` → `appKey/appSecret`

cartisan-boot-使用手册.md:
- 第 844 行: `findByAppId(appId)` → `getByAppKey(appKey)`
- 第 846 行: `appId()` → `appKey()`；删除 `permissions()`（已移除的字段，顺带修正）
- 第 858 行: `cartisan.openapi.self.app-id` → `cartisan.openapi.self.app-key`
- 第 1921 行: `app-id: "order-service"` → `app-key: "order-service"`

---

### Task 9: 运行测试验证

**命令:**
```bash
mvn test -pl cartisan-openapi
```

**预期:** 全部测试通过。

---

### Task 10: 编译验证

**命令:**
```bash
mvn compile
```

**预期:** 全量编译通过。
