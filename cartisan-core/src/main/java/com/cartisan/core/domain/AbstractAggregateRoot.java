package com.cartisan.core.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 聚合根抽象基类。
 *
 * <p>提供聚合根的通用功能，主要是领域事件的管理。</p>
 *
 * <h3>事件管理</h3>
 *
 * <ul>
 *   <li>{@code registerEvent()} - 注册领域事件</li>
 *   <li>{@code getDomainEvents()} - 获取已注册的事件列表（不可修改）</li>
 *   <li>{@code clearDomainEvents()} - 清空已注册的事件</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * public class Order extends AbstractAggregateRoot<Order> {
 *     private final OrderId id;
 *     private OrderStatus status;
 *
 *     public Order(OrderId id) {
 *         this.id = Objects.requireNonNull(id, "Order ID cannot be null");
 *         this.status = OrderStatus.PENDING;
 *     }
 *
 *     @Override
 *     public OrderId getId() {
 *         return id;
 *     }
 *
 *     public void confirm() {
 *         if (status != OrderStatus.PENDING) {
 *             throw new IllegalStateException("Only pending orders can be confirmed");
 *         }
 *         this.status = OrderStatus.CONFIRMED;
 *         registerEvent(new OrderConfirmedEvent(id.value()));
 *     }
 *
 *     public void ship() {
 *         if (status != OrderStatus.CONFIRMED) {
 *             throw new IllegalStateException("Only confirmed orders can be shipped");
 *         }
 *         this.status = OrderStatus.SHIPPED;
 *         registerEvent(new OrderShippedEvent(id.value()));
 *     }
 * }
 * }</pre>
 *
 * @param <T> 聚合根类型，用于支持链式调用
 * @see AggregateRoot
 * @see DomainEvent
 * @since 0.1.0
 */
public abstract class AbstractAggregateRoot<T> implements AggregateRoot<T> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 注册领域事件。
     *
     * @param event 要注册的事件，不能为 null
     * @throws NullPointerException 如果 event 为 null
     */
    protected void registerEvent(DomainEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        domainEvents.add(event);
    }

    /**
     * 获取已注册的领域事件列表。
     *
     * <p>返回的列表是不可修改的，任何修改操作都会抛出
     * {@code UnsupportedOperationException}。</p>
     *
     * @return 不可修改的事件列表
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * 清空已注册的领域事件。
     *
     * <p>通常在事件被持久化或发布后调用，以避免重复处理。</p>
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    /**
     * 获取聚合根的标识符。
     *
     * <p>子类必须实现此方法以返回聚合根的唯一标识。</p>
     *
     * @return 聚合根的标识符
     */
    public abstract Object getId();
}
