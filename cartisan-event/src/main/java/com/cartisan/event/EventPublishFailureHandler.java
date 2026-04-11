package com.cartisan.event;

/**
 * 事件发布失败处理器。
 *
 * <p>当事件发布到某个发布器失败时，通过此接口进行处理。
 * 默认实现为 {@link LoggingEventPublishFailureHandler}（记录 ERROR 日志）。</p>
 *
 * <p>如需自定义失败处理策略（如发送告警、重试等），
 * 只需注册一个实现此接口的 Spring Bean 即可自动注入。</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface EventPublishFailureHandler {

    /**
     * 处理事件发布失败。
     *
     * @param publisherType 发布器类型标识
     * @param event 发布失败的事件
     * @param exception 发布过程中抛出的异常
     */
    void onFailure(String publisherType, ApplicationEvent event, Exception exception);
}
