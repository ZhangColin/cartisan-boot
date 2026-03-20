# F05-07: Anthropic Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Anthropic Claude API Provider，支持同步调用和 SSE 流式调用

**Architecture:**
- 独立的 `AnthropicClient` 封装 HTTP 调用（RestClient 同步 + WebClient 流式）
- `AnthropicProvider` 实现 `ModelProvider` 接口，统一到 cartisan-ai 对话模型
- Anthropic 专用 DTO 处理其独特的请求/响应格式（与 OpenAI 不兼容）

**Tech Stack:**
- Java 21 Record（DTO）
- RestClient（同步调用）
- WebClient（SSE 流式调用）
- WireMock（测试）
- Jackson（JSON 序列化）

**Anthropic API 差异（关键）：**
- Header: `x-api-key`（非 `Authorization: Bearer`）、`anthropic-version: 2023-06-01`
- 端点: `POST /v1/messages`
- 必填: `max_tokens`
- 响应: `content` 是数组 `[{type: "text", text: "..."}]`
- 流式事件: `content_block_delta.delta.text`（非 OpenAI 的 `choices[0].delta.content`）
- Usage 在 `message_delta` 事件中

---

## File Structure

```
cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/
├── AnthropicProvider.java          # ModelProvider 实现
├── AnthropicClient.java            # HTTP 客户端
├── AnthropicProperties.java        # @ConfigurationProperties
├── dto/
│   ├── AnthropicChatRequest.java   # 请求 DTO
│   ├── AnthropicChatResponse.java  # 响应 DTO
│   ├── AnthropicStreamChunk.java   # 流式事件 DTO
│   └── AnthropicErrorResponse.java # 错误响应 DTO
└── package-info.java

cartisan-ai/src/test/java/com/cartisan/ai/provider/anthropic/
└── AnthropicProviderTest.java      # WireMock 测试
```

---

## Task 1: Create anthropic package and package-info

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/package-info.java`

- [ ] **Step 1: Create package-info.java**

```java
/**
 * Anthropic Claude API Provider 实现。
 *
 * <p>独立协议实现，与 OpenAI 不兼容：
 * <ul>
 *   <li>Header 使用 {@code x-api-key} 而非 {@code Authorization: Bearer}</li>
 *   <li>响应格式不同：{@code content} 是数组而非单一字符串</li>
 *   <li>流式事件格式不同：使用 {@code content_block_delta} 事件类型</li>
 * </ul>
 *
 * @since 0.5.0
 */
package com.cartisan.ai.provider.anthropic;
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :cartisan-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/package-info.java
git commit -m "feat(cartisan-ai): add anthropic package structure for F05-07"
```

---

## Task 2: Create AnthropicProperties configuration

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/AnthropicProperties.java`

- [ ] **Step 1: Create AnthropicProperties**

```java
package com.cartisan.ai.provider.anthropic;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic Provider 配置属性。
 *
 * <p>配置前缀：{@code cartisan.ai.anthropic}
 *
 * <p>{@code @EnableConfigurationProperties} 注册在 F05-09 自动装配模块中完成。
 */
@ConfigurationProperties("cartisan.ai.anthropic")
public class AnthropicProperties {

    /**
     * Anthropic API 密钥，作为 x-api-key header 发送，必填。
     */
    private String apiKey;

    /**
     * API 基础 URL，默认指向 Anthropic 官方 API。
     * <p>可配置为代理地址或 AWS Bedrock 端点。
     */
    private String baseUrl = "https://api.anthropic.com";

    /**
     * 支持的模型名称列表。
     * <p>默认包含 Claude 常用模型（2025 年版本）。
     */
    private List<String> models = new ArrayList<>(List.of(
            "claude-sonnet-4-20250514",
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229"
    ));

    /**
     * 默认 max_tokens，当 ChatRequest.maxTokens 为 null 时使用。
     * <p>Anthropic 要求 max_tokens 必填，OpenAI 则可选。
     */
    private int defaultMaxTokens = 4096;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    public int getDefaultMaxTokens() {
        return defaultMaxTokens;
    }

    public void setDefaultMaxTokens(int defaultMaxTokens) {
        this.defaultMaxTokens = defaultMaxTokens;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :cartisan-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/AnthropicProperties.java
git commit -m "feat(cartisan-ai): add AnthropicProperties for F05-07"
```

---

## Task 3: Create Anthropic DTOs (Request/Response/Stream)

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/dto/AnthropicChatRequest.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/dto/AnthropicChatResponse.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/dto/AnthropicStreamChunk.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/dto/AnthropicErrorResponse.java`

- [ ] **Step 1: Create AnthropicChatRequest**

```java
package com.cartisan.ai.provider.anthropic.dto;

import com.cartisan.ai.model.ChatMessage;
import com.cartisan.ai.model.Role;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API 请求体。
 *
 * @see <a href="https://docs.anthropic.com/en/api/messages">Anthropic Messages API</a>
 */
public record AnthropicChatRequest(
        String model,
        List<AnthropicMessage> messages,
        int maxTokens,
        Double temperature,
        boolean stream
) {
    public AnthropicChatRequest {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
    }

    /**
     * 从统一 ChatRequest 转换。
     */
    public static AnthropicChatRequest from(com.cartisan.ai.model.ChatRequest request, int defaultMaxTokens) {
        List<AnthropicMessage> messages = request.messages().stream()
                .map(m -> new AnthropicMessage(
                        m.role() == Role.USER ? "user" : "assistant",
                        m.content()))
                .toList();
        int maxTokens = request.maxTokens() != null ? request.maxTokens() : defaultMaxTokens;
        return new AnthropicChatRequest(request.model(), messages, maxTokens, request.temperature(), request.stream());
    }

    /**
     * Anthropic 消息格式（role + content 字符串）。
     */
    public record AnthropicMessage(String role, String content) {
        public AnthropicMessage {
            if (!role.equals("user") && !role.equals("assistant")) {
                throw new IllegalArgumentException("role must be 'user' or 'assistant'");
            }
        }
    }
}
```

- [ ] **Step 2: Create AnthropicChatResponse**

```java
package com.cartisan.ai.provider.anthropic.dto;

import java.util.List;

/**
 * Anthropic Messages API 响应体。
 */
public record AnthropicChatResponse(
        String id,
        String type,
        String role,
        List<ContentBlock> content,
        String model,
        String stopReason,
        Usage usage
) {
    /**
     * 内容块，当前仅支持 text 类型。
     */
    public record ContentBlock(String type, String text) {
        public ContentBlock {
            if (!"text".equals(type)) {
                throw new IllegalArgumentException("Only text type is supported");
            }
        }
    }

    /**
     * Token 使用量。
     */
    public record Usage(int inputTokens, int outputTokens) {
    }
}
```

- [ ] **Step 3: Create AnthropicStreamChunk**

```java
package com.cartisan.ai.provider.anthropic.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Anthropic 流式事件（SSE）。
 * <p>支持的事件类型：message_start, content_block_start, content_block_delta,
 * content_block_stop, message_delta, message_stop, ping, error
 */
public record AnthropicStreamChunk(
        String type,
        JsonNode data
) {
    /**
     * 获取 content_block_delta 的文本增量。
     */
    public String getTextDelta() {
        if (!"content_block_delta".equals(type)) {
            return "";
        }
        JsonNode delta = data.get("delta");
        if (delta != null && delta.has("text")) {
            return delta.get("text").asText();
        }
        return "";
    }

    /**
     * 检查是否是最终 message_delta 事件（含 usage）。
     */
    public boolean isMessageDelta() {
        return "message_delta".equals(type);
    }

    /**
     * 获取 message_delta 中的 usage。
     */
    public Usage getUsage() {
        if (!isMessageDelta()) {
            return null;
        }
        JsonNode usageNode = data.get("usage");
        if (usageNode == null) {
            return null;
        }
        return new Usage(
                usageNode.get("input_tokens").asInt(),
                usageNode.get("output_tokens").asInt()
        );
    }

    /**
     * 检查是否是 message_stop（流结束）。
     */
    public boolean isMessageStop() {
        return "message_stop".equals(type);
    }

    /**
     * 检查是否是 ping 事件（应忽略）。
     */
    public boolean isPing() {
        return "ping".equals(type);
    }

    /**
     * 检查是否是错误事件。
     */
    public boolean isError() {
        return "error".equals(type);
    }

    /**
     * 获取错误信息。
     */
    public String getErrorMessage() {
        if (!isError()) {
            return null;
        }
        JsonNode errorNode = data.get("error");
        if (errorNode != null && errorNode.has("message")) {
            return errorNode.get("message").asText();
        }
        return "Unknown error";
    }

    /**
     * Token 使用量（复用 AnthropicChatResponse.Usage）。
     */
    public record Usage(int inputTokens, int outputTokens) {
    }
}
```

- [ ] **Step 4: Create AnthropicErrorResponse**

```java
package com.cartisan.ai.provider.anthropic.dto;

/**
 * Anthropic HTTP 错误响应。
 */
public record AnthropicErrorResponse(Error error) {
    public record Error(String type, String message) {
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :cartisan-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/dto/
git commit -m "feat(cartisan-ai): add Anthropic DTOs for F05-07"
```

---

## Task 4: Create AnthropicClient (HTTP client)

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/AnthropicClient.java`

- [ ] **Step 1: Create AnthropicClient**

```java
package com.cartisan.ai.provider.anthropic;

import com.cartisan.ai.provider.anthropic.dto.AnthropicChatRequest;
import com.cartisan.ai.provider.anthropic.dto.AnthropicChatResponse;
import com.cartisan.ai.provider.anthropic.dto.AnthropicErrorResponse;
import com.cartisan.ai.provider.anthropic.dto.AnthropicStreamChunk;
import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.DomainException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.HttpProtocol;

/**
 * Anthropic API HTTP 客户端。
 * <p>封装同步调用（RestClient）和流式调用（WebClient）。
 */
public class AnthropicClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final RestClient restClient;
    private final WebClient webClient;

    public AnthropicClient(String baseUrl, String apiKey) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        reactor.netty.http.client.HttpClient.create().protocol(HttpProtocol.HTTP11)))
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 同步调用 /v1/messages。
     */
    public AnthropicChatResponse chat(AnthropicChatRequest request) {
        return restClient.post()
                .uri("/v1/messages")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                    byte[] body = res.getBody() != null ? res.getBody().readAllBytes() : new byte[0];
                    if (body.length == 0) {
                        throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, "Unknown error (no response body)");
                    }
                    try {
                        AnthropicErrorResponse err = objectMapper.readValue(body, AnthropicErrorResponse.class);
                        throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, err.error().message());
                    } catch (IOException e) {
                        throw new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, e, "Failed to parse error response");
                    }
                })
                .body(AnthropicChatResponse.class);
    }

    /**
     * 流式调用 /v1/messages（SSE）。
     * <p>解析 Anthropic 特有的事件格式：event: xxx\ndata: {...}
     */
    public Flux<AnthropicStreamChunk> chatStream(AnthropicChatRequest request) {
        return webClient.post()
                .uri("/v1/messages")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), res ->
                        res.bodyToMono(AnthropicErrorResponse.class)
                                .map(err -> new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, err.error().message()))
                                .switchIfEmpty(Mono.just(new DomainException(BaseCodeMessage.THIRD_PARTY_ERROR, "Unknown error (no response body)"))))
                .bodyToFlux(String.class)
                .flatMap(chunk -> Flux.fromArray(chunk.split("\n\n")))
                .filter(event -> !event.isBlank())
                .map(event -> parseSseEvent(event));
    }

    /**
     * 解析单个 SSE 事件。
     * <p>格式：event: message_start\ndata: {...}
     */
    private AnthropicStreamChunk parseSseEvent(String event) {
        String[] lines = event.split("\n", 2);
        String eventType = lines.length > 0 && lines[0].startsWith("event: ")
                ? lines[0].substring(7)
                : "";
        String dataLine = lines.length > 1 && lines[1].startsWith("data: ")
                ? lines[1].substring(6)
                : "{}";

        try {
            JsonNode dataNode = objectMapper.readValue(dataLine, JsonNode.class);
            // 如果事件中没有 type 字段，从 event 行提取
            if (dataNode.has("type")) {
                eventType = dataNode.get("type").asText();
            }
            return new AnthropicStreamChunk(eventType, dataNode);
        } catch (JsonProcessingException e) {
            // 解析失败时返回错误事件
            return new AnthropicStreamChunk("error",
                    objectMapper.createObjectNode().put("error", "Failed to parse event: " + e.getMessage()));
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :cartisan-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/AnthropicClient.java
git commit -m "feat(cartisan-ai): add AnthropicClient for F05-07"
```

---

## Task 4a: Add withStream() method to ChatRequest (prerequisite)

**Files:**
- Modify: `cartisan-ai/src/main/java/com/cartisan/ai/model/ChatRequest.java`

- [ ] **Step 1: Add withStream method to ChatRequest**

```java
// 添加到 ChatRequest record
public ChatRequest withStream(boolean stream) {
    return new ChatRequest(model, messages, temperature, maxTokens, stream);
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :cartisan-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/model/ChatRequest.java
git commit -m "feat(cartisan-ai): add withStream method to ChatRequest for F05-07"
```

---

## Task 5: Create AnthropicProvider

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/AnthropicProvider.java`

- [ ] **Step 1: Create AnthropicProvider**

```java
package com.cartisan.ai.provider.anthropic;

import com.cartisan.ai.model.ChatRequest;
import com.cartisan.ai.model.ChatResponse;
import com.cartisan.ai.model.ChatStreamEvent;
import com.cartisan.ai.model.TokenUsage;
import com.cartisan.ai.provider.ModelProvider;
import com.cartisan.ai.provider.anthropic.dto.AnthropicChatRequest;
import com.cartisan.ai.provider.anthropic.dto.AnthropicChatResponse;
import com.cartisan.ai.provider.anthropic.dto.AnthropicStreamChunk;
import com.cartisan.core.exception.DomainException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Anthropic Claude Provider 实现。
 */
public class AnthropicProvider implements ModelProvider {

    private final AnthropicClient client;
    private final List<String> supportedModels;
    private final int defaultMaxTokens;

    public AnthropicProvider(AnthropicProperties properties) {
        this.client = new AnthropicClient(properties.getBaseUrl(), properties.getApiKey());
        this.supportedModels = List.copyOf(properties.getModels());
        this.defaultMaxTokens = properties.getDefaultMaxTokens();
    }

    /**
     * 测试用构造函数（支持注入 Mock client）。
     */
    AnthropicProvider(AnthropicClient client, List<String> models, int defaultMaxTokens) {
        this.client = client;
        this.supportedModels = List.copyOf(models);
        this.defaultMaxTokens = defaultMaxTokens;
    }

    @Override
    public String id() {
        return "anthropic";
    }

    @Override
    public List<String> supportedModels() {
        return supportedModels;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        AnthropicChatRequest anthropicRequest = AnthropicChatRequest.from(request, defaultMaxTokens);
        AnthropicChatResponse response = client.chat(anthropicRequest);

        // Anthropic content 是数组，取第一个 text 块
        String content = response.content().stream()
                .filter(b -> "text".equals(b.type()))
                .findFirst()
                .map AnthropicChatResponse.ContentBlock::text
                .orElse("");

        return new ChatResponse(
                content,
                response.model(),
                new TokenUsage(response.usage().inputTokens(), response.usage().outputTokens(),
                        response.usage().inputTokens() + response.usage().outputTokens())
        );
    }

    @Override
    public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
        AnthropicChatRequest anthropicRequest = AnthropicChatRequest.from(request.withStream(true), defaultMaxTokens);

        return Flux.defer(() -> {
            AtomicReference<TokenUsage> pendingUsage = new AtomicReference<>();

            return client.chatStream(anthropicRequest)
                    .filter(chunk -> !chunk.isPing()) // 忽略 ping 事件
                    .flatMap(chunk -> {
                        // 错误事件
                        if (chunk.isError()) {
                            return Flux.error(new DomainException(
                                    com.cartisan.core.exception.BaseCodeMessage.THIRD_PARTY_ERROR,
                                    chunk.getErrorMessage()));
                        }

                        // text delta
                        String delta = chunk.getTextDelta();
                        if (!delta.isEmpty()) {
                            return Flux.just(new ChatStreamEvent(delta, false, null));
                        }

                        // message_delta 包含 usage
                        if (chunk.isMessageDelta()) {
                            AnthropicStreamChunk.Usage usage = chunk.getUsage();
                            if (usage != null) {
                                TokenUsage tokenUsage = new TokenUsage(
                                        usage.inputTokens(),
                                        usage.outputTokens(),
                                        usage.inputTokens() + usage.outputTokens());
                                pendingUsage.set(tokenUsage);
                            }
                            return Flux.empty();
                        }

                        // message_stop: 流结束，发出最终的 finished event
                        if (chunk.isMessageStop()) {
                            TokenUsage usage = pendingUsage.getAndSet(null);
                            return Flux.just(new ChatStreamEvent("", true, usage));
                        }

                        return Flux.empty();
                    });
        });
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :cartisan-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/provider/anthropic/AnthropicProvider.java
git commit -m "feat(cartisan-ai): add AnthropicProvider for F05-07"
```

---

## Task 6: Create AnthropicProviderTest with WireMock

**Files:**
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/provider/anthropic/AnthropicProviderTest.java`

- [ ] **Step 1: Create AnthropicProviderTest**

```java
package com.cartisan.ai.provider.anthropic;

import com.cartisan.ai.model.ChatMessage;
import com.cartisan.ai.model.ChatRequest;
import com.cartisan.ai.model.ChatStreamEvent;
import com.cartisan.ai.model.Role;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class AnthropicProviderTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()))
            .build();

    @Test
    void shouldReturnChatResponse_whenChatSucceeds() {
        // given
        AnthropicProperties properties = new AnthropicProperties();
        properties.setBaseUrl("http://localhost:" + wireMock.getPort());
        properties.setApiKey("test-key");
        properties.setModels(List.of("claude-sonnet-4-20250514"));
        AnthropicProvider provider = new AnthropicProvider(properties);

        stubFor(post(urlEqualTo("/v1/messages"))
                .withHeader("x-api-key", equalTo("test-key"))
                .withHeader("anthropic-version", equalTo("2023-06-01"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "msg_123",
                                    "type": "message",
                                    "role": "assistant",
                                    "content": [{"type": "text", "text": "Hello!"}],
                                    "model": "claude-sonnet-4-20250514",
                                    "stop_reason": "end_turn",
                                    "usage": {"input_tokens": 10, "output_tokens": 5}
                                }
                                """)));

        ChatRequest request = new ChatRequest(
                "claude-sonnet-4-20250514",
                List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, false);

        // when
        var response = provider.chat(request);

        // then
        assertThat(response.content()).isEqualTo("Hello!");
        assertThat(response.model()).isEqualTo("claude-sonnet-4-20250514");
        assertThat(response.usage().promptTokens()).isEqualTo(10);
        assertThat(response.usage().completionTokens()).isEqualTo(5);
        assertThat(response.usage().totalTokens()).isEqualTo(15);
    }

    @Test
    void shouldStreamResponse_whenStreamIsTrue() {
        // given
        AnthropicProperties properties = new AnthropicProperties();
        properties.setBaseUrl("http://localhost:" + wireMock.getPort());
        properties.setApiKey("test-key");
        properties.setModels(List.of("claude-sonnet-4-20250514"));
        AnthropicProvider provider = new AnthropicProvider(properties);

        stubFor(post(urlEqualTo("/v1/messages"))
                .withHeader("x-api-key", equalTo("test-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("""
                                event: message_start
                                data: {"type":"message_start","message":{"id":"msg_123","role":"assistant","content":[]}}

                                event: content_block_start
                                data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

                                event: content_block_delta
                                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}

                                event: content_block_delta
                                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":" World"}}

                                event: content_block_stop
                                data: {"type":"content_block_stop","index":0}

                                event: message_delta
                                data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":10}}

                                event: message_stop
                                data: {"type":"message_stop"}
                                """)));

        ChatRequest request = new ChatRequest(
                "claude-sonnet-4-20250514",
                List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, true);

        // when & then
        StepVerifier.create(provider.chatStream(request))
                .assertNext(event -> {
                    assertThat(event.delta()).isEqualTo("Hello");
                    assertThat(event.finished()).isFalse();
                })
                .assertNext(event -> {
                    assertThat(event.delta()).isEqualTo(" World");
                    assertThat(event.finished()).isFalse();
                })
                .assertNext(event -> {
                    assertThat(event.delta()).isEmpty();
                    assertThat(event.finished()).isTrue();
                    assertThat(event.usage()).isNotNull();
                    assertThat(event.usage().completionTokens()).isEqualTo(10);
                })
                .verifyComplete();
    }

    @Test
    void shouldUseDefaultMaxTokens_whenRequestMaxTokensIsNull() {
        // given
        AnthropicProperties properties = new AnthropicProperties();
        properties.setBaseUrl("http://localhost:" + wireMock.getPort());
        properties.setApiKey("test-key");
        properties.setModels(List.of("claude-sonnet-4-20250514"));
        properties.setDefaultMaxTokens(8192);
        AnthropicProvider provider = new AnthropicProvider(properties);

        stubFor(post(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.max_tokens", equalTo("8192")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "msg_123",
                                    "type": "message",
                                    "role": "assistant",
                                    "content": [{"type": "text", "text": "OK"}],
                                    "model": "claude-sonnet-4-20250514",
                                    "stop_reason": "end_turn",
                                    "usage": {"input_tokens": 10, "output_tokens": 5}
                                }
                                """)));

        ChatRequest request = new ChatRequest(
                "claude-sonnet-4-20250514",
                List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, false);

        // when
        provider.chat(request);

        // then
        verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.max_tokens", equalTo("8192"))));
    }

    @Test
    void shouldThrowException_whenStreamReturnsHttpError() {
        // given
        AnthropicProperties properties = new AnthropicProperties();
        properties.setBaseUrl("http://localhost:" + wireMock.getPort());
        properties.setApiKey("test-key");
        properties.setModels(List.of("claude-sonnet-4-20250514"));
        AnthropicProvider provider = new AnthropicProvider(properties);

        stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "type": "error",
                                    "error": {"type": "invalid_request_error", "message": "Invalid request"}
                                }
                                """)));

        ChatRequest request = new ChatRequest(
                "claude-sonnet-4-20250514",
                List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, true);

        // when & then
        StepVerifier.create(provider.chatStream(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof com.cartisan.core.exception.DomainException &&
                        throwable.getMessage().contains("Invalid request"))
                .verify();
    }

    @Test
    void shouldHandleMalformedSseEvent_gracefully() {
        // given
        AnthropicProperties properties = new AnthropicProperties();
        properties.setBaseUrl("http://localhost:" + wireMock.getPort());
        properties.setApiKey("test-key");
        properties.setModels(List.of("claude-sonnet-4-20250514"));
        AnthropicProvider provider = new AnthropicProvider(properties);

        // 包含格式错误的事件
        stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("""
                                event: message_start
                                data: {"type":"message_start"}

                                event: content_block_delta
                                data: {invalid json here}

                                event: message_stop
                                data: {"type":"message_stop"}
                                """)));

        ChatRequest request = new ChatRequest(
                "claude-sonnet-4-20250514",
                List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, true);

        // when & then - 应该将解析错误转换为 error 事件并终止流
        StepVerifier.create(provider.chatStream(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof com.cartisan.core.exception.DomainException)
                .verify();
    }
}
```

- [ ] **Step 2: Verify tests pass**

Run: `./gradlew :cartisan-ai:test --tests AnthropicProviderTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add cartisan-ai/src/test/java/com/cartisan/ai/provider/anthropic/AnthropicProviderTest.java
git commit -m "test(cartisan-ai): add AnthropicProviderTest for F05-07"
```

---

## Task 7: Final verification

**Files:**
- All files in this feature

- [ ] **Step 1: Run all module tests**

Run: `./gradlew :cartisan-ai:test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run all project tests**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :cartisan-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Final commit if needed**

```bash
git add -A
git commit -m "chore(cartisan-ai): final cleanup for F05-07 AnthropicProvider"
```

---

## Completion Checklist

- [ ] All tasks completed
- [ ] All tests passing (`./gradlew :cartisan-ai:test`)
- [ ] No compilation warnings
- [ ] Code follows project conventions (Java Record, RestClient/WebClient pattern)
- [ ] Ready for F05-09 (auto-configuration integration)

---

## Next Steps

After F05-07 completion:
1. Update `docs/specs/epic-05-ai/00_epic_backlog.md` to mark F05-07 as done
2. Proceed to F05-08 (SSE 流式工具) or F05-09 (自动配置)
