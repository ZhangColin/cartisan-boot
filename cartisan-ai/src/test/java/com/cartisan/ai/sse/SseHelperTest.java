package com.cartisan.ai.sse;

import com.cartisan.ai.model.ChatStreamEvent;
import com.cartisan.ai.model.TokenUsage;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

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
}
