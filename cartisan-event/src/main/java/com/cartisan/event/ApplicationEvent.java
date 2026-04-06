package com.cartisan.event;

import java.time.Instant;

/**
 * 应用事件基类。
 *
 * <p>应用事件用于跨上下文、跨系统的异步通信。</p>
 *
 * <p>当前实现：基于Spring事件机制（进程内）</p>
 * <p>未来扩展：支持RabbitMQ、Kafka等消息队列（跨进程）</p>
 *
 * <h3>事件元数据</h3>
 * <ul>
 *   <li>{@code eventId} - 事件的唯一标识符</li>
 *   <li>{@code occurredAt} - 事件发生时间</li>
 *   <li>{@code eventType} - 事件类型名称，用于路由</li>
 * </ul>
 *
 * <h3>实现建议</h3>
 * <p>推荐使用 Java Record 来实现事件类，确保不可变性和可序列化性。</p>
 *
 * @since 0.1.0
 */
public interface ApplicationEvent {

    /**
     * 获取事件唯一标识符。
     * @return 事件ID，格式为UUID字符串
     */
    String eventId();

    /**
     * 获取事件发生时间。
     * @return 事件发生时间（UTC）
     */
    Instant occurredAt();

    /**
     * 获取事件类型名称。
     * <p>用于消息队列的路由key或topic名称</p>
     * @return 事件类型，如 "order.created"
     */
    String eventType();
}
