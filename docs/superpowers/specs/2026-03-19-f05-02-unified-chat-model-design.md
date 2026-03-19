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

---

## 类型设计

### 包位置

所有类型位于 `com.cartisan.ai.model`。

### 类型关系

```
MessageContent (sealed interface)
  ├── TextContent (record)
  └── ImageContent (record)

Role (enum)                          USER, ASSISTANT

ChatMessage (record)                 role: Role, content: List<MessageContent>

TokenUsage (record)                  promptTokens, completionTokens, totalTokens

ChatRequest (class + Builder)        model, systemPrompt?, messages, temperature?, maxTokens?

ChatResponse (record)                content: String, model: String, usage: TokenUsage

ChatStreamEvent (record)             delta: String, finished: boolean, usage: @Nullable TokenUsage
```

---

### MessageContent

```java
public sealed interface MessageContent
        permits MessageContent.TextContent, MessageContent.ImageContent {

    record TextContent(String text) implements MessageContent {
        public TextContent {
            Objects.requireNonNull(text, "text cannot be null");
        }
    }

    record ImageContent(String url, @Nullable String mediaType) implements MessageContent {
        public ImageContent {
            Objects.requireNonNull(url, "url cannot be null");
        }
    }

    static MessageContent text(String text)  { return new TextContent(text); }
    static MessageContent image(String url)  { return new ImageContent(url, null); }
    static MessageContent image(String url, String mediaType) {
        return new ImageContent(url, mediaType);
    }
}
```

- `mediaType` 字段（如 `"image/jpeg"`）供 Anthropic API 使用，OpenAI Provider 可忽略
- 静态工厂方法简化调用方代码

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

    public static ChatMessage userText(String text) {
        return new ChatMessage(Role.USER, List.of(MessageContent.text(text)));
    }

    public static ChatMessage assistantText(String text) {
        return new ChatMessage(Role.ASSISTANT, List.of(MessageContent.text(text)));
    }

    public static ChatMessage user(List<MessageContent> content) {
        return new ChatMessage(Role.USER, content);
    }
}
```

- `List.copyOf()` 保证不可变性
- `userText` / `assistantText` 覆盖纯文本场景，`user(List)` 覆盖多模态场景

---

### TokenUsage

```java
public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {}
```

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
public record ChatResponse(String content, String model, TokenUsage usage) {

    public ChatResponse {
        Objects.requireNonNull(content, "content cannot be null");
        Objects.requireNonNull(model, "model cannot be null");
        Objects.requireNonNull(usage, "usage cannot be null");
    }
}
```

- 同步调用结果，`usage` 非 null（Provider 必须填充）

---

### ChatStreamEvent

```java
public record ChatStreamEvent(String delta, boolean finished, @Nullable TokenUsage usage) {

    public ChatStreamEvent {
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

- 不变量：非结束事件不能携带 usage
- `finishWithoutUsage()` 处理部分国内 Provider 不返回 token 用量的情况

---

## 测试策略

| 测试类 | 覆盖内容 |
|--------|---------|
| `ChatMessageTest` | `List.copyOf()` 不可变性；`userText` / `assistantText` 工厂方法 |
| `ChatRequestTest` | Builder 构建，必填字段 null 检查，`message()` 与 `messages()` 累加 |
| `ChatResponseTest` | 全字段非 null 约束 |
| `ChatStreamEventTest` | delta 事件无 usage；`finished=false` 时携带 usage 抛异常；终止事件两种工厂方法 |
| `MessageContentTest` | `TextContent` / `ImageContent` 构造及静态工厂 |

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
  ChatRequestTest.java
  ChatResponseTest.java
  ChatStreamEventTest.java
```

---

## 依赖

- 无外部依赖，纯 Java 21 标准库
- 与 F05-01 模块骨架已有的包结构对齐
- 为 F05-03 Provider SPI 提供方法签名所需的全部类型
