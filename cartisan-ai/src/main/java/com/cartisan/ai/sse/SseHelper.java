package com.cartisan.ai.sse;

import com.cartisan.ai.model.ChatStreamEvent;
import com.cartisan.ai.model.TokenUsage;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
        // 创建 SseEmitter，配置超时
        SseEmitter emitter = new SseEmitter(properties.getTimeout().toMillis());

        // 用于累积最后一个 usage
        AtomicReference<TokenUsage> lastUsage = new AtomicReference<>();
        // 防止多次 complete
        AtomicBoolean completed = new AtomicBoolean();

        // 配置超时处理
        emitter.onTimeout(() -> {
            if (completed.compareAndSet(false, true)) {
                emitter.complete();
            }
        });

        // 配置错误处理
        emitter.onError(ex -> {
            if (completed.compareAndSet(false, true)) {
                emitter.completeWithError(ex);
            }
        });

        // 订阅 Flux，发送事件
        Disposable disposable = events.doOnNext(event -> {
            try {
                emitter.send(event);
                // 累积 usage
                if (event.usage() != null) {
                    lastUsage.set(event.usage());
                }
            } catch (IOException e) {
                if (completed.compareAndSet(false, true)) {
                    emitter.completeWithError(e);
                }
            }
        })
        .doOnError(ex -> {
            if (completed.compareAndSet(false, true)) {
                emitter.completeWithError(ex);
            }
        })
        .doOnCancel(() -> {
            // 客户端断开，Flux 自动取消
        })
        .doFinally(signalType -> {
            // 回调最后一个 usage
            if (usageCallback != null) {
                usageCallback.accept(lastUsage.get());
            }
            if (completed.compareAndSet(false, true)) {
                emitter.complete();
            }
        })
        .subscribe();

        // 确保 emitter 取消时也取消 Flux 订阅
        emitter.onCompletion(() -> {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }
        });

        return emitter;
    }
}
