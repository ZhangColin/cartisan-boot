# F05-04 ModelProviderRegistry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `ModelProviderRegistry` — a Facade that looks up `ModelProvider` beans by id or model name and proxies `chat()`/`chatStream()` calls while transparently triggering all registered `ModelUsageListener`s.

**Architecture:** Single plain Java class (no `@Component`) in `com.cartisan.ai.provider`. Constructor builds two immutable lookup Maps from the injected provider list. Streaming Listener triggering uses `Flux.defer` + `AtomicBoolean` to guarantee exactly-once firing per subscription. Spring Bean registration is deferred to F05-09.

**Tech Stack:** Java 21, Reactor (`Flux.defer`, `doOnNext`), AssertJ, Mockito, JUnit 5

---

## File Map

| Action | File |
|--------|------|
| Create | `cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelProviderRegistry.java` |
| Create | `cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelProviderRegistryTest.java` |

---

## Task 1: Lookup — constructor, listProviders, getProvider, getProviderByModel

### Files
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelProviderRegistryTest.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelProviderRegistry.java`

---

- [ ] **Step 1: Write the failing tests for lookup behaviour**

Create `ModelProviderRegistryTest.java` with the test infrastructure and 7 lookup tests:

```java
package com.cartisan.ai.provider;

import com.cartisan.ai.model.*;
import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.DomainException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModelProviderRegistryTest {

    // ── Test doubles ─────────────────────────────────────────────────────────

    static ChatRequest sampleRequest(String model) {
        return new ChatRequest(model, List.of(new ChatMessage(Role.USER, "Hi")),
                null, null, false);
    }

    static class FakeProvider implements ModelProvider {
        private final String id;
        private final List<String> models;

        FakeProvider(String id, String... models) {
            this.id = id;
            this.models = List.of(models);
        }

        @Override public String id() { return id; }
        @Override public List<String> supportedModels() { return models; }

        @Override
        public ChatResponse chat(ChatRequest request) {
            return new ChatResponse("response from " + id, request.model(),
                    new TokenUsage(10, 20, 30));
        }

        @Override
        public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
            return Flux.just(
                new ChatStreamEvent("hello", false, null),
                new ChatStreamEvent("", true, new TokenUsage(10, 20, 30))
            );
        }
    }

    private final FakeProvider openai    = new FakeProvider("openai", "gpt-4o", "gpt-4o-mini");
    private final FakeProvider anthropic = new FakeProvider("anthropic", "claude-3-5-sonnet");

    // ── Lookup tests ─────────────────────────────────────────────────────────

    @Test
    void shouldReturnAllProviders() {
        var registry = new ModelProviderRegistry(List.of(openai, anthropic), List.of());

        assertThat(registry.listProviders()).containsExactlyInAnyOrder(openai, anthropic);
        // list must be unmodifiable
        assertThatThrownBy(() -> registry.listProviders().add(openai))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldReturnEmptyList_whenNoProviders() {
        var registry = new ModelProviderRegistry(List.of(), List.of());

        assertThat(registry.listProviders()).isEmpty();
    }

    @Test
    void shouldFindProviderById() {
        var registry = new ModelProviderRegistry(List.of(openai, anthropic), List.of());

        assertThat(registry.getProvider("openai")).isSameAs(openai);
    }

    @Test
    void shouldFindProviderByModel() {
        var registry = new ModelProviderRegistry(List.of(openai, anthropic), List.of());

        assertThat(registry.getProviderByModel("gpt-4o")).isSameAs(openai);
        assertThat(registry.getProviderByModel("claude-3-5-sonnet")).isSameAs(anthropic);
    }

    @Test
    void shouldThrowWhenProviderNotFound() {
        var registry = new ModelProviderRegistry(List.of(openai), List.of());

        assertThatThrownBy(() -> registry.getProvider("unknown"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCodeMessage())
                        .isEqualTo(BaseCodeMessage.RESOURCE_NOT_FOUND));
    }

    @Test
    void shouldThrowWhenModelNotFound() {
        var registry = new ModelProviderRegistry(List.of(openai), List.of());

        assertThatThrownBy(() -> registry.getProviderByModel("unknown-model"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCodeMessage())
                        .isEqualTo(BaseCodeMessage.RESOURCE_NOT_FOUND));
    }

    @Test
    void shouldThrowOnDuplicateModelName() {
        var conflict = new FakeProvider("deepseek", "gpt-4o"); // same model as openai

        assertThatThrownBy(() -> new ModelProviderRegistry(List.of(openai, conflict), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gpt-4o");
    }
}
```

- [ ] **Step 2: Run tests to verify they all fail**

```bash
./gradlew :cartisan-ai:test --tests "com.cartisan.ai.provider.ModelProviderRegistryTest" 2>&1 | tail -20
```

Expected: compilation error — `ModelProviderRegistry` does not exist.

- [ ] **Step 3: Implement the constructor and lookup methods**

Create `ModelProviderRegistry.java`:

```java
package com.cartisan.ai.provider;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.DomainException;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cartisan.ai.model.*;

public class ModelProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelProviderRegistry.class);

    private final Map<String, ModelProvider> providerById;
    private final Map<String, ModelProvider> providerByModel;
    private final List<ModelUsageListener> listeners;

    public ModelProviderRegistry(List<ModelProvider> providers, List<ModelUsageListener> listeners) {
        this.listeners = List.copyOf(listeners);

        Map<String, ModelProvider> byId    = new HashMap<>();
        Map<String, ModelProvider> byModel = new HashMap<>();

        for (ModelProvider provider : providers) {
            byId.put(provider.id(), provider);
            for (String model : provider.supportedModels()) {
                if (byModel.containsKey(model)) {
                    throw new IllegalArgumentException(
                        "Duplicate model name '" + model + "' declared by providers '"
                        + byModel.get(model).id() + "' and '" + provider.id() + "'");
                }
                byModel.put(model, provider);
            }
        }

        this.providerById    = Map.copyOf(byId);
        this.providerByModel = Map.copyOf(byModel);
    }

    public List<ModelProvider> listProviders() {
        return List.copyOf(providerById.values());
    }

    public ModelProvider getProvider(String providerId) {
        ModelProvider provider = providerById.get(providerId);
        if (provider == null) {
            throw new DomainException(BaseCodeMessage.RESOURCE_NOT_FOUND, providerId);
        }
        return provider;
    }

    public ModelProvider getProviderByModel(String modelName) {
        ModelProvider provider = providerByModel.get(modelName);
        if (provider == null) {
            throw new DomainException(BaseCodeMessage.RESOURCE_NOT_FOUND, modelName);
        }
        return provider;
    }

    public ChatResponse chat(String providerId, ChatRequest request) {
        // implemented in Task 2
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Flux<ChatStreamEvent> chatStream(String providerId, ChatRequest request) {
        // implemented in Task 3
        throw new UnsupportedOperationException("not yet implemented");
    }

    private void notifyListeners(String providerId, String model, TokenUsage usage) {
        for (ModelUsageListener listener : listeners) {
            try {
                listener.onUsage(providerId, model, usage);
            } catch (Exception e) {
                log.warn("ModelUsageListener threw an exception and was ignored", e);
            }
        }
    }
}
```

- [ ] **Step 4: Run lookup tests to verify they pass**

```bash
./gradlew :cartisan-ai:test --tests "com.cartisan.ai.provider.ModelProviderRegistryTest" 2>&1 | tail -20
```

Expected: 7 lookup tests PASS, `chat`/`chatStream` tests not yet written.

- [ ] **Step 5: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelProviderRegistry.java \
        cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelProviderRegistryTest.java
git commit -m "feat(cartisan-ai): F05-04 Registry lookup — getProvider / getProviderByModel / listProviders"
```

---

## Task 2: Sync delegation — chat() + Listener triggering

### Files
- Modify: `cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelProviderRegistryTest.java`
- Modify: `cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelProviderRegistry.java`

---

- [ ] **Step 1: Add sync delegation tests to the test file**

Append these three tests inside `ModelProviderRegistryTest` (before the closing `}`):

```java
    // ── Sync delegation tests ─────────────────────────────────────────────────

    @Test
    void shouldTriggerListenerOnChat() {
        var listener = mock(ModelUsageListener.class);
        var registry = new ModelProviderRegistry(List.of(openai), List.of(listener));

        registry.chat("openai", sampleRequest("gpt-4o"));

        verify(listener).onUsage("openai", "gpt-4o", new TokenUsage(10, 20, 30));
    }

    @Test
    void shouldNotTriggerListener_whenProviderNotFound() {
        var listener = mock(ModelUsageListener.class);
        var registry = new ModelProviderRegistry(List.of(openai), List.of(listener));

        assertThatThrownBy(() -> registry.chat("unknown", sampleRequest("gpt-4o")))
                .isInstanceOf(DomainException.class);
        verifyNoInteractions(listener);
    }

    @Test
    void shouldNotPropagateListenerException() {
        var badListener = mock(ModelUsageListener.class);
        doThrow(new RuntimeException("billing down")).when(badListener)
                .onUsage(any(), any(), any());
        var registry = new ModelProviderRegistry(List.of(openai), List.of(badListener));

        // chat() must succeed despite listener failure
        ChatResponse response = registry.chat("openai", sampleRequest("gpt-4o"));
        assertThat(response.content()).isEqualTo("response from openai");
    }
```

- [ ] **Step 2: Run to verify the 3 new tests fail**

```bash
./gradlew :cartisan-ai:test --tests "com.cartisan.ai.provider.ModelProviderRegistryTest" 2>&1 | tail -20
```

Expected: `shouldTriggerListenerOnChat`, `shouldNotTriggerListener_whenProviderNotFound`, `shouldNotPropagateListenerException` FAIL with `UnsupportedOperationException`.

- [ ] **Step 3: Implement chat() in ModelProviderRegistry**

Replace the `chat()` stub:

```java
    public ChatResponse chat(String providerId, ChatRequest request) {
        ModelProvider provider = getProvider(providerId);
        ChatResponse response = provider.chat(request);
        notifyListeners(provider.id(), response.model(), response.usage());
        return response;
    }
```

- [ ] **Step 4: Run all tests to verify they pass**

```bash
./gradlew :cartisan-ai:test --tests "com.cartisan.ai.provider.ModelProviderRegistryTest" 2>&1 | tail -20
```

Expected: all 10 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelProviderRegistry.java \
        cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelProviderRegistryTest.java
git commit -m "feat(cartisan-ai): F05-04 Registry sync delegation — chat() + Listener triggering"
```

---

## Task 3: Streaming delegation — chatStream() + exactly-once Listener

### Files
- Modify: `cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelProviderRegistryTest.java`
- Modify: `cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelProviderRegistry.java`

---

- [ ] **Step 1: Add streaming tests to the test file**

Append these 4 tests inside `ModelProviderRegistryTest`:

```java
    // ── Streaming delegation tests ────────────────────────────────────────────

    @Test
    void shouldTriggerListenerOnStream_exactlyOnce() {
        var listener = mock(ModelUsageListener.class);
        // Provider that emits two finished=true events (protocol violation)
        var badProtocolProvider = new FakeProvider("openai", "gpt-4o") {
            @Override
            public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
                return Flux.just(
                    new ChatStreamEvent("a", false, null),
                    new ChatStreamEvent("", true, new TokenUsage(10, 20, 30)),
                    new ChatStreamEvent("", true, new TokenUsage(99, 99, 99)) // duplicate finish
                );
            }
        };
        var registry = new ModelProviderRegistry(List.of(badProtocolProvider), List.of(listener));

        registry.chatStream("openai", sampleRequest("gpt-4o")).collectList().block();

        // Listener must fire exactly once with the FIRST finished event's usage
        verify(listener, times(1)).onUsage("openai", "gpt-4o", new TokenUsage(10, 20, 30));
    }

    @Test
    void shouldNotPropagateListenerException_onStream() {
        var badListener = mock(ModelUsageListener.class);
        doThrow(new RuntimeException("billing down")).when(badListener)
                .onUsage(any(), any(), any());
        var registry = new ModelProviderRegistry(List.of(openai), List.of(badListener));

        // Flux must complete normally despite listener failure
        List<ChatStreamEvent> events = registry.chatStream("openai", sampleRequest("gpt-4o"))
                .collectList().block();
        assertThat(events).hasSize(2);
        assertThat(events.getLast().finished()).isTrue();
    }

    @Test
    void shouldSkipListener_whenStreamFinishedEventHasNullUsage() {
        var listener = mock(ModelUsageListener.class);
        var nullUsageProvider = new FakeProvider("openai", "gpt-4o") {
            @Override
            public Flux<ChatStreamEvent> chatStream(ChatRequest request) {
                return Flux.just(new ChatStreamEvent("", true, null)); // finished but no usage
            }
        };
        var registry = new ModelProviderRegistry(List.of(nullUsageProvider), List.of(listener));

        registry.chatStream("openai", sampleRequest("gpt-4o")).collectList().block();

        verifyNoInteractions(listener);
    }

    @Test
    void shouldNotTriggerListenerOnStream_whenProviderNotFound() {
        var listener = mock(ModelUsageListener.class);
        var registry = new ModelProviderRegistry(List.of(openai), List.of(listener));

        assertThatThrownBy(() -> registry.chatStream("unknown", sampleRequest("gpt-4o")))
                .isInstanceOf(DomainException.class);
        verifyNoInteractions(listener);
    }
```

- [ ] **Step 2: Run to verify the 4 new tests fail**

```bash
./gradlew :cartisan-ai:test --tests "com.cartisan.ai.provider.ModelProviderRegistryTest" 2>&1 | tail -20
```

Expected: 4 streaming tests FAIL with `UnsupportedOperationException`.

- [ ] **Step 3: Implement chatStream() in ModelProviderRegistry**

Replace the `chatStream()` stub. Note `AtomicBoolean` is already imported from `java.util.concurrent.atomic`:

```java
    public Flux<ChatStreamEvent> chatStream(String providerId, ChatRequest request) {
        ModelProvider provider = getProvider(providerId);
        return Flux.defer(() -> {
            AtomicBoolean triggered = new AtomicBoolean(false);
            return provider.chatStream(request)
                    .doOnNext(event -> {
                        if (event.finished()
                                && event.usage() != null
                                && triggered.compareAndSet(false, true)) {
                            notifyListeners(provider.id(), request.model(), event.usage());
                        }
                    });
        });
    }
```

- [ ] **Step 4: Run all tests to verify they all pass**

```bash
./gradlew :cartisan-ai:test --tests "com.cartisan.ai.provider.ModelProviderRegistryTest" 2>&1 | tail -20
```

Expected: all 14 tests PASS.

- [ ] **Step 5: Run the full cartisan-ai test suite**

```bash
./gradlew :cartisan-ai:test 2>&1 | tail -20
```

Expected: all tests PASS (including existing F05-01 ~ F05-03 tests).

- [ ] **Step 6: Commit**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/provider/ModelProviderRegistry.java \
        cartisan-ai/src/test/java/com/cartisan/ai/provider/ModelProviderRegistryTest.java
git commit -m "feat(cartisan-ai): F05-04 Registry streaming delegation — chatStream() + exactly-once Listener"
```

---

## Completion Checklist

- [ ] All 14 tests in `ModelProviderRegistryTest` pass
- [ ] Full `cartisan-ai` test suite passes (`./gradlew :cartisan-ai:test`)
- [ ] No `@Component` on `ModelProviderRegistry` (Spring wiring deferred to F05-09)
- [ ] `chat()` and `chatStream()` stubs removed; `notifyListeners()` helper shared by both
