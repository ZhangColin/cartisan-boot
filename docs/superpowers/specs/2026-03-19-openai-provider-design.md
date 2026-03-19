# F05-05 OpenAI Provider 设计文档

**Feature**: F05-05 OpenAI Provider
**Epic**: Epic-05 AI
**日期**: 2026-03-19
**状态**: 已批准，待实现

---

## 1. 背景与目标

实现 `ModelProvider` SPI 的 OpenAI 适配层，支持：
- 同步对话调用（`RestClient`）
- 流式 SSE 调用（`WebClient`）
- `base-url` 可配置（兼容代理/Azure OpenAI）
- HTTP 错误统一映射到 `DomainException`

F05-06 DeepSeek Provider 将复用本 Feature 的 `OpenAiClient`（组合而非继承），设计时需保证 `OpenAiClient` 可被外部包访问。

---

## 2. 前置条件

在实现 F05-05 前，需向 `cartisan-core` 的 `BaseCodeMessage` 新增一个错误码：

```java
// cartisan-core: BaseCodeMessage.java
THIRD_PARTY_ERROR(502, "THIRD_PARTY_ERROR", "Third-party service error: {0}"),
```

此错误码用于将 OpenAI API 的各类错误（认证失败、限流、服务端错误）统一映射，并将 OpenAI 返回的 `error.message` 透传给调用方。

---

## 3. 关键设计决策

| 问题 | 决策 | 理由 |
|------|------|------|
| `supportedModels()` 维护方式 | 通过 `OpenAiProperties.models` 配置，提供常用模型默认值 | 模型列表迭代快；代理场景模型名不同，需可配置 |
| HTTP 错误映射 | 解析 OpenAI 错误 JSON，`error.message` 透传到 `DomainException` | 框架层透传，业务层按需包装 |
| HTTP 客户端构造 | `OpenAiProvider` 内部构造，私有方法共享配置 | Provider 自包含；WireMock 测试只需 `new OpenAiProvider(properties)` |
| 流式错误处理 | 包装成 `DomainException` 后 `onError` | 与同步路径一致；Reactor 错误信道不混入数据流 |
| `finished` 与 `usage` 合并 | Provider 层合并：暂存 `finished=true` 事件，等 usage chunk 到来后一起发出 | 遵守 F05-02 契约；Registry 的 `finished && usage != null` 检查要求两者同时存在 |
| `OpenAiClient` 可见性 | `public`（类），DTO 包私有 | F05-06 DeepSeek 需从 `provider.deepseek` 包访问 `OpenAiClient`；Java 不支持跨包的包私有访问 |

---

## 4. 包结构

```
cartisan-ai/src/main/java/com/cartisan/ai/
└── provider/
    └── openai/
        ├── OpenAiProperties.java          # @ConfigurationProperties（public）
        ├── OpenAiClient.java              # HTTP 通信 + SSE 解析 + 错误映射（public）
        ├── OpenAiProvider.java            # 实现 ModelProvider，委托 OpenAiClient（public）
        └── dto/                           # 包私有，不对外暴露 OpenAI 协议细节
            ├── OpenAiChatRequest.java
            ├── OpenAiChatResponse.java
            ├── OpenAiStreamChunk.java
            └── OpenAiErrorResponse.java
```

**边界约定：**
- `dto/` 下所有类**包私有**（无 `public`），不泄露 OpenAI 协议
- `OpenAiClient` 为 `public`，F05-06 通过 `new OpenAiClient(baseUrl, apiKey)` 复用
- `OpenAiProvider` 负责 `ChatRequest` ↔ OpenAI DTO 的双向适配，不含 HTTP 逻辑

---

## 5. `OpenAiProperties`

```java
@ConfigurationProperties("cartisan.ai.openai")
public class OpenAiProperties {
    private String apiKey;
    private String baseUrl = "https://api.openai.com/v1";
    private List<String> models = List.of(
        "gpt-4o", "gpt-4o-mini", "gpt-4-turbo",
        "gpt-3.5-turbo", "o1", "o1-mini", "o3-mini"
    );
    // getters / setters
}
```

---

## 6. DTO 结构（包私有）

所有 DTO 使用 `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` 处理 snake_case ↔ camelCase 映射（OpenAI API 使用 snake_case，Java Records 使用 camelCase）。

```java
// 发送给 OpenAI 的请求体
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record OpenAiChatRequest(
    String model,
    List<Map<String, String>> messages,
    Double temperature,          // nullable；OpenAI: temperature
    Integer maxTokens,           // nullable；OpenAI: max_tokens
    boolean stream,
    Map<String, Object> streamOptions  // stream=true 时设为 {"include_usage": true}；OpenAI: stream_options
) {}

// 同步响应体
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record OpenAiChatResponse(
    String id,
    String model,                // OpenAI 返回的实际模型名（可能带版本后缀）
    List<Choice> choices,
    Usage usage
) {
    record Choice(Message message) {}
    record Message(String role, String content) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Usage(int promptTokens, int completionTokens, int totalTokens) {}
}

// 流式 SSE 单条 chunk
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record OpenAiStreamChunk(
    String id,
    List<StreamChoice> choices,
    Usage usage    // 仅最后一条 usage-only chunk 携带（当 stream_options.include_usage=true 时）
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record StreamChoice(Delta delta, String finishReason) {}
    record Delta(String content) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Usage(int promptTokens, int completionTokens, int totalTokens) {}
}

// 错误响应体
record OpenAiErrorResponse(Error error) {
    record Error(String message, String type, String code) {}
}
```

---

## 7. `OpenAiClient`

### 构造

```java
public class OpenAiClient {
    private final RestClient restClient;
    private final WebClient webClient;

    public OpenAiClient(String baseUrl, String apiKey) {
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();

        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}
```

### 同步调用

```java
OpenAiChatResponse chat(OpenAiChatRequest request) {
    return restClient.post()
        .uri("/chat/completions")
        .body(request)
        .retrieve()
        .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
            OpenAiErrorResponse err = /* 解析响应体 */;
            throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, err.error().message());
        })
        .body(OpenAiChatResponse.class);
}
```

### 流式调用

```java
Flux<OpenAiStreamChunk> chatStream(OpenAiChatRequest request) {
    return webClient.post()
        .uri("/chat/completions")
        .bodyValue(request)
        .retrieve()
        .onStatus(status -> !status.is2xxSuccessful(), res ->
            res.bodyToMono(OpenAiErrorResponse.class)
               .map(err -> new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, err.error().message()))
        )
        .bodyToFlux(String.class)
        .filter(line -> line.startsWith("data:"))
        .map(line -> line.substring(5).trim())   // "data:" 为5字符，.trim() 兼容有无空格
        .filter(data -> !"[DONE]".equals(data))
        .map(data -> /* Jackson 反序列化为 OpenAiStreamChunk */);
}
```

> **注意**：`stream_options: {include_usage: true}` 依赖 OpenAI 原生 API 支持。部分代理（Azure OpenAI、LiteLLM 等旧版本）可能不支持此字段并返回 400。如需兼容代理，可将 `streamOptions` 设为 `null`，流结束时 usage 将为 `null`（Registry 的 listener 不会被触发）。

---

## 8. `OpenAiProvider`

### 类结构

```java
public class OpenAiProvider implements ModelProvider {
    private final OpenAiClient client;
    private final List<String> supportedModels;

    public OpenAiProvider(OpenAiProperties properties) {
        this.client = new OpenAiClient(properties.getBaseUrl(), properties.getApiKey());
        this.supportedModels = List.copyOf(properties.getModels());
    }

    @Override public String id() { return "openai"; }
    @Override public List<String> supportedModels() { return supportedModels; }
}
```

### 同步适配

```java
@Override
public ChatResponse chat(ChatRequest request) {
    OpenAiChatRequest req = toOpenAiRequest(request, false);
    OpenAiChatResponse res = client.chat(req);
    return new ChatResponse(
        res.choices().get(0).message().content(),
        res.model(),
        new TokenUsage(res.usage().promptTokens(),
                       res.usage().completionTokens(),
                       res.usage().totalTokens())
    );
}
```

### 流式适配（合并 `finished` 与 `usage`）

OpenAI 流式实际 SSE 顺序：
1. 内容 chunk（`choices` 非空，`finishReason=null`）
2. 终止 chunk（`finishReason="stop"`，delta 通常为空）
3. usage chunk（`choices=[]`，`usage` 非空；仅当 `stream_options.include_usage=true` 时出现）
4. `[DONE]`

Provider 层暂存 `finished=true` 事件，等 usage chunk 到来后合并发出，确保 `ChatStreamEvent.finished=true` 时 `usage` 携带数据，满足 `Registry` 的 `finished && usage != null` 条件。

若流结束时 usage chunk 未出现（代理不支持 `stream_options`），`doOnComplete` 兜底发出携带 `usage=null` 的终止事件，保证流不会无声结束。

```java
@Override
public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
    OpenAiChatRequest req = toOpenAiRequest(request, true);
    return Flux.defer(() -> {
        AtomicReference<ChatStreamEvent> pendingFinished = new AtomicReference<>();
        return client.chatStream(req)
            .concatMap(chunk -> {
                // usage-only chunk：补齐 usage 后发出 finished 事件
                if (chunk.choices().isEmpty() && chunk.usage() != null) {
                    ChatStreamEvent pending = pendingFinished.getAndSet(null);
                    if (pending != null) {
                        return Flux.just(new ChatStreamEvent(
                            pending.delta(), true, toTokenUsage(chunk.usage())));
                    }
                    return Flux.empty();
                }

                boolean finished = !chunk.choices().isEmpty()
                    && "stop".equals(chunk.choices().get(0).finishReason());
                String delta = chunk.choices().isEmpty() ? ""
                    : Optional.ofNullable(chunk.choices().get(0).delta().content()).orElse("");

                if (finished) {
                    // 暂存，等 usage chunk；若 usage chunk 不来，doOnComplete 兜底
                    pendingFinished.set(new ChatStreamEvent(delta, true, null));
                    return Flux.empty();
                }
                return Flux.just(new ChatStreamEvent(delta, false, null));
            })
            .concatWith(Mono.defer(() -> {
                // 兜底：流正常结束但 usage chunk 未到（代理不支持 stream_options 时）
                ChatStreamEvent pending = pendingFinished.getAndSet(null);
                return pending != null ? Mono.just(pending) : Mono.empty();
            }));
    });
}
```

> **注意**：`Flux.defer()` 保证每次订阅有独立的 `pendingFinished` 实例；不要对返回的 `Flux` 调用 `share()` 或 `replay()`，否则会破坏此保证。

### 请求转换

```java
private OpenAiChatRequest toOpenAiRequest(ChatRequest request, boolean stream) {
    List<Map<String, String>> messages = request.messages().stream()
        .map(m -> Map.of("role", m.role().name().toLowerCase(), "content", m.content()))
        .toList();
    Map<String, Object> streamOptions = stream ? Map.of("include_usage", true) : null;
    return new OpenAiChatRequest(
        request.model(), messages, request.temperature(),
        request.maxTokens(), stream, streamOptions
    );
}

private TokenUsage toTokenUsage(OpenAiStreamChunk.Usage u) {
    return new TokenUsage(u.promptTokens(), u.completionTokens(), u.totalTokens());
}
```

---

## 9. 测试策略

**工具**：WireMock（需在 `build.gradle.kts` 中显式声明，Spring Boot 3.4.x 的 `spring-boot-starter-test` 不自动引入）。SSE 流式测试需要真实 TCP 端口，`@WireMockTest` 满足此要求。

```kotlin
// cartisan-ai/build.gradle.kts 新增
testImplementation("org.wiremock.integrations:wiremock-spring-boot:3.2.0")
```

### 测试类划分

| 测试类 | 覆盖范围 |
|--------|---------|
| `OpenAiClientTest` | HTTP 层：同步/流式请求构造、SSE 解析、错误映射 |
| `OpenAiProviderTest` | 适配层：`ChatRequest` ↔ DTO 转换、`ChatStreamEvent` 映射 |

### `OpenAiClientTest` 用例

```
shouldReturnChatResponse_whenOpenAiReturns200
shouldThrowDomainException_whenOpenAiReturns401
shouldThrowDomainException_whenOpenAiReturns429
shouldThrowDomainException_whenOpenAiReturns500
shouldStreamChatEvents_whenOpenAiReturnsSSE
shouldCompleteFlux_whenDoneSignalReceived
shouldThrowDomainException_whenStreamErrorOccurs
```

### `OpenAiProviderTest` 用例

```
shouldReturnOpenAi_whenIdCalled
shouldReturnConfiguredModels_whenSupportedModelsCalled
shouldMapChatRequestToOpenAiFormat
shouldMapRolesToLowercase_whenBuildingMessages
shouldMapOpenAiResponseToChatResponse
shouldMapStreamChunksToChatStreamEvents
shouldMergeUsageIntoFinishedEvent_whenUsageChunkArrivesAfterStop
shouldEmitFinishedEventWithNullUsage_whenUsageChunkNeverArrives
shouldSetStreamOptionsIncludeUsage_whenStreamIsTrue
```

### WireMock SSE 响应示例

```
data: {"id":"x","choices":[{"delta":{"content":"Hello"},"finish_reason":null}]}
data: {"id":"x","choices":[{"delta":{"content":"!"},"finish_reason":"stop"}]}
data: {"id":"x","choices":[],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}
data: [DONE]
```

---

## 10. 与后续 Feature 的接口约定

| Feature | 依赖点 |
|---------|--------|
| F05-06 DeepSeek | `new OpenAiClient(baseUrl, apiKey)` 直接复用（`OpenAiClient` 为 `public`） |
| F05-09 自动配置 | `new OpenAiProvider(openAiProperties)`，无需外部组装 HTTP 客户端 |

---

## 11. 前置任务汇总

| 任务 | 所属模块 | 说明 |
|------|---------|------|
| 新增 `BaseCodeMessage.THIRD_PARTY_ERROR` | `cartisan-core` | `(502, "THIRD_PARTY_ERROR", "Third-party service error: {0}")` |
| 新增 WireMock 测试依赖 | `cartisan-ai/build.gradle.kts` | `wiremock-spring-boot:3.2.0` |
