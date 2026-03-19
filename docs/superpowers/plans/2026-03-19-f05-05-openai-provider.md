# F05-05 OpenAI Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 `OpenAiProvider`（`ModelProvider` SPI 的 OpenAI 适配层），支持同步调用和流式 SSE 调用，HTTP 错误统一映射到 `DomainException`。

**Architecture:** 分三层：`OpenAiProperties`（配置）、`OpenAiClient`（HTTP 通信 + SSE 解析，`public` 供 F05-06 复用）、`OpenAiProvider`（协议适配，实现 `ModelProvider`）。DTO 类（4 个）置于同一包内、包私有。

**Tech Stack:** Spring `RestClient`（同步）、Spring `WebClient`（流式）、WireMock（测试）、Jackson `@JsonNaming(SnakeCaseStrategy)`

**Spec:** `docs/superpowers/specs/2026-03-19-openai-provider-design.md`

---

## Task 0: Prerequisites

**Files:**
- Modify: `cartisan-core/src/main/java/com/cartisan/core/exception/BaseCodeMessage.java`
- Modify: `cartisan-ai/build.gradle.kts`

- [ ] 在 `BaseCodeMessage` 末尾新增枚举常量 `THIRD_PARTY_ERROR(502, "THIRD_PARTY_ERROR", "Third-party service error: {0}")`
- [ ] 在 `cartisan-ai/build.gradle.kts` 的 `dependencies` 块中新增 WireMock 测试依赖：`testImplementation("org.wiremock.integrations:wiremock-spring-boot:3.2.0")`
- [ ] 编译并运行 cartisan-core 测试：`./gradlew :cartisan-core:test :cartisan-ai:compileJava`
- [ ] Commit：`feat(cartisan-core): add THIRD_PARTY_ERROR code message`

---

## Task 1: DTOs（包私有数据类）

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiChatRequest.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiChatResponse.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiStreamChunk.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiErrorResponse.java`

所有 DTO 均为**包私有**（无 `public` 修饰符），置于 `com.cartisan.ai.provider.openai` 包（而非 `dto/` 子包）。
> **注**：Spec 图示中有 `dto/` 目录，但 Java 中包私有可见性不跨子包——若放到 `openai.dto` 包，`OpenAiClient`（在 `openai` 包）将无法访问。因此 DTO 与 Client 置于同一包，通过"无 `public` 修饰符"实现封装意图。

- [ ] 创建 `OpenAiChatRequest`：包含 `model`、`messages`、`temperature`（nullable）、`maxTokens`（nullable）、`stream`、`streamOptions`（nullable）字段。标注 `@JsonNaming(SnakeCaseStrategy.class)` 和 `@JsonInclude(NON_NULL)`（避免发送 null 字段）。
- [ ] 创建 `OpenAiChatResponse`：包含 `id`、`model`、`choices`、`usage` 字段，嵌套 `Choice`、`Message`、`Usage` 内部 record。`Usage` 需 `@JsonNaming(SnakeCaseStrategy.class)`，外层 record 也需要。标注 `@JsonIgnoreProperties(ignoreUnknown = true)`。
- [ ] 创建 `OpenAiStreamChunk`：包含 `id`、`choices`、`usage` 字段，嵌套 `StreamChoice`（含 `finishReason`）、`Delta`、`Usage` 内部 record。相关 record 标注 `@JsonNaming` 和 `@JsonIgnoreProperties`。
- [ ] 创建 `OpenAiErrorResponse`：包含 `error` 字段，嵌套 `Error`（含 `message`、`type`、`code`）。
- [ ] 编译验证：`./gradlew :cartisan-ai:compileJava`
- [ ] Commit：`feat(cartisan-ai): F05-05 OpenAI DTO classes`

---

## Task 2: `OpenAiProperties`

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiProperties.java`

- [ ] 创建 `OpenAiProperties`，`@ConfigurationProperties("cartisan.ai.openai")`，字段：`apiKey`、`baseUrl`（默认 `https://api.openai.com/v1`）、`models`（默认包含 `gpt-4o`、`gpt-4o-mini`、`gpt-4-turbo`、`gpt-3.5-turbo`、`o1`、`o1-mini`、`o3-mini`）。需要 `@EnableConfigurationProperties` 或在自动配置中注册（F05-09 处理，此处只创建类）。
- [ ] 编译验证：`./gradlew :cartisan-ai:compileJava`
- [ ] Commit：`feat(cartisan-ai): F05-05 OpenAiProperties`

---

## Task 3: `OpenAiClient` 同步调用

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiClient.java`
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/provider/openai/OpenAiClientTest.java`

- [ ] 写失败测试：`shouldReturnChatResponse_whenOpenAiReturns200`——WireMock stub POST `/chat/completions` 返回合法 JSON，构造 `OpenAiClient` 指向 WireMock 端口，调用 `chat()`，断言返回内容正确（使用 `@WireMockTest`）
- [ ] 运行测试，确认测试文件**编译失败**（`OpenAiClient` 类尚未创建）
- [ ] 创建 `OpenAiClient`（`public` 类）：构造函数接收 `(String baseUrl, String apiKey)`，内部构造 `RestClient` 和 `WebClient`，共用 baseUrl + Authorization header；实现 `chat(OpenAiChatRequest)` 方法，HTTP 错误时用 `ObjectMapper` 解析错误体抛出 `DomainException(THIRD_PARTY_ERROR, err.error().message())`
- [ ] 运行测试，确认通过：`./gradlew :cartisan-ai:test --tests "*OpenAiClientTest.shouldReturnChatResponse*"`
- [ ] 补充错误映射测试：`shouldThrowDomainException_whenOpenAiReturns401`、`shouldThrowDomainException_whenOpenAiReturns429`、`shouldThrowDomainException_whenOpenAiReturns500`
- [ ] 运行所有同步测试，确认全部通过：`./gradlew :cartisan-ai:test --tests "*OpenAiClientTest"`（仅同步用例）
- [ ] Commit：`feat(cartisan-ai): F05-05 OpenAiClient sync chat()`

---

## Task 4: `OpenAiClient` 流式调用

**Files:**
- Modify: `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiClient.java`
- Modify: `cartisan-ai/src/test/java/com/cartisan/ai/provider/openai/OpenAiClientTest.java`

SSE 解析策略：`WebClient.bodyToFlux(String.class)` → 按 `\n` 拆分每个 chunk（`flatMap(chunk -> Flux.fromArray(chunk.split("\n")))`，兼容 WireMock 一次返回完整 body 和真实流式逐行返回两种场景）→ 过滤 `data:` 前缀（`substring(5).trim()`）→ 过滤 `[DONE]` → Jackson 反序列化为 `OpenAiStreamChunk`。

- [ ] 写失败测试：`shouldStreamChatEvents_whenOpenAiReturnsSSE`——WireMock stub 返回 SSE 格式响应体（4 行：内容 chunk、stop chunk、usage chunk、`[DONE]`），调用 `chatStream()`，用 `.collectList().block()` 收集，断言事件数量和 usage chunk 解析正确
- [ ] 运行测试，确认失败（`chatStream` 不存在）
- [ ] 在 `OpenAiClient` 中实现 `chatStream(OpenAiChatRequest)`，流式 HTTP 错误同样映射为 `DomainException`
- [ ] 运行测试，确认通过
- [ ] 补充：`shouldCompleteFlux_whenDoneSignalReceived`、`shouldThrowDomainException_whenStreamErrorOccurs`
- [ ] 运行全部 `OpenAiClientTest`：`./gradlew :cartisan-ai:test --tests "*OpenAiClientTest"`
- [ ] Commit：`feat(cartisan-ai): F05-05 OpenAiClient stream chatStream()`

---

## Task 5: `OpenAiProvider`

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiProvider.java`
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/provider/openai/OpenAiProviderTest.java`

`OpenAiProvider` 测试使用包私有的测试构造函数 `OpenAiProvider(OpenAiClient client, List<String> models)` 注入 mock `OpenAiClient`，避免重复启动 WireMock。（此构造函数为计划层面添加，非 spec 要求，仅用于单元测试注入。）

`chatStream()` 实现需合并 `finished=true` 与 `usage`：用 `Flux.defer()` 包裹 `AtomicReference<ChatStreamEvent> pendingFinished`，`finishReason=stop` 时暂存，usage chunk 到来时合并发出；`concatWith(Mono.defer(...))` 兜底处理 usage chunk 未到的情况（代理不支持 `stream_options` 时）。

- [ ] 写失败测试（元数据）：`shouldReturnOpenAi_whenIdCalled`、`shouldReturnConfiguredModels_whenSupportedModelsCalled`
- [ ] 运行，确认失败
- [ ] 实现 `OpenAiProvider`：公开构造函数 `(OpenAiProperties properties)`，包私有测试构造函数 `(OpenAiClient client, List<String> models)`，`id()` 返回 `"openai"`，`supportedModels()` 返回配置的模型列表
- [ ] 运行，确认元数据测试通过
- [ ] 写失败测试（同步适配）：`shouldMapChatRequestToOpenAiFormat`、`shouldMapRolesToLowercase_whenBuildingMessages`、`shouldMapOpenAiResponseToChatResponse`
- [ ] 实现 `chat(ChatRequest)` 适配：`Role.name().toLowerCase()` 映射消息角色，`ChatResponse` 使用 `OpenAiChatResponse.model()` 字段
- [ ] 运行同步测试，确认通过
- [ ] 写失败测试（流式适配）：`shouldMapStreamChunksToChatStreamEvents`、`shouldMergeUsageIntoFinishedEvent_whenUsageChunkArrivesAfterStop`、`shouldEmitFinishedEventWithNullUsage_whenUsageChunkNeverArrives`、`shouldSetStreamOptionsIncludeUsage_whenStreamIsTrue`
- [ ] 实现 `chatStream(ChatRequest)` 适配（含 `pendingFinished` 合并逻辑）
- [ ] 运行全部 `OpenAiProviderTest`，确认通过
- [ ] 运行全模块测试：`./gradlew :cartisan-core:test :cartisan-ai:test`，确认全部通过
- [ ] Commit：`feat(cartisan-ai): F05-05 OpenAiProvider`

---

## 验收标准

- `./gradlew :cartisan-ai:test` 全绿
- `OpenAiClient` 为 `public`，DTO 类无 `public` 修饰符
- `new OpenAiProvider(properties)` 可独立实例化（F05-09 自动配置入口）
- `new OpenAiClient(baseUrl, apiKey)` 可被 F05-06 DeepSeek 直接复用
