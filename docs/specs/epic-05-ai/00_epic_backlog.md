# Epic 5: AI — Backlog

> **Epic 目标**：构建 cartisan-ai 模块，提供大模型调用统一抽象——统一对话模型、Provider SPI、OpenAI/Anthropic/DeepSeek 实现、SSE 流式工具、Spring Boot 自动配置
>
> **依赖**：Epic 1（cartisan-core 已完成 AggregateRoot、DomainEvent、CodeMessage 等基础类型）
>
> **复杂度**：M（Medium，单模块，含 HTTP 客户端集成与流式处理）

---

## Feature 列表

| ID | Feature | 描述 | 复杂度 | 预估代码量 |
|----|---------|------|--------|-----------|
| **F05-01** | 模块骨架 | `cartisan-ai/build.gradle.kts`，注册到 `settings.gradle.kts`，包结构初始化 | S | 30-50 行 |
| **F05-02** | 统一对话模型 | `ChatMessage`、`ChatRequest`、`ChatResponse`、`TokenUsage`、`ChatStreamEvent`（纯 Java Record） | S | 60-100 行 |
| **F05-03** | Provider SPI | `ModelProvider` 接口（`chat`、`chatStream`、`id`、`supportedModels`） | S | 20-40 行 |
| **F05-04** | ModelProviderRegistry | 扫描所有 `ModelProvider` Bean，按 providerId / modelName 查找 | S | 60-80 行 |
| **F05-05** | OpenAI Provider | `OpenAiProvider` 实现：同步调用 + 流式 SSE 解析，支持 base-url 代理 | M | 150-200 行 |
| **F05-06** | DeepSeek Provider | `DeepSeekProvider` 实现：兼容 OpenAI 协议，复用 F05-05 的 HTTP 客户端结构 | S | 40-60 行 |
| **F05-07** | Anthropic Provider | `AnthropicProvider` 实现：独立 API 协议，`anthropic-version` Header，`content_block_delta` 流格式 | M | 150-200 行 |
| **F05-08** | SSE 流式工具 | `SseHelper`：`Flux<ChatStreamEvent>` → `SseEmitter`，封装超时/异常/客户端断开 | M | 80-120 行 |
| **F05-09** | 自动配置 | `CartisanAiAutoConfiguration`，`@ConditionalOnProperty` 条件装配各 Provider Bean | S | 80-120 行 |

---

## 依赖关系

```
F05-01（模块骨架）
    └──→ F05-02（统一对话模型）
              └──→ F05-03（Provider SPI）
                        ├──→ F05-04（Registry）────────────────────┐
                        ├──→ F05-05（OpenAI Provider）             │
                        │         └──→ F05-06（DeepSeek Provider） ├──→ F05-09（自动配置）
                        └──→ F05-07（Anthropic Provider）──────────┤
    F05-02 ──→ F05-08（SSE 工具，依赖 ChatStreamEvent）────────────┘
```

**关键依赖说明：**
- `F05-03` 依赖 `F05-02`：Provider SPI 的方法签名使用统一对话模型类型
- `F05-06` 依赖 `F05-05`：DeepSeek 兼容 OpenAI 协议，复用其 HTTP 客户端与响应解析逻辑
- `F05-08` 只依赖 `F05-02`（`ChatStreamEvent`），与各 Provider 实现**互相独立**，可并行开发
- `F05-09` 依赖全部：自动配置需要装配所有 Provider Bean 和 Registry

---

## 推荐开发顺序

### 批次 1（串行）— 地基

| Feature | 理由 |
|---------|------|
| F05-01 | 无依赖，先建模块骨架 |
| F05-02 | 纯 Java Record，无框架依赖 |
| F05-03 | Provider SPI 接口，锁定契约 |

### 批次 2（并行）— 核心能力

| Feature | 依赖 | 说明 |
|---------|------|------|
| F05-04 | F05-03 | Registry 逻辑简单，可先完成 |
| F05-05 | F05-03 | **关键路径**：OpenAI Provider，流式处理是技术难点 |
| F05-08 | F05-02 | SSE 工具与 Provider 无耦合，可并行开发 |

### 批次 3（串行）— 扩展 Provider

| Feature | 依赖 | 说明 |
|---------|------|------|
| F05-06 | F05-05 | 复用 OpenAI HTTP 客户端结构，工作量小 |
| F05-07 | F05-03 | Anthropic 独立协议，参照 F05-05 结构实现 |

### 批次 4（收尾）— 统一配置

| Feature | 依赖 | 说明 |
|---------|------|------|
| F05-09 | 以上全部 | Spring Boot AutoConfiguration，零配置引入 |

---

## 各 Feature 详细说明

### F05-01: 模块骨架

**交付物：**
- `cartisan-ai/build.gradle.kts`
  - 依赖：`cartisan-core`、`spring-webflux`（Reactor，用于 `Flux`）、`spring-boot-starter`
- `settings.gradle.kts` 中追加 `include("cartisan-ai")`
- 包结构：`com.cartisan.ai/{model,provider,sse,config}`（空包，含 `package-info.java`）

**测试：**
- 验证模块可正常编译（`./gradlew :cartisan-ai:compileJava`）

---

### F05-02: 统一对话模型

**交付物：**
- `ChatMessage.java`（Record）：`role: Role`、`content: String`
- `Role.java`（枚举）：`SYSTEM / USER / ASSISTANT`
- `ChatRequest.java`：`model`、`messages`、`temperature`（可选）、`maxTokens`（可选）、`stream: boolean`
- `ChatResponse.java`（Record）：`content`、`model`、`usage: TokenUsage`
- `TokenUsage.java`（Record）：`promptTokens`、`completionTokens`、`totalTokens`
- `ChatStreamEvent.java`（Record）：`delta: String`、`finished: boolean`、`usage: TokenUsage`（仅最后一个 event 携带）

**测试：**
- 单元测试验证 Record 不可变性与 equals/hashCode
- 验证 `ChatStreamEvent` 终止 event 携带 usage

---

### F05-03: Provider SPI

**交付物：**
- `ModelProvider.java`（接口）
  - `id() → String`：提供商标识（`openai` / `anthropic` / `deepseek`）
  - `supportedModels() → List<String>`：支持的模型列表
  - `chat(ChatRequest) → ChatResponse`：同步调用
  - `chatStream(ChatRequest) → Flux<ChatStreamEvent>`：流式调用

**测试：**
- 使用 Mock 实现验证接口契约完整性

---

### F05-04: ModelProviderRegistry

**交付物：**
- `ModelProviderRegistry.java`
  - 构造时注入所有 `ModelProvider` Bean（`List<ModelProvider>`）
  - `getProvider(providerId: String) → ModelProvider`：按提供商 ID 查找
  - `getProviderByModel(modelName: String) → ModelProvider`：按模型名查找
  - `listProviders() → List<ModelProvider>`
  - 未找到时抛 `CartisanException(NOT_FOUND)`

**测试：**
- 单元测试：多个 Mock Provider 注入，验证查找正确性
- 验证未知 providerId / modelName 抛出正确异常

---

### F05-05: OpenAI Provider

**交付物：**
- `OpenAiProvider.java`（实现 `ModelProvider`）
  - `id()` 返回 `"openai"`
  - 使用 `RestClient` 发起同步调用（`/v1/chat/completions`）
  - 使用 `WebClient` 发起流式调用（SSE 解析 `data:` 行，处理 `[DONE]`）
  - 支持 `base-url` 可配置（兼容代理/Azure OpenAI）
  - HTTP 错误映射到 `CartisanException`
- `OpenAiProperties.java`（`@ConfigurationProperties("cartisan.ai.openai")`）
  - `apiKey`、`baseUrl`（默认 `https://api.openai.com/v1`）

**测试：**
- 使用 WireMock 模拟 OpenAI API
- 测试同步调用正确解析响应
- 测试流式调用正确拼接 delta、携带最终 usage
- 测试 HTTP 4xx/5xx 映射到 `CartisanException`

---

### F05-06: DeepSeek Provider

**交付物：**
- `DeepSeekProvider.java`（实现 `ModelProvider`）
  - `id()` 返回 `"deepseek"`
  - 复用 `OpenAiProvider` 的 HTTP 客户端和 SSE 解析逻辑（组合而非继承）
  - `base-url` 默认指向 `https://api.deepseek.com/v1`
- `DeepSeekProperties.java`（`@ConfigurationProperties("cartisan.ai.deepseek")`）
  - `apiKey`、`baseUrl`

**测试：**
- 使用 WireMock 验证正确调用 DeepSeek 端点
- 验证 `supportedModels()` 返回 DeepSeek 模型列表

---

### F05-07: Anthropic Provider

**交付物：**
- `AnthropicProvider.java`（实现 `ModelProvider`）
  - `id()` 返回 `"anthropic"`
  - 使用 `RestClient` 发起同步调用（`/v1/messages`）
  - 使用 `WebClient` 发起流式调用（解析 `content_block_delta` 事件类型）
  - Header：`x-api-key`、`anthropic-version: 2023-06-01`
  - 请求/响应格式独立映射（与 OpenAI 不同）
- `AnthropicProperties.java`（`@ConfigurationProperties("cartisan.ai.anthropic")`）
  - `apiKey`、`baseUrl`（默认 `https://api.anthropic.com`）

**测试：**
- 使用 WireMock 模拟 Anthropic API
- 测试 `content_block_delta` 流式解析
- 测试请求构造正确携带必要 Header

---

### F05-08: SSE 流式工具

**交付物：**
- `SseHelper.java`
  - `toSse(Flux<ChatStreamEvent>) → SseEmitter`
  - `toSse(Flux<ChatStreamEvent>, Consumer<TokenUsage>) → SseEmitter`：流结束时回调 usage
  - 封装：超时设置、异常处理（发送 error event）、客户端断开检测

**测试：**
- 单元测试验证 Flux 事件逐条写入 SseEmitter
- 测试流结束时 `Consumer<TokenUsage>` 被调用
- 测试异常时 SseEmitter 收到 error event 并 complete

---

### F05-09: 自动配置

**交付物：**
- `CartisanAiAutoConfiguration.java`
  - `OpenAiProvider` Bean：`@ConditionalOnProperty("cartisan.ai.openai.api-key")`
  - `DeepSeekProvider` Bean：`@ConditionalOnProperty("cartisan.ai.deepseek.api-key")`
  - `AnthropicProvider` Bean：`@ConditionalOnProperty("cartisan.ai.anthropic.api-key")`
  - `ModelProviderRegistry` Bean：`@ConditionalOnBean(ModelProvider.class)`
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**配置示例（application.yml）：**
```yaml
cartisan:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1  # 可指向代理
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}
```

**测试：**
- 集成测试：配置 openai api-key，验证只有 `OpenAiProvider` Bean 被创建
- 验证无任何 api-key 时 `ModelProviderRegistry` 不创建
- 验证引入依赖后零配置自动生效

---

## 技术风险点

| 风险 | 说明 | 应对 |
|------|------|------|
| Reactor 依赖边界 | `chatStream` 返回 `Flux`，需引入 `spring-webflux`；Virtual Thread 与 Reactor 混用需注意边界 | 明确约定：Provider 内部可用 Reactor，SseHelper 处理 Flux→SseEmitter 转换；业务代码无需感知 Reactor |
| Anthropic 流式协议 | 格式为 `content_block_start` / `content_block_delta` / `message_stop`，比 OpenAI 复杂 | F05-07 独立实现，WireMock 覆盖各事件类型 |
| HTTP 客户端选型 | 同步用 `RestClient`（Spring 6.1+），流式用 `WebClient`；两者共存 | 以 Provider 为单位封装，对外只暴露 `Flux` |
| 测试隔离 | 不依赖真实 API Key | 全部 Provider 测试使用 WireMock，CI 可正常运行 |

---

## 复杂度评估标准

| 复杂度 | 代码量 | 特征 |
|--------|--------|------|
| **S** | 30-80 行 | 纯数据类、简单工具、无复杂集成 |
| **M** | 100-200 行 | 涉及 HTTP 客户端、流式处理、多组件协作 |
| **L** | 200-300 行 | 跨模块集成、复杂状态管理、需要仔细设计 |

---

## 参考文档

- 设计文档：[cartisan-boot-设计文档.md](../../cartisan-boot-设计文档.md) §4.9
- Epic 依赖：Epic 1（cartisan-core）
- AI 协作 SOP：[AI协作开发SOP.md](../../AI协作开发SOP.md)
