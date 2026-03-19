# F05-06 DeepSeek Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 提取 OpenAI 协议抽象基类 `OpenAiCompatibleProvider`，重构 `OpenAiProvider` 继承它，并基于同一基类实现 `DeepSeekProvider`。

**Architecture:** 新建 `openaicompat/` 包承载协议层（抽象基类 + HTTP 客户端 + DTOs），`openai/` 和 `deepseek/` 各自只放具体子类。`OpenAiCompatibleProvider` 为抽象类，`id()` 和 `supportedModels()` 由子类实现。

**Tech Stack:** Java 21, Spring Boot 3.4.x, RestClient（同步）, WebClient + Reactor（流式）, WireMock（HTTP 层测试）, Mockito（Provider 层测试）

---

## 文件清单

### 新建
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openaicompat/package-info.java`
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openaicompat/OpenAiCompatibleClient.java` — 原 `OpenAiClient` 改名迁移
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openaicompat/OpenAiChatRequest.java` — 迁移自 `openai/`
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openaicompat/OpenAiChatResponse.java` — 迁移自 `openai/`
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openaicompat/OpenAiStreamChunk.java` — 迁移自 `openai/`
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openaicompat/OpenAiErrorResponse.java` — 迁移自 `openai/`
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openaicompat/OpenAiCompatibleProvider.java` — 抽象基类，chat/chatStream 逻辑
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/deepseek/package-info.java`
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/deepseek/DeepSeekProperties.java`
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/deepseek/DeepSeekProvider.java`
- `cartisan-ai/src/test/java/com/cartisan/ai/provider/openaicompat/OpenAiCompatibleClientTest.java` — 原 `OpenAiClientTest` 改名迁移
- `cartisan-ai/src/test/java/com/cartisan/ai/provider/deepseek/DeepSeekProviderTest.java`

### 修改
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiProvider.java` — 改为继承 `OpenAiCompatibleProvider`，保留 `id()`、`supportedModels()` 和构造
- `cartisan-ai/src/test/java/com/cartisan/ai/provider/openai/OpenAiProviderTest.java` — 更新 import 路径

### 删除
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiClient.java`
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiChatRequest.java`
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiChatResponse.java`
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiStreamChunk.java`
- `cartisan-ai/src/main/java/com/cartisan/ai/provider/openai/OpenAiErrorResponse.java`
- `cartisan-ai/src/test/java/com/cartisan/ai/provider/openai/OpenAiClientTest.java`

---

## Task 1: 迁移 DTOs 到 openaicompat/

**Files:**
- Create: `provider/openaicompat/package-info.java`
- Create: `provider/openaicompat/OpenAiChatRequest.java`
- Create: `provider/openaicompat/OpenAiChatResponse.java`
- Create: `provider/openaicompat/OpenAiStreamChunk.java`
- Create: `provider/openaicompat/OpenAiErrorResponse.java`
- Delete: 上述 4 个文件在 `provider/openai/` 下的原版本

- [ ] 创建 `openaicompat/package-info.java`，package 声明为 `com.cartisan.ai.provider.openaicompat`
- [ ] 将 4 个 DTO 文件复制到 `openaicompat/`，将每个文件的 package 声明改为 `com.cartisan.ai.provider.openaicompat`
- [ ] 删除 `openai/` 下的 4 个原 DTO 文件
- [ ] 更新 `OpenAiClient.java` 中对这 4 个类的 import（改为 `openaicompat`）
- [ ] 更新 `OpenAiProvider.java` 中对这 4 个类的 import
- [ ] 编译验证：`./gradlew :cartisan-ai:compileJava`
- [ ] 提交：`refactor(cartisan-ai): migrate OpenAi DTOs to openaicompat package`

---

## Task 2: 迁移并改名 OpenAiClient → OpenAiCompatibleClient

**Files:**
- Create: `provider/openaicompat/OpenAiCompatibleClient.java`
- Create: `test/.../openaicompat/OpenAiCompatibleClientTest.java`
- Delete: `provider/openai/OpenAiClient.java`
- Delete: `test/.../openai/OpenAiClientTest.java`
- Modify: `provider/openai/OpenAiProvider.java`

- [ ] 将 `OpenAiClient.java` 复制到 `openaicompat/OpenAiCompatibleClient.java`，修改 package 声明和类名
- [ ] 删除 `openai/OpenAiClient.java`
- [ ] 更新 `OpenAiProvider.java`：将所有 `OpenAiClient` 引用改为 `OpenAiCompatibleClient`，包括 import、字段类型、两个构造函数中的 `new OpenAiClient(...)` 实例化语句
- [ ] 将 `OpenAiClientTest.java` 复制到 `openaicompat/OpenAiCompatibleClientTest.java`，修改 package 声明、类名，以及文件内所有 `OpenAiClient` → `OpenAiCompatibleClient`
- [ ] 删除 `openai/OpenAiClientTest.java`
- [ ] 运行测试：`./gradlew :cartisan-ai:test`，确认全部通过
- [ ] 提交：`refactor(cartisan-ai): migrate OpenAiClient to openaicompat as OpenAiCompatibleClient`

---

## Task 3: 提取 OpenAiCompatibleProvider 抽象基类，重构 OpenAiProvider

**Files:**
- Create: `provider/openaicompat/OpenAiCompatibleProvider.java`
- Modify: `provider/openai/OpenAiProvider.java`
- Modify: `test/.../openai/OpenAiProviderTest.java`

- [ ] 创建 `OpenAiCompatibleProvider.java`（抽象类，实现 `ModelProvider`）：
  - `protected` 构造接收 `OpenAiCompatibleClient`
  - 将 `chat()`、`chatStream()`、`toOpenAiRequest()`、`toTokenUsage()` 从 `OpenAiProvider` 迁移进来
  - `id()` 和 `supportedModels()` 声明为 `abstract`
- [ ] 重构 `OpenAiProvider`：
  - 改为 `extends OpenAiCompatibleProvider`
  - 删除迁移走的方法，只保留 `id()`、`supportedModels()` 和两个构造函数
  - 构造函数调用 `super(client)`
- [ ] 更新 `OpenAiProviderTest`：将 `OpenAiClient` import 改为 `OpenAiCompatibleClient`，`mock(OpenAiClient.class)` 改为 `mock(OpenAiCompatibleClient.class)`
- [ ] 运行测试：`./gradlew :cartisan-ai:test`，确认全部通过
- [ ] 提交：`refactor(cartisan-ai): extract OpenAiCompatibleProvider abstract base class`

---

## Task 4: 实现 DeepSeekProvider（TDD）

**Files:**
- Create: `provider/deepseek/package-info.java`
- Create: `provider/deepseek/DeepSeekProperties.java`
- Create: `provider/deepseek/DeepSeekProvider.java`
- Create: `test/.../deepseek/DeepSeekProviderTest.java`

- [ ] 创建 `deepseek/package-info.java`
- [ ] 创建 `DeepSeekProperties`：`@ConfigurationProperties("cartisan.ai.deepseek")`，字段 `apiKey`、`baseUrl`（默认 `https://api.deepseek.com/v1`）、`models`（默认 `deepseek-chat`、`deepseek-reasoner`）
- [ ] 先写 `DeepSeekProviderTest`：
  - `shouldReturnDeepSeek_whenIdCalled()`
  - `shouldReturnConfiguredModels_whenSupportedModelsCalled()`
- [ ] 运行测试确认**失败**：`./gradlew :cartisan-ai:test --tests "*DeepSeekProviderTest"`
- [ ] 实现 `DeepSeekProvider`：`extends OpenAiCompatibleProvider`，实现 `id()` 返回 `"deepseek"`，实现 `supportedModels()`；提供面向 `DeepSeekProperties` 的公共构造和 package-private 测试构造
- [ ] 运行测试确认**通过**：`./gradlew :cartisan-ai:test`
- [ ] 提交：`feat(cartisan-ai): F05-06 DeepSeekProvider`
