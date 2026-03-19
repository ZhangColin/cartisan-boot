# F05-03 Provider SPI 设计文档

**日期**：2026-03-19
**Feature**：F05-03 Provider SPI
**状态**：待实现

---

## 背景

F05-02 已完成统一对话模型（`ChatRequest`、`ChatResponse`、`ChatStreamEvent`、`TokenUsage` 等）。F05-03 基于这些类型，定义 Provider SPI——描述"一个 AI 模型提供商应该实现什么契约"。后续的 OpenAI、DeepSeek、Anthropic Provider 均实现此接口。

---

## 设计决策

### 1. 纯接口，不提供默认实现

`ModelProvider` 是纯抽象接口，4 个方法全部为抽象方法，不提供 `default` 实现。

**理由**：Epic-05 计划的三个 Provider（OpenAI、DeepSeek、Anthropic）全部支持同步和流式调用。为"不支持流式的 Provider"预留 `default` 实现属于过度设计（YAGNI）。接口即契约，实现者承诺同时支持两种调用方式。

**排除的替代方案**：
- 带 `supportsStream()` 默认方法：把类型系统能解决的问题变成运行时检查，调用方容易遗漏
- 拆分 `StreamingModelProvider`：导致 Registry 中需要 `instanceof` 判断，增加复杂度

### 2. 新增 `ModelUsageListener` 扩展点，支持计费

每次 AI 调用均消耗 Token，需要支持计费统计。采用 Listener SPI 模式（参考 LangChain4j `ChatModelListener`），与 `ModelProvider` 接口解耦：

- `ModelProvider` 保持纯净，Provider 实现者无感知
- 业务层注册 `ModelUsageListener` Bean 即可自动收集用量
- 触发逻辑由 F05-04 `ModelProviderRegistry` 负责（对 Provider 调用结果拦截后触发）

`TokenUsage` 数据来源：
- 同步调用：`ChatResponse.usage()`
- 流式调用：最后一个 `ChatStreamEvent.usage()`（F05-02 已设计）

---

## 交付物

### `ModelProvider.java`

```
com.cartisan.ai.provider.ModelProvider
```

| 方法 | 签名 | 说明 |
|------|------|------|
| `id` | `String id()` | 提供商标识，如 `openai` / `anthropic` / `deepseek` |
| `supportedModels` | `List<String> supportedModels()` | 支持的模型名称列表 |
| `chat` | `ChatResponse chat(ChatRequest request)` | 同步对话调用 |
| `chatStream` | `Flux<ChatStreamEvent> chatStream(ChatRequest request)` | 流式对话调用 |

### `ModelUsageListener.java`

```
com.cartisan.ai.provider.ModelUsageListener
```

函数式接口，计费/用量统计扩展点：

```java
@FunctionalInterface
public interface ModelUsageListener {
    void onUsage(String providerId, String model, TokenUsage usage);
}
```

**参数说明**：
- `providerId`：来自 `ModelProvider.id()`
- `model`：来自 `ChatResponse.model()`（服务端确认的实际模型名，而非请求中的 `ChatRequest.model()`；在代理/路由场景下两者可能不同）
- `usage`：本次调用消耗的 Token 明细

---

## 测试策略

使用 `FakeModelProvider implements ModelProvider` 验证接口契约完整性：

| 测试场景 | 验证点 |
|----------|--------|
| `shouldReturnNonBlankId` | `id()` 返回非空字符串 |
| `shouldReturnNonEmptySupportedModels` | `supportedModels()` 返回非空列表 |
| `shouldReturnValidChatResponse` | `chat()` 返回的 `ChatResponse` 中 content/model/usage 均非 null |
| `shouldReturnStreamWithFinishedEvent` | `chatStream()` 返回的事件序列，最后一个事件满足：① `finished == true`；② `usage != null`（两个条件独立断言） |

`ModelUsageListener` 是函数式接口，自身无逻辑，在 F05-04 集成测试中覆盖。

---

## 边界说明

| 包含 | 不包含 |
|------|--------|
| `ModelProvider` 接口定义 | 任何具体 Provider 实现（F05-05 ~ F05-07） |
| `ModelUsageListener` 接口定义 | Listener 触发逻辑（F05-04 Registry 负责） |
| 接口契约测试 | HTTP 客户端、SSE 解析（后续 Feature） |

---

## 依赖关系

- **上游**：F05-02（`ChatRequest`、`ChatResponse`、`ChatStreamEvent`、`TokenUsage`）
- **下游**：F05-04（Registry）、F05-05（OpenAI）、F05-06（DeepSeek）、F05-07（Anthropic）
