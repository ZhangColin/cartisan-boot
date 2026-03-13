package com.cartisan.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 领域事件基类。
 *
 * <p>领域事件表示在领域中发生的有意义的事情，通常由聚合根发布。</p>
 *
 * <h2>事件元数据</h2>
 *
 * <ul>
 *   <li>{@code eventId} - 事件的唯一标识符，自动生成 UUID</li>
 *   <li>{@code occurredAt} - 事件发生时间，自动设置为当前时间</li>
 *   <li>{@code aggregateId} - 关联的聚合根 ID，由子类提供</li>
 *   <li>{@code eventType} - 事件类型名称，默认为类名</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 *
 * <pre>{@code
 * public class OrderCreatedEvent extends DomainEvent {
 *     private final String customerId;
 *     private final BigDecimal totalAmount;
 *
 *     public OrderCreatedEvent(String orderId, String customerId, BigDecimal totalAmount) {
 *         super(orderId);
 *         this.customerId = customerId;
 *         this.totalAmount = totalAmount;
 *     }
 *
 *     public String getCustomerId() {
 *         return customerId;
 *     }
 *
 *     public BigDecimal getTotalAmount() {
 *         return totalAmount;
 *     }
 * }
 *
 * // 在聚合根中使用
 * public class Order extends AbstractAggregateRoot<Order> {
 *     public void create(String customerId, BigDecimal totalAmount) {
 *         // 业务逻辑...
 *         registerEvent(new OrderCreatedEvent(id.value(), customerId, totalAmount));
 *     }
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant occurredAt;
    private final String aggregateId;

    /**
     * 创建领域事件。
     *
     * @param aggregateId 关联的聚合根 ID，不能为 null
     * @throws NullPointerException 如果 aggregateId 为 null
     */
    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId cannot be null");
    }

    /**
     * 获取事件唯一标识符。
     *
     * @return 事件 ID，格式为 UUID 字符串
     */
    public String eventId() {
        return eventId;
    }

    /**
     * 获取事件发生时间。
     *
     * @return 事件发生时间（UTC）
     */
    public Instant occurredAt() {
        return occurredAt;
    }

    /**
     * 获取关联的聚合根 ID。
     *
     * @return 聚合根 ID
     */
    public String aggregateId() {
        return aggregateId;
    }

    /**
     * 获取事件类型名称。
     *
     * @return 事件类型名称，默认为简单类名
     */
    public String eventType() {
        return this.getClass().getSimpleName();
    }
}
