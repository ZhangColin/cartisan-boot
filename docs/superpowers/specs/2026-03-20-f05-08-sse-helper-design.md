# F05-08 SSE 流式工具设计文档

> **日期**: 2026-03-20
> **Feature**: F05-08 SSE 流式工具
> **复杂度**: M（Medium）
> **预估代码量**: 80-120 行

---

## 1. 目标

将 `Flux<ChatStreamEvent>` 转换为 Spring MVC `SseEmitter`，封装超时、异常、客户端断开等边界情况处理，使业务 Controller 只需一行代码即可返回流式响应。

---

## 2. 核心组件

### 2.1 SseHelper

```java
@Component
public class SseHelper {

    private final SseProperties properties;

    public SseHelper(SseProperties properties) {
        this.properties = properties;
    }

    /**
     * 将流式事件转换为 SSE Emitter（使用默认配置）
     */
    public SseEmitter toSse(Flux<ChatStreamEvent> events) {
        return toSse(events, null);
    }

    /**
     * 将流式事件转换为 SSE Emitter，流结束时回调 usage
     *
     * @param events 事件流
     * @param usageCallback 流完成时的 Token 使用回调
     * @return SseEmitter
     */
    public SseEmitter toSse(Flux<ChatStreamEvent> events,
                            Consumer<TokenUsage> usageCallback) {
        // 实现逻辑
    }
}
```

### 2.2 SseProperties

```java
@ConfigurationProperties("cartisan.ai.sse")
public class SseProperties {

    /**
     * SSE 连接超时时间，默认 5 分钟
     */
    private Duration timeout = Duration.ofMinutes(5);

    /**
     * 心跳间隔，默认 30 秒
     */
    private Duration heartbeat = Duration.ofSeconds(30);

    /**
     * 是否启用心跳，默认 true
     */
    private boolean heartbeatEnabled = true;

    // getters & setters
}
```

---

## 3. 数据流设计

```
Provider.chatStream()  →  Flux<ChatStreamEvent>
                              ↓
                         SseHelper.toSse()
                              ↓
                     ┌────────┴────────┐
                     ↓                 ↓
              创建 SseEmitter      订阅 Flux
                     ↓                 ↓
              配置 onTimeout      doOnNext → 发送 event
              配置 onError        doOnError → completeWithError
                     ↓            doOnCancel → 记录日志
                  返回给 Controller    doFinally → complete()
                                          ↓
                                    提取最后一个 usage
                                          ↓
                                    调用 usageCallback
```

**关键点**：
- `ChatStreamEvent.finished=true` 时，`usage` 字段非空
- 通过 `Flux.reduce()` 或 `events.filter(e -> e.usage() != null).takeLast(1)` 提取最终 usage
- 客户端断开时 `doOnCancel` 触发，Flux 自动取消订阅

---

## 4. 异常处理与边界情况

| 场景 | 处理方式 |
|------|----------|
| **正常完成** | `emitter.complete()`，调用 `usageCallback`（若非空） |
| **超时** | `onTimeout(() -> emitter.complete())`，记录日志 |
| **Flux 错误** | `doOnError(ex -> emitter.completeWithError(ex))` |
| **客户端断开** | `doOnCancel()` 记录日志，Flux 自动取消 |
| **发送失败** | 捕获 `IOException`，调用 `emitter.completeWithError(ex)` |
| **usage 为空** | 回调传入 `null`（流异常中断时可能无 usage） |

**心跳机制**（可选启用）：
- 使用 `Flux.merge()` 合并事件流和心跳流
- 心跳 event：`SseEmitter.event().name("heartbeat").data("ping")`

---

## 5. 使用示例

### 5.1 Controller 中使用

```java
@RestController
@RequestMapping("/ai/chat")
public class ChatController {

    private final ModelProviderRegistry registry;
    private final SseHelper sseHelper;

    @GetMapping("/stream")
    public SseEmitter chatStream(@RequestParam String model) {
        ModelProvider provider = registry.getProviderByModel(model);

        ChatRequest request = new ChatRequest(
            model,
            List.of(new ChatMessage(Role.USER, "你好")),
            true
        );

        return sseHelper.toSse(provider.chatStream(request), usage -> {
            // 记录 Token 消耗
            log.info("Tokens used: {}", usage.totalTokens());
        });
    }
}
```

### 5.2 配置（application.yml）

```yaml
cartisan:
  ai:
    sse:
      timeout: 5m              # 超时时间
      heartbeat: 30s           # 心跳间隔
      heartbeat-enabled: true  # 是否启用心跳
```

---

## 6. 包结构与文件清单

```
cartisan-ai/src/main/java/com/cartisan/ai/sse/
├── SseHelper.java              # 核心工具类
└── SseProperties.java          # 配置属性

cartisan-ai/src/test/java/com/cartisan/ai/sse/
└── SseHelperTest.java          # 单元测试
```

---

## 7. 依赖

- **F05-02**: `ChatStreamEvent`、`TokenUsage`
- **Spring Web**: `SseEmitter`
- **Reactor Core**: `Flux`

---

## 8. 测试策略

```java
@ExtendWith(MockitoExtension.class)
class SseHelperTest {

    @Test
    void shouldSendEventsToSseEmitter() {
        // Given: 模拟 Flux 发送 3 个 event
        // When: 调用 toSse()
        // Then: 验证 SseEmitter 收到所有 event 并 complete
    }

    @Test
    void shouldInvokeUsageCallback_whenStreamCompletes() {
        // Given: Flux 最后一个 event 携带 usage
        // When: 流完成
        // Then: usageCallback 被调用，参数正确
    }

    @Test
    void shouldCompleteWithError_whenFluxErrors() {
        // Given: Flux 抛出异常
        // When: 调用 toSse()
        // Then: SseEmitter.completeWithError() 被调用
    }

    @Test
    void shouldCompleteOnTimeout() {
        // 验证超时场景
    }

    @Test
    void shouldHandleClientDisconnect() {
        // 模拟客户端断开，验证资源清理
    }
}
```

**无需 WireMock** — 纯单元测试，用 `Flux.just()` / `Flux.error()` 模拟流。

---

## 9. 设计原则

1. **简洁优先** — 简单场景一行代码搞定
2. **行业最佳实践** — 超时优雅完成（`complete()`），不抛异常
3. **可配置** — 支持 Spring Boot 配置文件
4. **可扩展** — 未来可通过 Builder 模式扩展更多选项
