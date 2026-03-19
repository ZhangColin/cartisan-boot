# F05-06 DeepSeek Provider 设计文档

**日期**：2026-03-20
**范围**：F05-06 DeepSeek Provider + OpenAiProvider 重构

---

## 背景

DeepSeek 兼容 OpenAI 的 HTTP API 协议（`/v1/chat/completions`、请求/响应格式、SSE 流式格式）。F05-05 已实现的 `OpenAiClient` 本质上是"OpenAI 协议的 HTTP 客户端"，与 OpenAI 这家公司没有强绑定。

F05-06 不只是新增一个 `DeepSeekProvider`，而是借此机会将协议层抽象出来：引入 `OpenAiCompatibleProvider` 抽象基类，让 `OpenAiProvider` 和 `DeepSeekProvider` 共同继承，消除代码重复，并为后续其他 OpenAI 兼容提供商（如 Groq、Together AI 等）提供扩展点。

---

## 设计决策

### 1. 继承 vs 组合（对 Backlog 的偏离说明）

原 `00_epic_backlog.md` F05-06 条目写明"组合而非继承"。本设计选择**继承**，理由如下：

`DeepSeekProvider` 在 `ModelProvider` SPI 范围内完全兼容 OpenAI 协议，`is-a` 关系成立。组合会引入一个无意义的委托层（`DeepSeekProvider` 持有 `OpenAiCompatibleProvider` 并逐一转发 `chat`/`chatStream`），增加间接性而无收益。继承在此更能直接表达"DeepSeek 是 OpenAI 协议的一种具体实现"这一语义。

此决策经过充分讨论后确认，Backlog 中的原始指导已被本设计文档取代。

### 2. 抽象类 vs 具体类

`OpenAiCompatibleProvider` 设计为**抽象类**。`id()` 和 `supportedModels()` 与具体提供商绑定，基类没有合理默认值，强制子类实现更安全。同时防止直接实例化一个没有明确身份的 Provider 混入 Registry。

### 3. 包结构

将协议层提升到独立包 `openaicompat/`，`openai/` 和 `deepseek/` 只放各自的具体子类。避免 `deepseek` 包依赖 `openai` 包内部实现。

### 4. DTO 类名保留 `OpenAi` 前缀

`OpenAiChatRequest`、`OpenAiChatResponse`、`OpenAiStreamChunk`、`OpenAiErrorResponse` 迁入 `openaicompat/` 后保留原名。这些类直接对应 OpenAI 的 wire format，改名反而模糊来源；DeepSeek 兼容的正是这套格式，保留前缀准确表达了这层含义。

---

## 包结构

```
provider/
├── openaicompat/
│   ├── package-info.java
│   ├── OpenAiCompatibleProvider.java   # 抽象基类，实现 chat/chatStream 逻辑
│   ├── OpenAiCompatibleClient.java     # HTTP 客户端（原 OpenAiClient 改名迁移）
│   ├── OpenAiChatRequest.java          # 迁移自 openai/
│   ├── OpenAiChatResponse.java         # 迁移自 openai/
│   ├── OpenAiStreamChunk.java          # 迁移自 openai/
│   └── OpenAiErrorResponse.java        # 迁移自 openai/
├── openai/
│   ├── OpenAiProvider.java             # extends OpenAiCompatibleProvider（重构）
│   └── OpenAiProperties.java           # 不变
└── deepseek/
    ├── package-info.java
    ├── DeepSeekProvider.java           # extends OpenAiCompatibleProvider（新建）
    └── DeepSeekProperties.java         # 新建
```

---

## 各组件设计

### OpenAiCompatibleProvider（抽象基类）

```
抽象方法：
  id() → String
  supportedModels() → List<String>

实现方法（从 OpenAiProvider 迁移）：
  chat(ChatRequest) → ChatResponse
  chatStream(ChatRequest) → Flux<ChatStreamEvent>

构造：
  OpenAiCompatibleProvider(OpenAiCompatibleClient client)   // 供子类 & 测试用
```

### OpenAiCompatibleClient

原 `OpenAiClient` 改名，包路径从 `openai/` 迁移至 `openaicompat/`，逻辑不变。

### OpenAiProvider（重构）

- `extends OpenAiCompatibleProvider`
- 只覆盖 `id()` 返回 `"openai"`，`supportedModels()` 返回配置的模型列表
- 构造函数接收 `OpenAiProperties`，提供 package-private 测试构造（接收 `OpenAiCompatibleClient` + `List<String>`）

### DeepSeekProperties

- `@ConfigurationProperties("cartisan.ai.deepseek")`
- 字段：`apiKey`、`baseUrl`（默认 `https://api.deepseek.com/v1`）、`models`
- 默认模型列表：`deepseek-chat`、`deepseek-reasoner`

### DeepSeekProvider

- `extends OpenAiCompatibleProvider`
- `id()` 返回 `"deepseek"`
- `supportedModels()` 返回配置的模型列表
- 构造函数接收 `DeepSeekProperties`，提供 package-private 测试构造

---

## 测试策略

| 测试类 | 类型 | 覆盖内容 |
|--------|------|----------|
| `OpenAiCompatibleClientTest` | WireMock | 原 `OpenAiClientTest` 改名，逻辑不变 |
| `OpenAiProviderTest` | Mockito | import 路径更新（见下方迁移清单），逻辑不变 |
| `DeepSeekProviderTest` | Mockito | 仅覆盖 Provider 身份：`id()`、`supportedModels()`；`chat`/`chatStream` 协议逻辑由基类承载，已由 `OpenAiProviderTest` 覆盖，无需重复 |

### OpenAiProviderTest / OpenAiClientTest 迁移清单

重构后以下类型的 import 路径需从 `provider.openai.*` 更新为 `provider.openaicompat.*`：

- `OpenAiClient` → `OpenAiCompatibleClient`
- `OpenAiChatRequest`
- `OpenAiChatResponse`
- `OpenAiStreamChunk`
- `OpenAiErrorResponse`

测试构造中的 `mock(OpenAiClient.class)` 需同步改为 `mock(OpenAiCompatibleClient.class)`。

---

## 不在本次范围内

- F05-07 Anthropic Provider（独立协议，单独设计）
- F05-08 SSE 工具
- F05-09 自动配置（DeepSeek Bean 注册留至 F05-09 统一处理）
- 能力接口扩展（`ImageProvider` 等），留待后续 Epic
