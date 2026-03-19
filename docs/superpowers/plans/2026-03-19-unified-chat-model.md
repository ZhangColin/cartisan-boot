# F05-02: 统一对话模型 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `com.cartisan.ai.model` 包下创建 6 个纯 Java Record 类型，作为 AI 模块的统一对话契约。

**Architecture:** 全部为纯 Java Record，零框架依赖。紧凑构造函数校验不变量，`List.copyOf` 保证不可变。TDD 驱动，每个类型先写测试再实现。

**Tech Stack:** Java 21 Record, JUnit 5, AssertJ

**Spec:** `docs/superpowers/specs/2026-03-19-unified-chat-model-design.md`

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| Create | `cartisan-ai/src/main/java/com/cartisan/ai/model/Role.java` | 消息角色枚举 |
| Create | `cartisan-ai/src/main/java/com/cartisan/ai/model/ChatMessage.java` | 单条对话消息 |
| Create | `cartisan-ai/src/main/java/com/cartisan/ai/model/TokenUsage.java` | Token 用量统计 |
| Create | `cartisan-ai/src/main/java/com/cartisan/ai/model/ChatRequest.java` | 对话请求 |
| Create | `cartisan-ai/src/main/java/com/cartisan/ai/model/ChatResponse.java` | 同步对话响应 |
| Create | `cartisan-ai/src/main/java/com/cartisan/ai/model/ChatStreamEvent.java` | 流式事件 |
| Create | `cartisan-ai/src/test/java/com/cartisan/ai/model/RoleTest.java` | Role 测试 |
| Create | `cartisan-ai/src/test/java/com/cartisan/ai/model/ChatMessageTest.java` | ChatMessage 测试 |
| Create | `cartisan-ai/src/test/java/com/cartisan/ai/model/TokenUsageTest.java` | TokenUsage 测试 |
| Create | `cartisan-ai/src/test/java/com/cartisan/ai/model/ChatRequestTest.java` | ChatRequest 测试 |
| Create | `cartisan-ai/src/test/java/com/cartisan/ai/model/ChatResponseTest.java` | ChatResponse 测试 |
| Create | `cartisan-ai/src/test/java/com/cartisan/ai/model/ChatStreamEventTest.java` | ChatStreamEvent 测试 |
| Delete | `cartisan-ai/src/main/java/com/cartisan/ai/model/package-info.java` | 由实际类替代 |

---

### Task 1: Role 枚举

**Files:**
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/model/RoleTest.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/model/Role.java`

- [ ] **Step 1: Write the failing test**

```java
package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void shouldHaveThreeRoles() {
        assertThat(Role.values()).containsExactly(Role.SYSTEM, Role.USER, Role.ASSISTANT);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.RoleTest" --info`
Expected: FAIL — `Role` class not found

- [ ] **Step 3: Write minimal implementation**

```java
package com.cartisan.ai.model;

public enum Role {
    SYSTEM, USER, ASSISTANT
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.RoleTest" --info`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/model/Role.java \
       cartisan-ai/src/test/java/com/cartisan/ai/model/RoleTest.java
git commit -m "feat(cartisan-ai): F05-02 添加 Role 枚举"
```

---

### Task 2: ChatMessage

**Files:**
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/model/ChatMessageTest.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/model/ChatMessage.java`

- [ ] **Step 1: Write the failing test**

```java
package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageTest {

    @Test
    void shouldCreateChatMessage() {
        // Given & When
        ChatMessage message = new ChatMessage(Role.USER, "Hello");

        // Then
        assertThat(message.role()).isEqualTo(Role.USER);
        assertThat(message.content()).isEqualTo("Hello");
    }

    @Test
    void shouldThrowException_whenRoleIsNull() {
        assertThatThrownBy(() -> new ChatMessage(null, "Hello"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("role must not be null");
    }

    @Test
    void shouldThrowException_whenContentIsNull() {
        assertThatThrownBy(() -> new ChatMessage(Role.USER, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("content must not be null");
    }

    @Test
    void shouldBeEqual_whenSameRoleAndContent() {
        // Given
        ChatMessage msg1 = new ChatMessage(Role.USER, "Hello");
        ChatMessage msg2 = new ChatMessage(Role.USER, "Hello");

        // Then
        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.ChatMessageTest" --info`
Expected: FAIL — `ChatMessage` class not found

- [ ] **Step 3: Write minimal implementation**

```java
package com.cartisan.ai.model;

import java.util.Objects;

public record ChatMessage(Role role, String content) {
    public ChatMessage {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.ChatMessageTest" --info`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/model/ChatMessage.java \
       cartisan-ai/src/test/java/com/cartisan/ai/model/ChatMessageTest.java
git commit -m "feat(cartisan-ai): F05-02 添加 ChatMessage Record"
```

---

### Task 3: TokenUsage

**Files:**
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/model/TokenUsageTest.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/model/TokenUsage.java`

- [ ] **Step 1: Write the failing test**

```java
package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenUsageTest {

    @Test
    void shouldCreateTokenUsage() {
        // Given & When
        TokenUsage usage = new TokenUsage(10, 20, 30);

        // Then
        assertThat(usage.promptTokens()).isEqualTo(10);
        assertThat(usage.completionTokens()).isEqualTo(20);
        assertThat(usage.totalTokens()).isEqualTo(30);
    }

    @Test
    void shouldAllowZeroTokens() {
        // Given & When
        TokenUsage usage = new TokenUsage(0, 0, 0);

        // Then
        assertThat(usage.promptTokens()).isZero();
        assertThat(usage.completionTokens()).isZero();
        assertThat(usage.totalTokens()).isZero();
    }

    @Test
    void shouldThrowException_whenPromptTokensNegative() {
        assertThatThrownBy(() -> new TokenUsage(-1, 20, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token counts must not be negative");
    }

    @Test
    void shouldThrowException_whenCompletionTokensNegative() {
        assertThatThrownBy(() -> new TokenUsage(10, -1, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token counts must not be negative");
    }

    @Test
    void shouldThrowException_whenTotalTokensNegative() {
        assertThatThrownBy(() -> new TokenUsage(10, 20, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token counts must not be negative");
    }

    @Test
    void shouldBeEqual_whenSameValues() {
        // Given
        TokenUsage usage1 = new TokenUsage(10, 20, 30);
        TokenUsage usage2 = new TokenUsage(10, 20, 30);

        // Then
        assertThat(usage1).isEqualTo(usage2);
        assertThat(usage1.hashCode()).isEqualTo(usage2.hashCode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.TokenUsageTest" --info`
Expected: FAIL — `TokenUsage` class not found

- [ ] **Step 3: Write minimal implementation**

```java
package com.cartisan.ai.model;

public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
    public TokenUsage {
        if (promptTokens < 0 || completionTokens < 0 || totalTokens < 0) {
            throw new IllegalArgumentException("Token counts must not be negative");
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.TokenUsageTest" --info`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/model/TokenUsage.java \
       cartisan-ai/src/test/java/com/cartisan/ai/model/TokenUsageTest.java
git commit -m "feat(cartisan-ai): F05-02 添加 TokenUsage Record"
```

---

### Task 4: ChatRequest

**Files:**
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/model/ChatRequestTest.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/model/ChatRequest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRequestTest {

    @Test
    void shouldCreateChatRequest() {
        // Given
        List<ChatMessage> messages = List.of(new ChatMessage(Role.USER, "Hello"));

        // When
        ChatRequest request = new ChatRequest("gpt-4", messages, 0.7, 1000, false);

        // Then
        assertThat(request.model()).isEqualTo("gpt-4");
        assertThat(request.messages()).hasSize(1);
        assertThat(request.temperature()).isEqualTo(0.7);
        assertThat(request.maxTokens()).isEqualTo(1000);
        assertThat(request.stream()).isFalse();
    }

    @Test
    void shouldAllowNullOptionalFields() {
        // Given
        List<ChatMessage> messages = List.of(new ChatMessage(Role.USER, "Hello"));

        // When
        ChatRequest request = new ChatRequest("gpt-4", messages, null, null, false);

        // Then
        assertThat(request.temperature()).isNull();
        assertThat(request.maxTokens()).isNull();
    }

    @Test
    void shouldThrowException_whenModelIsNull() {
        List<ChatMessage> messages = List.of(new ChatMessage(Role.USER, "Hello"));

        assertThatThrownBy(() -> new ChatRequest(null, messages, null, null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("model must not be null");
    }

    @Test
    void shouldThrowException_whenMessagesIsNull() {
        assertThatThrownBy(() -> new ChatRequest("gpt-4", null, null, null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("messages must not be null");
    }

    @Test
    void shouldThrowException_whenMessagesIsEmpty() {
        assertThatThrownBy(() -> new ChatRequest("gpt-4", List.of(), null, null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("messages must not be empty");
    }

    @Test
    void shouldDefensivelyCopyMessages() {
        // Given
        ArrayList<ChatMessage> mutableList = new ArrayList<>();
        mutableList.add(new ChatMessage(Role.USER, "Hello"));
        ChatRequest request = new ChatRequest("gpt-4", mutableList, null, null, false);

        // When — 修改原始 List
        mutableList.add(new ChatMessage(Role.ASSISTANT, "Hi"));

        // Then — Record 内部不受影响
        assertThat(request.messages()).hasSize(1);
    }

    @Test
    void shouldReturnUnmodifiableMessages() {
        // Given
        List<ChatMessage> messages = List.of(new ChatMessage(Role.USER, "Hello"));
        ChatRequest request = new ChatRequest("gpt-4", messages, null, null, false);

        // Then
        assertThatThrownBy(() -> request.messages().add(new ChatMessage(Role.ASSISTANT, "Hi")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.ChatRequestTest" --info`
Expected: FAIL — `ChatRequest` class not found

- [ ] **Step 3: Write minimal implementation**

```java
package com.cartisan.ai.model;

import java.util.List;
import java.util.Objects;

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

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.ChatRequestTest" --info`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/model/ChatRequest.java \
       cartisan-ai/src/test/java/com/cartisan/ai/model/ChatRequestTest.java
git commit -m "feat(cartisan-ai): F05-02 添加 ChatRequest Record"
```

---

### Task 5: ChatResponse

**Files:**
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/model/ChatResponseTest.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/model/ChatResponse.java`

- [ ] **Step 1: Write the failing test**

```java
package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatResponseTest {

    @Test
    void shouldCreateChatResponse() {
        // Given
        TokenUsage usage = new TokenUsage(10, 20, 30);

        // When
        ChatResponse response = new ChatResponse("Hello!", "gpt-4", usage);

        // Then
        assertThat(response.content()).isEqualTo("Hello!");
        assertThat(response.model()).isEqualTo("gpt-4");
        assertThat(response.usage()).isEqualTo(usage);
    }

    @Test
    void shouldThrowException_whenContentIsNull() {
        TokenUsage usage = new TokenUsage(10, 20, 30);

        assertThatThrownBy(() -> new ChatResponse(null, "gpt-4", usage))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("content must not be null");
    }

    @Test
    void shouldThrowException_whenModelIsNull() {
        TokenUsage usage = new TokenUsage(10, 20, 30);

        assertThatThrownBy(() -> new ChatResponse("Hello!", null, usage))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("model must not be null");
    }

    @Test
    void shouldThrowException_whenUsageIsNull() {
        assertThatThrownBy(() -> new ChatResponse("Hello!", "gpt-4", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("usage must not be null");
    }

    @Test
    void shouldBeEqual_whenSameValues() {
        // Given
        TokenUsage usage = new TokenUsage(10, 20, 30);
        ChatResponse resp1 = new ChatResponse("Hello!", "gpt-4", usage);
        ChatResponse resp2 = new ChatResponse("Hello!", "gpt-4", usage);

        // Then
        assertThat(resp1).isEqualTo(resp2);
        assertThat(resp1.hashCode()).isEqualTo(resp2.hashCode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.ChatResponseTest" --info`
Expected: FAIL — `ChatResponse` class not found

- [ ] **Step 3: Write minimal implementation**

```java
package com.cartisan.ai.model;

import java.util.Objects;

public record ChatResponse(String content, String model, TokenUsage usage) {
    public ChatResponse {
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(usage, "usage must not be null");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.ChatResponseTest" --info`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/model/ChatResponse.java \
       cartisan-ai/src/test/java/com/cartisan/ai/model/ChatResponseTest.java
git commit -m "feat(cartisan-ai): F05-02 添加 ChatResponse Record"
```

---

### Task 6: ChatStreamEvent

**Files:**
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/model/ChatStreamEventTest.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/model/ChatStreamEvent.java`

- [ ] **Step 1: Write the failing test**

```java
package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatStreamEventTest {

    @Test
    void shouldCreateStreamingEvent() {
        // Given & When
        ChatStreamEvent event = new ChatStreamEvent("Hello", false, null);

        // Then
        assertThat(event.delta()).isEqualTo("Hello");
        assertThat(event.finished()).isFalse();
        assertThat(event.usage()).isNull();
    }

    @Test
    void shouldCreateFinishedEventWithUsage() {
        // Given
        TokenUsage usage = new TokenUsage(10, 20, 30);

        // When
        ChatStreamEvent event = new ChatStreamEvent("", true, usage);

        // Then
        assertThat(event.delta()).isEmpty();
        assertThat(event.finished()).isTrue();
        assertThat(event.usage()).isEqualTo(usage);
    }

    @Test
    void shouldThrowException_whenDeltaIsNull() {
        assertThatThrownBy(() -> new ChatStreamEvent(null, false, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("delta must not be null");
    }

    @Test
    void shouldBeEqual_whenSameValues() {
        // Given
        ChatStreamEvent event1 = new ChatStreamEvent("Hi", false, null);
        ChatStreamEvent event2 = new ChatStreamEvent("Hi", false, null);

        // Then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.ChatStreamEventTest" --info`
Expected: FAIL — `ChatStreamEvent` class not found

- [ ] **Step 3: Write minimal implementation**

```java
package com.cartisan.ai.model;

import java.util.Objects;

public record ChatStreamEvent(String delta, boolean finished, TokenUsage usage) {
    public ChatStreamEvent {
        Objects.requireNonNull(delta, "delta must not be null");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :cartisan-ai:test --tests "com.cartisan.ai.model.ChatStreamEventTest" --info`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/model/ChatStreamEvent.java \
       cartisan-ai/src/test/java/com/cartisan/ai/model/ChatStreamEventTest.java
git commit -m "feat(cartisan-ai): F05-02 添加 ChatStreamEvent Record"
```

---

### Task 7: 清理与全量验证

**Files:**
- Delete: `cartisan-ai/src/main/java/com/cartisan/ai/model/package-info.java`

- [ ] **Step 1: 删除 model 包的 package-info.java（已被实际类替代）**

```bash
git rm cartisan-ai/src/main/java/com/cartisan/ai/model/package-info.java
```

- [ ] **Step 2: 运行 cartisan-ai 全量测试**

Run: `./gradlew :cartisan-ai:test --info`
Expected: 全部 PASS

- [ ] **Step 3: 运行全项目编译确保无破坏**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A cartisan-ai/
git commit -m "feat(cartisan-ai): F05-02 清理 package-info，全量验证通过"
```
