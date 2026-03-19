# F05-02 统一对话模型 — 设计文档

**Feature**：F05-02
**Epic**：Epic 5 — cartisan-ai
**日期**：2026-03-19
**状态**：已审批，待实现

---

## 背景与目标

为 cartisan-ai 模块建立统一的对话模型层（`com.cartisan.ai.model` 包），屏蔽各家 LLM Provider（OpenAI、Anthropic、DeepSeek、GLM、Kimi、Doubao、Gemini 等）的 API 差异，为上层 Provider SPI（F05-03）提供统一类型契约。

---

## 设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| `ChatRequest` 构造方式 | Builder 模式 | 可选字段多（temperature、maxTokens 等），Builder 比多参数构造函数可读性高 |
| System prompt 位置 | `ChatRequest` 独立字段 | Anthropic / Gemini 将 system 作为顶层字段，独立字段映射更自然；Provider 层各自适配 |
| `Role` 枚举值 | `USER` / `ASSISTANT` 两个值 | system 由 `systemPrompt` 字段承载，不需要 SYSTEM role |
| 消息内容类型 | `sealed interface MessageContent` | 支持多模态（文本 + 图片），sealed 允许编译器穷举，pattern matching 友好 |
| 流式事件可选 usage | `@Nullable TokenUsage` | `Optional` 作为 Record 字段有 Jackson 序列化陷阱；`finished` 字段已足够指示 usage 时机 |
| `stream` 字段 | 不在 `ChatRequest` 中 | 同步/流式由调用方法决定（`chat()` vs `chatStream()`），字段冗余且有歧义 |
| `TokenUsage` 字段类型 | `long` | Gemini 1.5 Pro 上下文窗口可达 100 万 token，`int`（约 21 亿上限）当前够用，但 `long` 零成本且面向未来 |
| `@Nullable` 注解来源 | `org.springframework.lang.Nullable` | `spring-webflux` 已传递依赖此包，无需额外引入；整个模块统一使用此注解 |
| 工具调用（tool use）支持 | 本 Feature 范围外 | F05-02 只覆盖纯文本对话模型；工具调用涉及独立类型设计，推迟到后续 Feature |

---

## 类型设计

### 包位置

所有类型位于 `com.cartisan.ai.model`。

### 类型关系

```
MessageContent (sealed interface)
  ├── TextContent (static record)
  └── ImageContent (static record)

Role (enum)                          USER, ASSISTANT

ChatMessage (record)                 role: Role, content: List<MessageContent>

TokenUsage (record)                  promptTokens: long, completionTokens: long, totalTokens: long

ChatRequest (class + Builder)        model, systemPrompt?, messages (>=1), temperature?, maxTokens?

ChatResponse (record)                content: String, model: String, finishReason: String, usage: TokenUsage

ChatStreamEvent (record)             delta: String (never null), finished: boolean, usage: @Nullable TokenUsage
```

---

### MessageContent

> **注意**：sealed interface 中的 record 实现类（`TextContent`、`ImageContent`）在 Java 中是隐式 `static` 的，不需要显式加 `static` 关键字，但语义等同于静态内部类。

```java
public sealed interface MessageContent
        permits MessageContent.TextContent, MessageContent.ImageContent {

    // TextContent 和 ImageContent 是隐式 static record
    record TextContent(String text) implements MessageContent {
        public TextContent {
            Objects.requireNonNull(text, "text cannot be null");
        }
    }

    /**
     * 图片内容。
     *
     * urlOrDataUri 字段支持两种形式：
     *   - HTTP/HTTPS URL：如 "https://example.com/image.jpg"（OpenAI、Anthropic URL 模式）
     *   - Base64 Data URI：如 "data:image/jpeg;base64,..."（OpenAI、Anthropic base64 模式）
     *
     * Provider 层负责解析此字段并映射到各家 API 要求的格式：
     *   - OpenAI：统一放入 image_url.url，两种形式均支持
     *   - Anthropic：URL 模式用 source.type="url"，base64 模式用 source.type="base64" + source.data
     *
     * mediaType（如 "image/jpeg"）在 Anthropic base64 模式下为必填，
     * URL 模式和 OpenAI 可忽略此字段（传 null 即可）。
     */
    record ImageContent(String urlOrDataUri, @Nullable String mediaType) implements MessageContent {
        public ImageContent {
            Objects.requireNonNull(urlOrDataUri, "urlOrDataUri cannot be null");
        }
    }

    static MessageContent text(String text) { return new TextContent(text); }
    static MessageContent image(String urlOrDataUri) { return new ImageContent(urlOrDataUri, null); }
    static MessageContent image(String urlOrDataUri, String mediaType) {
        return new ImageContent(urlOrDataUri, mediaType);
    }
}
```

---

### Role

```java
public enum Role {
    USER, ASSISTANT
}
```

---

### ChatMessage

```java
public record ChatMessage(Role role, List<MessageContent> content) {

    public ChatMessage {
        Objects.requireNonNull(role, "role cannot be null");
        content = List.copyOf(Objects.requireNonNull(content, "content cannot be null"));
    }

    // 纯文本场景工厂方法
    public static ChatMessage userText(String text) {
        return new ChatMessage(Role.USER, List.of(MessageContent.text(text)));
    }

    public static ChatMessage assistantText(String text) {
        return new ChatMessage(Role.ASSISTANT, List.of(MessageContent.text(text)));
    }

    // 多模态场景工厂方法
    public static ChatMessage user(List<MessageContent> content) {
        return new ChatMessage(Role.USER, content);
    }

    public static ChatMessage assistant(List<MessageContent> content) {
        return new ChatMessage(Role.ASSISTANT, content);
    }
}
```

- `List.copyOf()` 保证不可变性
- `userText` / `assistantText` 覆盖纯文本场景（约 90% 用例）
- `user(List)` / `assistant(List)` 覆盖多模态场景（文本 + 图片混合）

---

### TokenUsage

```java
public record TokenUsage(long promptTokens, long completionTokens, long totalTokens) {}
```

使用 `long` 而非 `int`，面向 Gemini 等百万级 token 上下文窗口。

---

### ChatRequest

```java
public final class ChatRequest {

    private final String model;
    private final @Nullable String systemPrompt;
    private final List<ChatMessage> messages;
    private final @Nullable Double temperature;
    private final @Nullable Integer maxTokens;

    private ChatRequest(Builder builder) {
        this.model = Objects.requireNonNull(builder.model, "model cannot be null");
        if (builder.messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        this.systemPrompt = builder.systemPrompt;
        this.messages = List.copyOf(builder.messages);
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
    }

    public static Builder builder() { return new Builder(); }

    // getters ...

    public static final class Builder {
        private String model;
        private String systemPrompt;
        private final List<ChatMessage> messages = new ArrayList<>();
        private Double temperature;
        private Integer maxTokens;

        public Builder model(String model)              { this.model = model; return this; }
        public Builder systemPrompt(String prompt)      { this.systemPrompt = prompt; return this; }
        public Builder message(ChatMessage message)     { this.messages.add(message); return this; }
        public Builder messages(List<ChatMessage> msgs) { this.messages.addAll(msgs); return this; }
        public Builder temperature(double temperature)  { this.temperature = temperature; return this; }
        public Builder maxTokens(int maxTokens)         { this.maxTokens = maxTokens; return this; }

        public ChatRequest build() { return new ChatRequest(this); }
    }
}
```

**典型用法：**
```java
ChatRequest.builder()
    .model("gpt-4o")
    .systemPrompt("你是一个助手")
    .message(ChatMessage.userText("你好"))
    .temperature(0.7)
    .build();
```

---

### ChatResponse

```java
public record ChatResponse(String content, String model, String finishReason, TokenUsage usage) {

    public ChatResponse {
        Objects.requireNonNull(content, "content cannot be null");
        Objects.requireNonNull(model, "model cannot be null");
        Objects.requireNonNull(finishReason, "finishReason cannot be null");
        Objects.requireNonNull(usage, "usage cannot be null");
    }
}
```

`finishReason` 常见值（由 Provider 层映射后填入统一值）：

| 统一值 | 语义 |
|--------|------|
| `"stop"` | 正常完成 |
| `"length"` | 达到 maxTokens 截断 |
| `"content_filter"` | 内容过滤 |

> **范围说明**：工具调用（tool use / function calling）返回值不在本 Feature 范围内，后续 Feature 独立设计相关类型。`content` 字段当前只承载纯文本内容。

---

### ChatStreamEvent

```java
public record ChatStreamEvent(String delta, boolean finished, @Nullable TokenUsage usage) {

    public ChatStreamEvent {
        Objects.requireNonNull(delta, "delta cannot be null");
        if (!finished && usage != null) {
            throw new IllegalArgumentException("usage should only be present on the final event");
        }
    }

    public static ChatStreamEvent delta(String delta) {
        return new ChatStreamEvent(delta, false, null);
    }

    public static ChatStreamEvent finish(TokenUsage usage) {
        return new ChatStreamEvent("", true, usage);
    }

    public static ChatStreamEvent finishWithoutUsage() {
        return new ChatStreamEvent("", true, null);
    }
}
```

**`delta` 字段契约：**
- 普通流式事件：`delta` 为非空文本片段（可以是空字符串 `""`，流开始时某些 Provider 会发一个空 delta）
- 终止事件（`finished=true`）：`delta` **始终为空字符串 `""`**，不会为 `null`
- 调用方拼接内容时：只需累加所有 `delta`，终止事件的空字符串不影响结果

**不变量：**
- 非结束事件（`finished=false`）不能携带 `usage`
- `finishWithoutUsage()` 处理部分国内 Provider（如某些 DeepSeek / GLM 流式版本）不返回 token 用量的情况

---

## 测试策略

| 测试类 | 覆盖内容 |
|--------|---------|
| `MessageContentTest` | `TextContent` / `ImageContent` 构造及静态工厂；null 检查 |
| `ChatMessageTest` | `List.copyOf()` 不可变性；四种工厂方法（userText / assistantText / user / assistant） |
| `TokenUsageTest` | Record 构造、equals/hashCode |
| `ChatRequestTest` | Builder 正常构建；`model=null` 抛异常；`messages` 为空抛异常；`message()` 与 `messages()` 累加 |
| `ChatResponseTest` | 全字段非 null 约束；`finishReason` 非 null 约束 |
| `ChatStreamEventTest` | delta 事件 `usage=null`；`finished=false` 时携带 usage 抛异常；`finish()` 和 `finishWithoutUsage()` 工厂方法；delta 字段 null 检查 |

命名规范：`given_{条件}_when_{操作}_then_{预期结果}`，使用 AssertJ。

---

## 文件清单

```
cartisan-ai/src/main/java/com/cartisan/ai/model/
  Role.java
  MessageContent.java
  ChatMessage.java
  TokenUsage.java
  ChatRequest.java
  ChatResponse.java
  ChatStreamEvent.java

cartisan-ai/src/test/java/com/cartisan/ai/model/
  MessageContentTest.java
  ChatMessageTest.java
  TokenUsageTest.java
  ChatRequestTest.java
  ChatResponseTest.java
  ChatStreamEventTest.java
```

---

## 依赖

- 无外部依赖，纯 Java 21 标准库
- `@Nullable` 使用 `org.springframework.lang.Nullable`（由 `spring-webflux` 传递依赖，无需额外引入）
- 与 F05-01 模块骨架已有的包结构对齐
- 为 F05-03 Provider SPI 提供方法签名所需的全部类型
