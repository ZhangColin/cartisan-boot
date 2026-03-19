# F05-03 Provider SPI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 定义 `ModelProvider` 接口和 `ModelUsageListener` 接口，作为后续所有 AI Provider 实现的统一契约与计费扩展点。

**Architecture:** 两个纯接口，无实现逻辑。`ModelProvider` 定义同步/流式对话契约；`ModelUsageListener` 是函数式接口，供 F05-04 Registry 在每次调用后触发计费回调。测试通过 `FakeModelProvider` 内部类验证接口契约完整性。

**Tech Stack:** Java 21、Reactor (`reactor.core.publisher.Flux`)、JUnit 5、AssertJ

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| 创建 | `cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelProvider.java` | Provider SPI 接口（4 个抽象方法） |
| 创建 | `cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelUsageListener.java` | 计费监听器函数式接口 |
| 创建 | `cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelProviderContractTest.java` | FakeModelProvider 接口契约测试 |
| 创建 | `cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelUsageListenerTest.java` | Listener 函数式接口测试 |

---

## Task 1: `ModelProvider` 接口 + 契约测试

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelProvider.java`
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelProviderContractTest.java`

- [ ] **Step 1: 写契约测试（会编译失败——`ModelProvider` 尚未存在）**

新建文件 `cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelProviderContractTest.java`：

```java
package com.cartisan.ai.provider;

import com.cartisan.ai.model.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelProviderContractTest {

    // FakeModelProvider：最小化满足接口契约的测试替身
    static class FakeModelProvider implements ModelProvider {
        @Override
        public String id() {
            return "fake";
        }

        @Override
        public List<String> supportedModels() {
            return List.of("fake-model-v1");
        }

        @Override
        public ChatResponse chat(ChatRequest request) {
            return new ChatResponse("fake response", request.model(), new TokenUsage(10, 20, 30));
        }

        @Override
        public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
            return Flux.just(
                new ChatStreamEvent("hello ", false, null),
                new ChatStreamEvent("world", false, null),
                new ChatStreamEvent("", true, new TokenUsage(10, 20, 30))
            );
        }
    }

    private final ModelProvider provider = new FakeModelProvider();

    private ChatRequest sampleRequest() {
        return new ChatRequest(
            "fake-model-v1",
            List.of(new ChatMessage(Role.USER, "Hi")),
            null, null, false
        );
    }

    @Test
    void shouldReturnNonBlankId() {
        assertThat(provider.id()).isNotBlank();
    }

    @Test
    void shouldReturnNonEmptySupportedModels() {
        assertThat(provider.supportedModels()).isNotEmpty();
    }

    @Test
    void shouldReturnValidChatResponse() {
        ChatResponse response = provider.chat(sampleRequest());

        assertThat(response.content()).isNotNull();
        assertThat(response.model()).isNotNull();
        assertThat(response.usage()).isNotNull();
    }

    @Test
    void shouldReturnStreamWithFinishedEvent() {
        List<ChatStreamEvent> events = provider.chatStream(sampleRequest()).collectList().block();

        assertThat(events).isNotEmpty();

        ChatStreamEvent lastEvent = events.get(events.size() - 1);
        assertThat(lastEvent.finished()).isTrue();
        assertThat(lastEvent.usage()).isNotNull();
    }
}
```

- [ ] **Step 2: 确认编译失败**

```bash
cd /Users/zhangcolin/workspace/cartisan-boot
./gradlew :cartisan-ai:compileTestJava 2>&1 | grep "error:"
```

预期输出包含：`error: cannot find symbol` 指向 `ModelProvider`

- [ ] **Step 3: 创建 `ModelProvider` 接口**

新建文件 `cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelProvider.java`：

```java
package com.cartisan.ai.provider;

import com.cartisan.ai.model.ChatRequest;
import com.cartisan.ai.model.ChatResponse;
import com.cartisan.ai.model.ChatStreamEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ModelProvider {

    String id();

    List<String> supportedModels();

    ChatResponse chat(ChatRequest request);

    Flux<ChatStreamEvent> chatStream(ChatRequest request);
}
```

- [ ] **Step 4: 运行契约测试，确认全部通过**

```bash
./gradlew :cartisan-ai:test --tests "com.cartisan.ai.provider.ModelProviderContractTest"
```

预期：`4 tests completed, 0 failures`

- [ ] **Step 5: 提交**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelProvider.java \
        cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelProviderContractTest.java
git commit -m "feat(cartisan-ai): F05-03 ModelProvider 接口 + 契约测试"
```

---

## Task 2: `ModelUsageListener` 接口 + 测试

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelUsageListener.java`
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelUsageListenerTest.java`

- [ ] **Step 1: 写测试（会编译失败——`ModelUsageListener` 尚未存在）**

新建文件 `cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelUsageListenerTest.java`：

```java
package com.cartisan.ai.provider;

import com.cartisan.ai.model.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelUsageListenerTest {

    @Test
    void shouldBeAssignableAsLambda() {
        List<String> captured = new ArrayList<>();

        ModelUsageListener listener = (providerId, model, usage) ->
            captured.add(providerId + ":" + model + ":" + usage.totalTokens());

        listener.onUsage("openai", "gpt-4o", new TokenUsage(10, 20, 30));

        assertThat(captured).containsExactly("openai:gpt-4o:30");
    }
}
```

- [ ] **Step 2: 确认编译失败**

```bash
./gradlew :cartisan-ai:compileTestJava 2>&1 | grep "error:"
```

预期输出包含：`error: cannot find symbol` 指向 `ModelUsageListener`

- [ ] **Step 3: 创建 `ModelUsageListener` 接口**

新建文件 `cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelUsageListener.java`：

```java
package com.cartisan.ai.provider;

import com.cartisan.ai.model.TokenUsage;

@FunctionalInterface
public interface ModelUsageListener {

    /**
     * 每次 AI 调用完成后触发，用于 Token 用量统计与计费。
     *
     * @param providerId 提供商标识，来自 {@link ModelProvider#id()}
     * @param model      实际使用的模型名，来自 {@code ChatResponse.model()}（服务端确认值，
     *                   在代理/路由场景下可能与请求中的 model 不同）
     * @param usage      本次调用消耗的 Token 明细
     */
    void onUsage(String providerId, String model, TokenUsage usage);
}
```

- [ ] **Step 4: 运行所有 provider 测试，确认全部通过**

```bash
./gradlew :cartisan-ai:test --tests "com.cartisan.ai.provider.*"
```

预期：`5 tests completed, 0 failures`

- [ ] **Step 5: 提交**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelUsageListener.java \
        cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelUsageListenerTest.java
git commit -m "feat(cartisan-ai): F05-03 ModelUsageListener 计费监听器接口"
```

---

## Task 3: 全量验证

- [ ] **Step 1: 运行 cartisan-ai 全量测试**

```bash
./gradlew :cartisan-ai:test
```

预期：所有测试通过，无失败

- [ ] **Step 2: 确认编译无警告**

```bash
./gradlew :cartisan-ai:compileJava
```

预期：`BUILD SUCCESSFUL`
