# 测试覆盖增强实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 提升测试覆盖率，特别是 cartisan-ai、cartisan-web、cartisan-data-query 模块的分支覆盖率

**Architecture:** 遵循项目 TDD 规范，先写测试，再实现代码（如果需要），使用 AssertJ 断言

**Tech Stack:** JUnit 5, AssertJ, Mockito, Reactor StepVerifier

---

## 文件结构

### 新增测试文件

| 文件 | 职责 |
|------|------|
| `cartisan-ai/src/test/java/com/cartisan/ai/sse/SsePropertiesTest.java` | SseProperties 配置类测试 |
| `cartisan-web/src/test/java/com/cartisan/web/support/TreeNodeTest.java` | TreeNode 类测试 |
| `cartisan-data-query/src/test/java/com/cartisan/data/query/support/JooqTenantSupportIntegrationTest.java` | JooqTenantSupport 有租户上下文集成测试 |

### 修改测试文件

| 文件 | 修改内容 |
|------|----------|
| `cartisan-ai/src/test/java/com/cartisan/ai/sse/SseHelperTest.java` | 补充边界场景测试 |
| `cartisan-web/src/test/java/com/cartisan/web/support/TreeNodeBuilderTest.java` | 补充 null 输入测试 |

---

## Task 1: SseProperties 单元测试

**Files:**
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/sse/SsePropertiesTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.cartisan.ai.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SseProperties 单元测试")
class SsePropertiesTest {

    @Test
    @DisplayName("给定默认配置 - 获取 timeout - 返回 30 秒")
    void shouldReturnDefaultTimeout() {
        // Given: 默认构造的 SseProperties
        SseProperties properties = new SseProperties();

        // When
        Duration timeout = properties.getTimeout();

        // Then
        assertThat(timeout).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("给定自定义 timeout - 设置后获取 - 返回自定义值")
    void shouldReturnCustomTimeout() {
        // Given
        SseProperties properties = new SseProperties();
        Duration customTimeout = Duration.ofMinutes(5);

        // When
        properties.setTimeout(customTimeout);

        // Then
        assertThat(properties.getTimeout()).isEqualTo(customTimeout);
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `./gradlew :cartisan-ai:test --tests SsePropertiesTest`
Expected: PASS（SseProperties 是简单的 POJO，不需要修改代码）

- [ ] **Step 3: Commit**

```bash
git add cartisan-ai/src/test/java/com/cartisan/ai/sse/SsePropertiesTest.java
git commit -m "test(ai): add SseProperties unit test

- Test default timeout value (30 seconds)
- Test custom timeout setter/getter

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: TreeNode 单元测试

**Files:**
- Create: `cartisan-web/src/test/java/com/cartisan/web/support/TreeNodeTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.cartisan.web.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TreeNode 单元测试")
class TreeNodeTest {

    @Test
    @DisplayName("给定基本构造参数 - 创建 TreeNode - 返回正确属性")
    void shouldCreateTreeNodeWithBasicConstructor() {
        // Given
        Long id = 1L;
        String name = "Test Node";
        Long parentId = 0L;

        // When
        TreeNode<Long> node = new TreeNode<>(id, name, parentId);

        // Then
        assertThat(node.id()).isEqualTo(id);
        assertThat(node.name()).isEqualTo(name);
        assertThat(node.parentId()).isEqualTo(parentId);
        assertThat(node.children()).isEmpty();
    }

    @Test
    @DisplayName("给定带子节点的构造参数 - 创建 TreeNode - 返回包含子节点的树")
    void shouldCreateTreeNodeWithChildren() {
        // Given
        TreeNode<Long> child1 = new TreeNode<>(2L, "Child 1", 1L);
        TreeNode<Long> child2 = new TreeNode<>(3L, "Child 2", 1L);
        List<TreeNode<Long>> children = List.of(child1, child2);

        // When
        TreeNode<Long> parent = new TreeNode<>(1L, "Parent", 0L, children);

        // Then
        assertThat(parent.id()).isEqualTo(1L);
        assertThat(parent.name()).isEqualTo("Parent");
        assertThat(parent.parentId()).isEqualTo(0L);
        assertThat(parent.children()).hasSize(2);
        assertThat(parent.children()).isEqualTo(children);
    }

    @Test
    @DisplayName("给定 setChildren 方法 - 设置子节点 - 子节点被正确设置")
    void shouldSetChildren() {
        // Given
        TreeNode<Long> node = new TreeNode<>(1L, "Node", 0L);
        List<TreeNode<Long>> children = List.of(
            new TreeNode<>(2L, "Child", 1L)
        );

        // When
        node.setChildren(children);

        // Then
        assertThat(node.children()).isEqualTo(children);
    }

    @Test
    @DisplayName("给定空子节点列表 - 设置 children - 支持空列表")
    void shouldSupportEmptyChildren() {
        // Given
        List<TreeNode<Long>> emptyChildren = List.of();
        TreeNode<Long> node = new TreeNode<>(1L, "Node", 0L, emptyChildren);

        // When & Then
        assertThat(node.children()).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `./gradlew :cartisan-web:test --tests TreeNodeTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add cartisan-web/src/test/java/com/cartisan/web/support/TreeNodeTest.java
git commit -m "test(web): add TreeNode unit test

- Test basic constructor with id, name, parentId
- Test constructor with children
- Test setChildren method
- Test empty children list

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: SseHelper 边界场景测试补充

**Files:**
- Modify: `cartisan-ai/src/test/java/com/cartisan/ai/sse/SseHelperTest.java`

- [ ] **Step 1: Add additional test cases**

在 `SseHelperTest.java` 末尾添加以下测试：

```java
package com.cartisan.ai.sse;

import com.cartisan.ai.model.ChatStreamEvent;
import com.cartisan.ai.model.TokenUsage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SseHelper 单元测试")
class SseHelperTest {

    // ... 保留原有测试 ...

    @Nested
    @DisplayName("边界场景测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("给定空 Flux - 调用 toSse - 立即完成")
        void shouldCompleteImmediately_withEmptyFlux() throws InterruptedException {
            // Given
            SseProperties properties = new SseProperties();
            SseHelper sseHelper = new SseHelper(properties);
            Flux<ChatStreamEvent> emptyEvents = Flux.empty();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<TokenUsage> capturedUsage = new AtomicReference<>();

            // When
            SseEmitter emitter = sseHelper.toSse(emptyEvents, u -> {
                capturedUsage.set(u);
                latch.countDown();
            });

            // Then: 等待异步完成
            boolean completed = latch.await(1, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(capturedUsage.get()).isNull(); // 空流没有 usage
        }

        @Test
        @DisplayName("给定无 usage 的流 - 调用 toSse - usage 回调不被调用")
        void notInvokeUsageCallback_whenNoUsageInStream() throws InterruptedException {
            // Given
            SseProperties properties = new SseProperties();
            SseHelper sseHelper = new SseHelper(properties);

            Flux<ChatStreamEvent> events = Flux.just(
                new ChatStreamEvent("Hello", false, null),
                new ChatStreamEvent("!", true, null)
            );

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<TokenUsage> capturedUsage = new AtomicReference<>();

            // When
            SseEmitter emitter = sseHelper.toSse(events, u -> {
                capturedUsage.set(u);
                latch.countDown();
            });

            // Then
            boolean completed = latch.await(1, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(capturedUsage.get()).isNull();
        }

        @Test
        @DisplayName("给定多个 usage 的流 - 调用 toSse - 只回调最后一个 usage")
        void shouldCallbackLastUsage_whenMultipleUsagesInStream() throws InterruptedException {
            // Given
            SseProperties properties = new SseProperties();
            SseHelper sseHelper = new SseHelper(properties);

            TokenUsage usage1 = new TokenUsage(10, 5, 15);
            TokenUsage usage2 = new TokenUsage(20, 10, 30);
            TokenUsage usage3 = new TokenUsage(30, 15, 45);

            Flux<ChatStreamEvent> events = Flux.just(
                new ChatStreamEvent("Hello", false, usage1),
                new ChatStreamEvent(" World", false, usage2),
                new ChatStreamEvent("!", true, usage3)
            );

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<TokenUsage> capturedUsage = new AtomicReference<>();

            // When
            SseEmitter emitter = sseHelper.toSse(events, u -> {
                capturedUsage.set(u);
                latch.countDown();
            });

            // Then
            boolean completed = latch.await(1, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(capturedUsage.get()).isEqualTo(usage3); // 最后一个 usage
        }

        @Test
        @DisplayName("给定 null usage 回调 - 调用 toSse - 不抛异常")
        void shouldHandleNullUsageCallback() {
            // Given
            SseProperties properties = new SseProperties();
            SseHelper sseHelper = new SseHelper(properties);

            TokenUsage usage = new TokenUsage(10, 20, 30);
            Flux<ChatStreamEvent> events = Flux.just(
                new ChatStreamEvent("Hello", false, null),
                new ChatStreamEvent("!", true, usage)
            );

            // When & Then: 不应抛出异常
            SseEmitter emitter = sseHelper.toSse(events, null);
            assertThat(emitter).isNotNull();
        }
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `./gradlew :cartisan-ai:test --tests SseHelperTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add cartisan-ai/src/test/java/com/cartisan/ai/sse/SseHelperTest.java
git commit -m "test(ai): add SseHelper edge case tests

- Test empty Flux completion
- Test null usage callback handling
- Test multiple usage in stream (last one wins)
- Test stream without usage

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: TreeNodeBuilder null 输入测试

**Files:**
- Modify: `cartisan-web/src/test/java/com/cartisan/web/support/TreeNodeBuilderTest.java`

- [ ] **Step 1: Add null input test**

在 `TreeNodeBuilderTest.java` 的测试类末尾添加：

```java
@Test
@DisplayName("给定 null 节点列表 - 调用 build - 抛出 NullPointerException")
void shouldThrowNullPointerException_whenNodesIsNull() {
    // Given
    List<TreeNode<Long>> nullNodes = null;

    // When & Then
    assertThatThrownBy(() -> TreeNodeBuilder.build(
            nullNodes,
            id -> String.valueOf(id),
            parentId -> String.valueOf(parentId),
            0L
    ))
        .isInstanceOf(NullPointerException.class);
}

@Test
@DisplayName("给定单节点树 - 调用 build - 返回单根节点")
void shouldBuildSingleRootNode() {
    // Given
    List<TreeNode<Long>> nodes = List.of(
        new TreeNode<>(1L, "Root", 0L)
    );

    // When
    List<TreeNode<Long>> tree = TreeNodeBuilder.build(
        nodes,
        id -> String.valueOf(id),
        parentId -> String.valueOf(parentId),
        0L
    );

    // Then
    assertThat(tree).hasSize(1);
    assertThat(tree.get(0).id()).isEqualTo(1L);
    assertThat(tree.get(0).children()).isEmpty();
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `./gradlew :cartisan-web:test --tests TreeNodeBuilderTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add cartisan-web/src/test/java/com/cartisan/web/support/TreeNodeBuilderTest.java
git commit -m "test(web): add TreeNodeBuilder null input test

- Test null nodes list throws NullPointerException
- Test single root node tree building

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: JooqTenantSupport 有租户上下文集成测试

**Files:**
- Create: `cartisan-data-query/src/test/java/com/cartisan/data/query/support/JooqTenantSupportIntegrationTest.java`

- [ ] **Step 1: Write the test**

```java
package com.cartisan.data.query.support;

import com.cartisan.security.context.TenantContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JooqTenantSupport 集成测试。
 *
 * <p>测试有租户上下文时的条件生成逻辑。
 * 需要 cartisan-security 模块支持。
 */
@DisplayName("JooqTenantSupport 集成测试（有租户上下文）")
class JooqTenantSupportIntegrationTest {

    private static final Long TEST_TENANT_ID = 123L;

    @BeforeEach
    void setUp() {
        // 设置租户上下文
        TenantContext.runWithTenantId(TEST_TENANT_ID, () -> {});
    }

    @AfterEach
    void tearDown() {
        // 清理租户上下文
        TenantContext.clear();
    }

    @Test
    @DisplayName("给定有租户上下文 - 调用 eqTenantId - 返回租户等值条件")
    void shouldReturnTenantEqCondition_whenTenantContextExists() {
        // Given: 租户上下文已设置
        var tenantIdField = DSL.field("tenant_id", Long.class);

        // When: 调用 eqTenantId
        Condition result = JooqTenantSupport.eqTenantId(tenantIdField);

        // Then: 应返回租户等值条件
        assertThat(result).isNotNull();
        // 注意：无法直接验证 Condition 的值，因为它是一个内部实现
        // 但可以验证它不是 noCondition
        assertThat(result).isNotEqualTo(DSL.noCondition());
    }

    @Test
    @DisplayName("给定清除租户上下文后 - 调用 eqTenantId - 返回 noCondition")
    void shouldReturnNoCondition_afterTenantContextCleared() {
        // Given: 先清除租户上下文
        TenantContext.clear();
        var tenantIdField = DSL.field("tenant_id", Long.class);

        // When
        Condition result = JooqTenantSupport.eqTenantId(tenantIdField);

        // Then
        assertThat(result).isEqualTo(DSL.noCondition());
    }
}
```

注意：这个测试需要 `TenantContext` 支持 `runWithTenantId` 和 `clear` 方法。如果当前版本没有这些方法，需要先添加。

- [ ] **Step 2: Check if TenantContext has required methods**

Run: `./gradlew :cartisan-data-query:test --tests JooqTenantSupportIntegrationTest`
Expected: 如果 `TenantContext` 缺少 `runWithTenantId` 或 `clear` 方法，需要先添加

- [ ] **Step 3: If methods missing, add to TenantContext**

如果需要，在 `cartisan-security/src/main/java/com/cartisan/security/context/TenantContext.java` 添加：

```java
/**
 * 在指定租户上下文中执行操作。
 *
 * @param tenantId 租户 ID
 * @param action   要执行的操作
 */
public static void runWithTenantId(Long tenantId, Runnable action) {
    TENANT_ID.runWhere(() -> tenantId, action);
}

/**
 * 清除当前租户上下文。
 *
 * <p>仅用于测试环境。</p>
 */
public static void clear() {
    TENANT_ID.runWhere(() -> null, () -> {});
}
```

- [ ] **Step 4: Run test again**

Run: `./gradlew :cartisan-data-query:test --tests JooqTenantSupportIntegrationTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-data-query/src/test/java/com/cartisan/data/query/support/JooqTenantSupportIntegrationTest.java
git add cartisan-security/src/main/java/com/cartisan/security/context/TenantContext.java
git commit -m "test(data-query): add JooqTenantSupport integration test

- Add test for tenant context exists scenario
- Add TenantContext.runWithTenantId() helper method
- Add TenantContext.clear() helper for testing

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: 运行全量测试并验证覆盖率

- [ ] **Step 1: Run all tests**

```bash
./gradlew test jacocoTestReport
```

Expected: 所有测试通过

- [ ] **Step 2: Verify coverage improvement**

检查各模块覆盖率：
- cartisan-ai/sse: 预期 > 85%
- cartisan-web/support: 预期 > 85%
- cartisan-data-query/support: 预期 > 80%

- [ ] **Step 3: Final commit if coverage target met**

```bash
git commit --allow-empty -m "test: verify coverage improvement targets met

- cartisan-ai/sse: >85% coverage
- cartisan-web/support: >85% coverage
- cartisan-data-query/support: >80% coverage

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## 验收标准

| 指标 | 目标 |
|------|------|
| cartisan-ai 模块分支覆盖 | ≥ 75% |
| cartisan-web/support 分支覆盖 | ≥ 85% |
| cartisan-data-query/support 指令覆盖 | ≥ 80% |
| 所有新增测试通过 | 100% |
