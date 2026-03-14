package com.cartisan.event;

import com.cartisan.core.domain.DomainEvent;

/**
 * 领域事件发布器接口。
 *
 * <p>定义了发布领域事件的契约，实现类负责将事件发布到具体的事件总线（如 Spring Events）。</p>
 *
 * <h2>使用示例</h2>
 *
 * <pre>{@code
 * // 在应用服务或 Repository 中注入
 * public class OrderService {
 *     private final DomainEventPublisher eventPublisher;
 *
 *     public OrderService(DomainEventPublisher eventPublisher) {
 *         this.eventPublisher = eventPublisher;
 *     }
 *
 *     public void completeOrder(Order order) {
 *         order.complete();
 *         // 发布事件
 *         eventPublisher.publish(order.getDomainEvents());
 *     }
 * }
 * }</pre>
 *
 * <h2>事件发布时机</h2>
 *
 * <p>事件通常在事务内同步发布。监听器建议使用
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)}
 * 获得事务提交后执行语义，确保事件对应已持久化的数据。</p>
 *
 * @since 0.1.0
 */
public interface DomainEventPublisher {

    /**
     * 发布领域事件。
     *
     * <p>事件将被发布到事件总线，匹配的监听器将接收到事件通知。</p>
     *
     * @param event 要发布的领域事件，不能为 null
     * @throws NullPointerException 如果 event 为 null
     */
    void publish(DomainEvent event);
}
