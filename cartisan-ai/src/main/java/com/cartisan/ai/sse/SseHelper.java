package com.cartisan.ai.sse;

import com.cartisan.ai.model.ChatStreamEvent;
import com.cartisan.ai.model.TokenUsage;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * SSE 流式输出工具。
 *
 * <p>将 {@link Flux Flux&lt;ChatStreamEvent&gt;} 转换为 {@link SseEmitter}，
 * 封装超时、异常和客户端断开处理。
 */
@Component
public class SseHelper {

    private final SseProperties properties;

    public SseHelper(SseProperties properties) {
        this.properties = properties;
    }

    /**
     * 将流式事件转换为 SSE Emitter（使用默认配置）。
     *
     * @param events 事件流
     * @return SseEmitter
     */
    public SseEmitter toSse(Flux<ChatStreamEvent> events) {
        return toSse(events, null);
    }

    /**
     * 将流式事件转换为 SSE Emitter，流结束时回调 usage。
     *
     * @param events 事件流
     * @param usageCallback 流完成时的 Token 使用回调
     * @return SseEmitter
     */
    public SseEmitter toSse(Flux<ChatStreamEvent> events,
                            Consumer<TokenUsage> usageCallback) {
        // TODO: 实现逻辑
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
