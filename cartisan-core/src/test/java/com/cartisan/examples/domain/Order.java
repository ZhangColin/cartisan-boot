package com.cartisan.examples.domain;

import com.cartisan.core.domain.AggregateRoot;
import com.cartisan.core.domain.DomainEntity;
import com.cartisan.core.domain.Identity;
import com.cartisan.core.domain.ValueObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 订单聚合根 - 完整的领域模型示例。
 *
 * <p>展示如何使用 cartisan-core 提供的 DDD 基础类型。</p>
 */
public class Order implements AggregateRoot<Order> {

    private final OrderId id;
    private final List<OrderItem> items;
    private ShippingAddress shippingAddress;
    private OrderStatus status;

    private Order(OrderId id, List<OrderItem> items, ShippingAddress shippingAddress, OrderStatus status) {
        this.id = Objects.requireNonNull(id, "Order ID cannot be null");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "Items cannot be null"));
        this.shippingAddress = shippingAddress;
        this.status = status != null ? status : OrderStatus.PENDING;
    }

    /**
     * 创建新订单。
     */
    public static Order create(List<OrderItem> items, ShippingAddress shippingAddress) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        return new Order(OrderId.generate(), items, shippingAddress, OrderStatus.PENDING);
    }

    /**
     * 从已有状态重建订单（用于持久化后重建）。
     */
    public static Order reconstruct(OrderId id, List<OrderItem> items, ShippingAddress shippingAddress, OrderStatus status) {
        return new Order(id, items, shippingAddress, status);
    }

    /**
     * 确认订单。
     */
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * 发货。
     */
    public void ship() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be shipped");
        }
        if (shippingAddress == null) {
            throw new IllegalStateException("Cannot ship order without shipping address");
        }
        this.status = OrderStatus.SHIPPED;
    }

    /**
     * 更新收货地址。
     */
    public void updateShippingAddress(ShippingAddress newAddress) {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot update shipping address for shipped or delivered orders");
        }
        this.shippingAddress = Objects.requireNonNull(newAddress, "Shipping address cannot be null");
    }

    /**
     * 添加订单项。
     */
    public void addItem(OrderItem item) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot add items to non-pending orders");
        }
        items.add(Objects.requireNonNull(item, "Item cannot be null"));
    }

    /**
     * 计算订单总金额。
     */
    public BigDecimal calculateTotal() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public OrderId id() {
        return id;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public OrderStatus getStatus() {
        return status;
    }

    // ============================================================
    // 领域模型组件定义
    // ============================================================

    /**
     * 订单ID - 值对象。
     */
    public record OrderId(String value) implements Identity<String> {
        public static OrderId of(String value) {
            return new OrderId(value);
        }

        public static OrderId generate() {
            return new OrderId(java.util.UUID.randomUUID().toString());
        }

        public OrderId {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Order ID cannot be blank");
            }
        }
    }

    /**
     * 订单状态 - 枚举值对象。
     */
    public enum OrderStatus {
        PENDING,       // 待确认
        CONFIRMED,     // 已确认
        SHIPPED,       // 已发货
        DELIVERED,     // 已送达
        CANCELLED      // 已取消
    }
}

/**
 * 订单项 - 实体。
 */
class OrderItem implements DomainEntity<OrderItem, OrderItem.OrderItemId> {

    private final OrderItemId id;
    private final String productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private int quantity;

    public OrderItem(OrderItemId id, String productId, String productName, BigDecimal unitPrice, int quantity) {
        this.id = Objects.requireNonNull(id, "Order item ID cannot be null");
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null");
        this.productName = Objects.requireNonNull(productName, "Product name cannot be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "Unit price cannot be null");
        setQuantity(quantity);
    }

    public static OrderItem create(String productId, String productName, BigDecimal unitPrice, int quantity) {
        return new OrderItem(
                OrderItemId.generate(),
                productId,
                productName,
                unitPrice,
                quantity
        );
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = quantity;
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public OrderItemId getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    /**
     * 订单项ID - 值对象。
     */
    public record OrderItemId(String value) implements Identity<String> {
        public static OrderItemId generate() {
            return new OrderItemId(java.util.UUID.randomUUID().toString());
        }

        public OrderItemId {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Order item ID cannot be blank");
            }
        }
    }
}

/**
 * 收货地址 - 值对象。
 */
record ShippingAddress(
        String recipient,
        String phone,
        String province,
        String city,
        String district,
        String detailAddress,
        String postalCode
) implements ValueObject<ShippingAddress> {

    public ShippingAddress {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("Recipient cannot be blank");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone cannot be blank");
        }
        if (province == null || province.isBlank()) {
            throw new IllegalArgumentException("Province cannot be blank");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be blank");
        }
        if (detailAddress == null || detailAddress.isBlank()) {
            throw new IllegalArgumentException("Detail address cannot be blank");
        }
    }

    public String fullAddress() {
        return String.format("%s%s%s%s", province, city, district != null ? district : "", detailAddress);
    }
}
