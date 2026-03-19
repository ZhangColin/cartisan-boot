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

F05-06 DeepSeek Provider 将复用本 Feature 的 HTTP 客户端结构（`OpenAiClient`），设计时需保证可复用性。

---

## 2. 关键设计决策

| 问题 | 决策 | 理由 |
|------|------|------|
| `supportedModels()` 维护方式 | 通过 `OpenAiProperties.models` 配置，提供常用模型默认值 | 模型列表迭代快，base-url 已可配置代理，模型名也需一致可配置 |
| HTTP 错误映射 | 解析 OpenAI 错误 JSON，原样透传 `error.message` | 框架层透传，业务层按需包装 |
| HTTP 客户端构造 | `OpenAiProvider` 内部构造，私有方法共享配置 | 保持 Provider 自包含，WireMock 测试只需 `new OpenAiProvider(properties)` |
| 流式错误处理 | 包装成 `DomainException` 后 `onError` | 与同步路径一致，Reactor 错误信道不混入数据流 |
| `finished` 与 `usage` 合并 | Provider 层合并：暂存 `finished=true` 事件，等 usage chunk 到来后一起发出 | 遵守 F05-02 契约；Registry 的 `finished && usage != null` 检查要求两者同时存在 |

---

## 3. 包结构

```
cartisan-ai/src/main/java/com/cartisan/ai/
└── provider/
    └── openai/
        ├── OpenAiProperties.java          # @ConfigurationProperties
        ├── OpenAiClient.java              # HTTP 通信 + SSE 解析 + 错误映射
        ├── OpenAiProvider.java            # 实现 ModelProvider，委托 OpenAiClient
        └── dto/                           # 包私有，不对外暴露 OpenAI 协议
            ├── OpenAiChatRequest.java
            ├── OpenAiChatResponse.java
            ├── OpenAiStreamChunk.java
            └── OpenAiErrorResponse.java
```

**边界约定：**
- `dto/` 下所有类**包私有**（无 `public`）
- `OpenAiClient` 对外只暴露 `chat()` 和 `chatStream()`，返回类型为 OpenAI DTO
- `OpenAiProvider` 负责 `ChatRequest` ↔ OpenAI DTO 的双向适配，不含 HTTP 逻辑
- F05-06 DeepSeek 直接 `new OpenAiClient(baseUrl, apiKey)` 复用

---

## 4. `OpenAiProperties`

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

## 5. DTO 结构（包私有）

```java
// 发送给 OpenAI 的请求体
record OpenAiChatRequest(
    String model,
    List<Map<String, String>> messages,
    Double temperature,       // nullable
    Integer maxTokens,        // nullable，序列化为 max_tokens
    boolean stream,
    Map<String, Object> streamOptions  // {"include_usage": true}，stream=true 时携带
) {}

// 同步响应体
record OpenAiChatResponse(
    String id,
    List<Choice> choices,
    Usage usage
) {
    record Choice(Message message) {}
    record Message(String role, String content) {}
    record Usage(int promptTokens, int completionTokens, int totalTokens) {}
}

// 流式 SSE 单条 chunk
record OpenAiStreamChunk(
    String id,
    List<StreamChoice> choices,
    Usage usage    // 仅最后一条 usage-only chunk 携带
) {
    record StreamChoice(Delta delta, String finishReason) {}
    record Delta(String content) {}
    record Usage(int promptTokens, int completionTokens, int totalTokens) {}
}

// 错误响应体
record OpenAiErrorResponse(Error error) {
    record Error(String message, String type, String code) {}
}
```

---

## 6. `OpenAiClient`

### 构造

```java
class OpenAiClient {
    private final RestClient restClient;
    private final WebClient webClient;

    OpenAiClient(String baseUrl, String apiKey) {
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
        .map(line -> line.substring(6).trim())
        .filter(data -> !"[DONE]".equals(data))
        .map(data -> /* Jackson 反序列化为 OpenAiStreamChunk */);
}
```

---

## 7. `OpenAiProvider`

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
        request.model(),
        new TokenUsage(res.usage().promptTokens(),
                       res.usage().completionTokens(),
                       res.usage().totalTokens())
    );
}
```

### 流式适配（合并 `finished` 与 `usage`）

OpenAI 流式实际顺序：
1. 内容 chunk（`choices` 非空，`finishReason=null`）
2. 终止 chunk（`finishReason="stop"`）
3. usage chunk（`choices` 为空，`usage` 非空）
4. `[DONE]`

Provider 层暂存 `finished=true` 事件，等 usage chunk 到来后合并发出，确保 `ChatStreamEvent.finished=true` 时 `usage` 一定不为 null。

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
                    // 暂存，等 usage chunk
                    pendingFinished.set(new ChatStreamEvent(delta, true, null));
                    return Flux.empty();
                }
                return Flux.just(new ChatStreamEvent(delta, false, null));
            });
    });
}
```

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

## 8. 测试策略

**工具**：WireMock（Spring Boot Test 内置），不依赖真实 API Key。

### 测试类划分

| 测试类 | 覆盖范围 |
|--------|---------|
| `OpenAiClientTest` | HTTP 层：请求构造、SSE 解析、错误映射 |
| `OpenAiProviderTest` | 适配层：ChatRequest ↔ DTO 转换、ChatStreamEvent 映射 |

### `OpenAiClientTest` 用例

```
shouldReturnChatResponse_whenOpenAiReturns200
shouldThrowDomainException_whenOpenAiReturns401
shouldThrowDomainException_whenOpenAiReturns429
shouldThrowDomainException_whenOpenAiReturns500
shouldStreamChatEvents_whenOpenAiReturnsSSE
shouldCompleteFlux_whenDoneSignalReceived
```

### `OpenAiProviderTest` 用例

```
shouldReturnOpenAi_whenIdCalled
shouldReturnConfiguredModels_whenSupportedModelsCalled
shouldMapChatRequestToOpenAiFormat
shouldMapOpenAiResponseToChatResponse
shouldMapStreamChunksToChatStreamEvents
shouldMergeUsageIntoFinishedEvent_whenUsageChunkArrivesAfterStop
```

### WireMock SSE 响应示例

```
data: {"id":"x","choices":[{"delta":{"content":"Hello"},"finish_reason":null}]}
data: {"id":"x","choices":[{"delta":{"content":"!"},"finish_reason":"stop"}]}
data: {"id":"x","choices":[],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}
data: [DONE]
```

---

## 9. 与后续 Feature 的接口约定

| Feature | 依赖点 |
|---------|--------|
| F05-06 DeepSeek | `new OpenAiClient(baseUrl, apiKey)` 直接复用，DeepSeekProvider 组合而非继承 |
| F05-09 自动配置 | `new OpenAiProvider(openAiProperties)`，无需外部组装 HTTP 客户端 |
