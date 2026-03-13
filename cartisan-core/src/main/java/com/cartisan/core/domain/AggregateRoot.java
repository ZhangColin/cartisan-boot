package com.cartisan.core.domain;

/**
 * 聚合根标记接口。
 *
 * <p>聚合根是 DDD 中的核心概念，表示一组相关对象的访问入口点。</p>
 *
 * <p>聚合根具有以下特征：</p>
 * <ul>
 *   <li>拥有全局唯一标识</li>
 *   <li>负责维护其内部对象的不变性约束</li>
 *   <li>外部对象只能通过聚合根来访问其内部对象</li>
 *   <li>可以发布领域事件</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 *
 * <p>本接口仅作为标记接口使用，实际功能由 {@link AbstractAggregateRoot} 提供。</p>
 *
 * <h3>示例</h3>
 *
 * <pre>{@code
 * // 方式1: 继承 AbstractAggregateRoot（推荐）
 * public class Order extends AbstractAggregateRoot<Order> {
 *     private final OrderId id;
 *
 *     public Order(OrderId id) {
 *         this.id = id;
 *     }
 *
 *     public void ship() {
 *         // 业务逻辑...
 *         registerEvent(new OrderShippedEvent(id.value()));
 *     }
 *
 *     @Override
 *     public OrderId getId() {
 *         return id;
 *     }
 * }
 *
 * // 方式2: 仅实现标记接口（自定义事件管理）
 * public class Product implements AggregateRoot {
 *     private final ProductId id;
 *     private final List<DomainEvent> events = new ArrayList<>();
 *
 *     public Product(ProductId id) {
 *         this.id = id;
 *     }
 *
 *     public ProductId getId() {
 *         return id;
 *     }
 *
 *     // 自定义事件管理逻辑...
 * }
 * }</pre>
 *
 * @param <T> 聚合根类型，用于支持链式调用
 * @see AbstractAggregateRoot
 * @see DomainEvent
 * @since 0.1.0
 */
public interface AggregateRoot<T> {
}
