# F05-04 ModelProviderRegistry 设计文档

**日期**：2026-03-19
**Feature**：F05-04 ModelProviderRegistry
**状态**：待实现

---

## 背景

F05-03 已完成 `ModelProvider` 接口与 `ModelUsageListener` 接口定义。F05-04 在此基础上构建注册表（Registry），解决两个问题：

1. **Provider 查找**：系统中存在多个 AI Provider Bean（OpenAI、DeepSeek、Anthropic），调用方需要按 providerId 或 modelName 找到对应 Provider。
2. **Listener 触发**：每次 AI 调用完成后需自动触发所有 `ModelUsageListener`（计费、统计），触发逻辑集中在 Registry，调用方无感知。

---

## 设计决策

### 1. Facade Registry（查找 + 代理调用）

Registry 同时提供查找方法和代理调用方法，而非纯查找注册表。

**理由**：
- F05-03 设计文档明确规定"Listener 触发逻辑由 F05-04 Registry 负责"
- 纯查找 Registry 无法拦截调用、触发 Listener，会导致每个调用方都必须手动触发，容易遗漏
- 代理调用方法使横切关注点（计费）在框架层一次性封装，调用方只需一行代码

**排除的替代方案**：
- 纯查找 + 调用方手动触发 Listener：违反 DRY，调用方负担重
- Registry + Dispatcher 分离：对 60-80 行体量过度设计，且调用方需注入两个 Bean
- Registry 实现 `ModelProvider` 接口：`ModelProvider.id()` 语义为单一提供商标识，Registry 实现该接口语义不自洽

### 2. 不加 `@Component`，由 F05-09 AutoConfiguration 注册

`cartisan-ai` 是框架模块，业务项目的 `@ComponentScan` 不会扫描 `com.cartisan.ai` 包。

Registry 是普通 Java 类，由 F05-09 `CartisanAiAutoConfiguration` 以 `@Bean` 方法显式注册：

```java
@Bean
@ConditionalOnBean(ModelProvider.class)
public ModelProviderRegistry modelProviderRegistry(
        List<ModelProvider> providers,
        ObjectProvider<List<ModelUsageListener>> listeners) {
    return new ModelProviderRegistry(providers, listeners.getIfAvailable(List::of));
}
```

F05-04 本身只交付普通类与单元测试，不涉及 Spring 上下文。

### 3. 流式调用通过 Reactor 操作符透明触发 Listener

`chatStream()` 返回 `Flux<ChatStreamEvent>`，TokenUsage 仅在最后一个事件（`finished == true`）携带。Registry 在返回的 Flux 上注入 `doOnNext`，检测到终止事件时触发所有 Listener，对调用方完全透明。

### 4. Listener 异常不影响主调用链

遍历触发所有 Listener 时，用 try-catch 包住每个 Listener 调用，异常只记录日志，不往上抛，确保计费逻辑故障不影响 AI 调用结果。

---

## 交付物

### `ModelProviderRegistry.java`

```
com.cartisan.ai.provider.ModelProviderRegistry
```

**构造参数**：
- `List<ModelProvider> providers`：所有注册的 Provider
- `List<ModelUsageListener> listeners`：所有注册的 Listener（可为空列表）

**内部结构**（构造时一次性建立，不可变）：
- `providerById`：`Map<String, ModelProvider>`，key 为 `provider.id()`
- `providerByModel`：`Map<String, ModelProvider>`，key 为 `provider.supportedModels()` 展开的每个模型名

**公开 API**：

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `listProviders()` | `List<ModelProvider>` | 返回所有已注册的 Provider |
| `getProvider(String providerId)` | `ModelProvider` | 按 providerId 查找，找不到抛 `DomainException` |
| `getProviderByModel(String modelName)` | `ModelProvider` | 按模型名查找，找不到抛 `DomainException` |
| `chat(String providerId, ChatRequest request)` | `ChatResponse` | 代理同步调用，自动触发 Listener |
| `chatStream(String providerId, ChatRequest request)` | `Flux<ChatStreamEvent>` | 代理流式调用，订阅时透明触发 Listener |

**异常**：使用 `DomainException(BaseCodeMessage.RESOURCE_NOT_FOUND, providerId/modelName)`，不新增 `AiCodeMessage`。

---

## Listener 触发逻辑

### 同步调用

```
getProvider(providerId) → provider.chat(request) → 触发所有 Listener → 返回 response
```

触发参数：
- `providerId`：来自 `provider.id()`
- `model`：来自 `response.model()`（服务端确认值）
- `usage`：来自 `response.usage()`

### 流式调用

```
getProvider(providerId) → provider.chatStream(request) → 包装 Flux（注入 doOnNext）→ 返回给调用方
```

调用方消费 Flux 时，`doOnNext` 检测到 `event.finished() == true` 自动触发所有 Listener。

触发参数：
- `providerId`：来自 `provider.id()`
- `model`：来自 `request.model()`（流式调用无 ChatResponse，使用请求中的模型名）
- `usage`：来自终止事件的 `event.usage()`

---

## 测试策略

使用 `FakeModelProvider`（实现 `ModelProvider`，返回固定数据）+ 直接 `new ModelProviderRegistry(...)` 进行单元测试，无需 Spring 上下文。

| 测试方法 | 验证点 |
|----------|--------|
| `shouldReturnAllProviders` | `listProviders()` 返回所有注入的 Provider |
| `shouldFindProviderById` | `getProvider("openai")` 返回正确 Provider |
| `shouldFindProviderByModel` | `getProviderByModel("gpt-4o")` 返回正确 Provider |
| `shouldThrowWhenProviderNotFound` | `getProvider("unknown")` 抛 `DomainException`，code 为 `RESOURCE_NOT_FOUND` |
| `shouldThrowWhenModelNotFound` | `getProviderByModel("unknown-model")` 抛 `DomainException` |
| `shouldTriggerListenerOnChat` | `chat()` 后 Listener 收到正确的 providerId / model / usage |
| `shouldTriggerListenerOnStream` | 消费完 `chatStream()` 的 Flux 后，Listener 收到正确的 usage |
| `shouldNotPropagateListenerException` | Listener 抛异常时，`chat()` 仍正常返回结果 |

---

## 边界说明

| 包含 | 不包含 |
|------|--------|
| `ModelProviderRegistry` 类实现 | Spring Bean 注册（F05-09 负责） |
| 查找方法（by id / by model） | 平台模型名到 Provider 的映射（业务层职责） |
| 代理调用方法（chat / chatStream） | 具体 Provider 实现（F05-05 ~ F05-07） |
| Listener 触发逻辑（同步 + 流式） | SSE 转换（F05-08 负责） |

---

## 依赖关系

- **上游**：F05-03（`ModelProvider`、`ModelUsageListener`、`ChatRequest`、`ChatResponse`、`ChatStreamEvent`、`TokenUsage`）
- **下游**：F05-09（AutoConfiguration 负责注册 Bean）
