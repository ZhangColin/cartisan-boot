package com.cartisan.event.example;

import com.cartisan.event.ApplicationEvent;
import com.cartisan.event.PublishTo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 订单创建事件（示例）。
 */
@PublishTo({"spring", "rabbitmq"})
public record OrderCreatedEvent(
    String eventId,
    Instant occurredAt,
    Long orderId,
    String customerId,
    BigDecimal totalAmount
) implements ApplicationEvent {

    public OrderCreatedEvent(Long orderId, String customerId, BigDecimal totalAmount) {
        this(UUID.randomUUID().toString(), Instant.now(), orderId, customerId, totalAmount);
    }

    @Override
    public String eventType() {
        return "order.created";
    }
}
