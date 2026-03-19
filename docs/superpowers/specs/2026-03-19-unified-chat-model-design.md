# F05-02: 统一对话模型 — 设计文档

> **Feature**: F05-02 统一对话模型
> **Epic**: Epic 5 — AI 模块
> **复杂度**: S（60-100 行）
> **依赖**: F05-01（模块骨架，已完成）

---

## 目标

在 `com.cartisan.ai.model` 包下创建统一对话模型类型，作为整个 AI 模块的公共契约。所有类型为纯 Java Record，零框架依赖，供后续 Provider SPI、Registry、SSE 工具等使用。

## 设计决策

| 决策 | 选项 | 结论 |
|------|------|------|
| 可选字段处理 | A) 引用类型 + null / B) Optional | A — null 表示"不传此字段"，与 JSON 序列化天然兼容 |
| ChatRequest 构建方式 | A) 纯构造函数 / B) Builder | A — 保持简单，框架内部使用无需便捷构建 |
| Token 计数类型 | int 原始类型 | token 数必定存在且非负，无需 null 语义 |
| messages 不可变性 | 紧凑构造函数中 List.copyOf | 防御性拷贝，保证 Record 不可变 |

## 类型设计

### Role

```java
public enum Role {
    SYSTEM, USER, ASSISTANT
}
```

简单标记枚举，无额外字段。

### ChatMessage

```java
public record ChatMessage(Role role, String content) {
    public ChatMessage {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}
```

### TokenUsage

```java
public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
    public TokenUsage {
        if (promptTokens < 0 || completionTokens < 0 || totalTokens < 0) {
            throw new IllegalArgumentException("Token counts must not be negative");
        }
    }
}
```

### ChatRequest

```java
public record ChatRequest(
    String model,
    List<ChatMessage> messages,
    Double temperature,
    Integer maxTokens,
    boolean stream
) {
    public ChatRequest {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(messages, "messages must not be null");
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        messages = List.copyOf(messages);
    }
}
```

- `temperature` / `maxTokens`：可选，`null` 表示不设置
- `stream`：`boolean` 原始类型，默认 `false`

### ChatResponse

```java
public record ChatResponse(String content, String model, TokenUsage usage) {
    public ChatResponse {
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(usage, "usage must not be null");
    }
}
```

### ChatStreamEvent

```java
public record ChatStreamEvent(String delta, boolean finished, TokenUsage usage) {
    public ChatStreamEvent {
        Objects.requireNonNull(delta, "delta must not be null");
        // usage 仅在 finished=true 时携带，其余时候为 null
    }
}
```

- `delta`：增量文本，非 null（无内容时为空字符串）
- `finished`：是否为最后一个事件
- `usage`：仅终止事件携带，其余为 `null`

## 包结构

```
com.cartisan.ai.model/
├── Role.java
├── ChatMessage.java
├── TokenUsage.java
├── ChatRequest.java
├── ChatResponse.java
└── ChatStreamEvent.java
```

## 测试策略

- 各 Record 紧凑构造函数校验：null 输入 → `NullPointerException`
- `ChatRequest.messages` 防御性拷贝：外部修改原 List 不影响 Record 内部
- `TokenUsage` 负数校验 → `IllegalArgumentException`
- Record 的 equals/hashCode 正确性验证

## 约束

- 零框架依赖：不引入 Spring、JPA 或任何第三方注解
- 遵循项目 Record 惯例：紧凑构造函数 + `Objects.requireNonNull`
- 所有类型不可变
