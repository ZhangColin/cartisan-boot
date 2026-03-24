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

class SseHelperTest {

    @Test
    void shouldSendEventsToSseEmitter_whenStreamCompletes() throws IOException {
        // Given: SseHelper and a Flux with 3 events
        SseProperties properties = new SseProperties();
        SseHelper sseHelper = new SseHelper(properties);

        TokenUsage usage = new TokenUsage(10, 20, 30);
        List<ChatStreamEvent> events = List.of(
            new ChatStreamEvent("Hello", false, null),
            new ChatStreamEvent(" World", false, null),
            new ChatStreamEvent("!", true, usage)
        );

        // When: call toSse
        SseEmitter emitter = sseHelper.toSse(Flux.fromIterable(events));

        // Then: emitter created with correct timeout
        assertThat(emitter.getTimeout()).isEqualTo(properties.getTimeout().toMillis());
    }

    @Test
    void shouldInvokeUsageCallback_whenStreamCompletes() throws InterruptedException {
        // Given
        SseProperties properties = new SseProperties();
        SseHelper sseHelper = new SseHelper(properties);

        TokenUsage usage = new TokenUsage(10, 20, 30);
        Flux<ChatStreamEvent> events = Flux.just(
            new ChatStreamEvent("Hello", false, null),
            new ChatStreamEvent("!", true, usage)
        );

        AtomicReference<TokenUsage> capturedUsage = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // When
        SseEmitter emitter = sseHelper.toSse(events, u -> {
            capturedUsage.set(u);
            latch.countDown();
        });

        // Then: 等待异步回调完成
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(capturedUsage.get()).isEqualTo(usage);
        assertThat(emitter.getTimeout()).isEqualTo(properties.getTimeout().toMillis());
    }

    @Test
    void shouldCompleteWithError_whenFluxErrors() {
        // Given
        SseProperties properties = new SseProperties();
        SseHelper sseHelper = new SseHelper(properties);

        RuntimeException expectedError = new RuntimeException("Stream error");
        Flux<ChatStreamEvent> events = Flux.error(expectedError);

        // When
        SseEmitter emitter = sseHelper.toSse(events);

        // Then: emitter 创建成功且配置了正确的超时
        assertThat(emitter.getTimeout()).isEqualTo(properties.getTimeout().toMillis());

        // 验证 emitter 处于错误完成状态：
        // 尝试发送事件到已因错误完成的 emitter 会抛出异常
        assertThatThrownBy(() -> emitter.send(SseEmitter.event().data("test")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already completed with error");
    }

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

            AtomicReference<TokenUsage> capturedUsage = new AtomicReference<>();

            // When
            SseEmitter emitter = sseHelper.toSse(emptyEvents, capturedUsage::set);

            // Then: emitter 创建成功，空流立即完成但不调用回调（因为没有 usage）
            assertThat(emitter).isNotNull();
            assertThat(capturedUsage.get()).isNull();
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

            AtomicReference<TokenUsage> capturedUsage = new AtomicReference<>();

            // When
            SseEmitter emitter = sseHelper.toSse(events, capturedUsage::set);

            // Then: 短暂等待确保流已处理，但回调不应被调用
            Thread.sleep(100);
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

    @Nested
    @DisplayName("Emitter 回调测试")
    class EmitterCallbackTests {

        @Test
        @DisplayName("给定永不发射的流 - 调用 toSse - emitter 正常创建")
        void shouldHandleNeverStream() {
            // Given
            SseProperties properties = new SseProperties();
            SseHelper sseHelper = new SseHelper(properties);

            // 创建一个永不发射的 Flux（模拟客户端可能随时断开）
            Flux<ChatStreamEvent> events = Flux.never();

            // When
            SseEmitter emitter = sseHelper.toSse(events);

            // Then: emitter 创建成功，回调已注册
            assertThat(emitter).isNotNull();
            assertThat(emitter.getTimeout()).isEqualTo(properties.getTimeout().toMillis());
        }

        @Test
        @DisplayName("给定延迟发射的流 - 调用 toSse - 正确处理延迟事件")
        void shouldHandleDelayedEvents() throws InterruptedException {
            // Given
            SseProperties properties = new SseProperties();
            SseHelper sseHelper = new SseHelper(properties);

            TokenUsage usage = new TokenUsage(10, 5, 15);
            // 创建一个延迟发射的 Flux，最后一个事件包含 usage
            Flux<ChatStreamEvent> events = Flux.just(
                new ChatStreamEvent("Hello", false, null),
                new ChatStreamEvent("!", true, usage)
            ).delayElements(java.time.Duration.ofMillis(10));

            AtomicReference<TokenUsage> capturedUsage = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            // When
            SseEmitter emitter = sseHelper.toSse(events, u -> {
                capturedUsage.set(u);
                latch.countDown();
            });

            // Then: 等待异步完成
            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(capturedUsage.get()).isEqualTo(usage);
            assertThat(emitter.getTimeout()).isEqualTo(properties.getTimeout().toMillis());
        }
    }
}
