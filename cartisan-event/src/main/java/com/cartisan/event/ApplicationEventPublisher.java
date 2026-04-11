package com.cartisan.event;

/**
 * 应用事件发布器接口。
 *
 * <p>定义了发布应用事件的契约，实现类负责将事件发布到具体的
 * 事件总线（如Spring Events、RabbitMQ、Kafka）。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 在应用服务中注入
 * public class OrderService {
 *     private final ApplicationEventPublisher eventPublisher;
 *
 *     public OrderService(ApplicationEventPublisher eventPublisher) {
 *         this.eventPublisher = eventPublisher;
 *     }
 *
 *     @Transactional
 *     public void createOrder(...) {
 *         Order order = new Order(...);
 *         orderRepository.save(order);
 *         // 手动发布应用事件
 *         eventPublisher.publishApplicationEvent(new OrderCreatedEvent(...));
 *     }
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
public interface ApplicationEventPublisher {

    /**
     * 获取发布器类型标识。
     * @return 类型标识，如 "spring"、"rabbitmq"、"kafka"
     */
    String getType();

    /**
     * 发布应用事件。
     *
     * @param event 要发布的应用事件，不能为 null
     * @throws NullPointerException 如果 event 为 null
     */
    void publishApplicationEvent(ApplicationEvent event);
}
