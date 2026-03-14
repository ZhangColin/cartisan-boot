package com.cartisan.event;

import com.cartisan.core.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 基于 Spring Events 的领域事件发布器实现。
 *
 * <p>将 {@link DomainEvent} 直接发布到 Spring 事件总线，Spring 会自动将其包装为
 * {@code PayloadApplicationEvent<DomainEvent>}。监听器可以直接接收领域事件子类。</p>
 *
 * <h2>事件监听</h2>
 *
 * <pre>{@code
 * // 直接接收领域事件子类
 * @EventListener
 * void handle(OrderCreatedEvent event) {
 *     // 处理事件
 * }
 *
 * // 或使用事务后监听
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
 * void handleAfterCommit(OrderCreatedEvent event) {
 *     // 事务提交后处理
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 创建 Spring 事件发布器。
     *
     * @param applicationEventPublisher Spring 事件发布器，不能为 null
     * @throws NullPointerException 如果 applicationEventPublisher 为 null
     */
    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = java.util.Objects.requireNonNull(
            applicationEventPublisher,
            "applicationEventPublisher cannot be null"
        );
    }

    @Override
    public void publish(DomainEvent event) {
        java.util.Objects.requireNonNull(event, "event cannot be null");
        applicationEventPublisher.publishEvent(event);
    }
}
