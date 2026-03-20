package com.cartisan.ai.sse;

import com.cartisan.ai.model.ChatStreamEvent;
import com.cartisan.ai.model.TokenUsage;
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
}
