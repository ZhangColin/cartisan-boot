package com.cartisan.core.domain;

/**
 * 标识符接口。
 *
 * <p>领域对象的唯一标识，用于类型安全的 ID 封装。</p>
 *
 * <h2>类型安全</h2>
 *
 * <p>使用本接口可以避免"基本类型偏执"（Primitive Obsession）反模式，
 * 为不同类型的 ID 提供编译时类型检查：</p>
 *
 * <pre>{@code
 * // ❌ 错误：容易混淆不同类型的 ID
 * void processOrder(String orderId, String userId) { ... }
 * processOrder(user.getId(), order.getId()); // 编译通过，但参数顺序错误！
 *
 * // ✅ 正确：类型安全
 * void processOrder(OrderId orderId, UserId userId) { ... }
 * processOrder(user.getId(), order.getId()); // 编译错误！类型不匹配
 * }</pre>
 *
 * <h2>使用 Record 实现</h2>
 *
 * <p>Java Record 是实现 Identity 的理想方式，零样板代码：</p>
 *
 * <pre>{@code
 * // String 类型的 ID
 * public record UserId(String value) implements Identity<String> {
 *     public UserId {
 *         if (value == null || value.isBlank()) {
 *             throw new IllegalArgumentException("User ID cannot be blank");
 *         }
 *     }
 * }
 *
 * // Long 类型的 ID
 * public record OrderId(Long value) implements Identity<Long> {
 *     public OrderId {
 *         if (value == null || value <= 0) {
 *             throw new IllegalArgumentException("Order ID must be positive");
 *         }
 *     }
 * }
 *
 * // UUID 类型的 ID
 * public record ProductId(UUID value) implements Identity<UUID> {
 *     public ProductId {
 *         if (value == null) {
 *             throw new IllegalArgumentException("Product ID cannot be null");
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>与 Entity 配合使用</h2>
 *
 * <pre>{@code
 * public class User implements DomainEntity<User, UserId> {
 *     private final UserId id;
 *     private String name;
 *
 *     public User(UserId id, String name) {
 *         this.id = Objects.requireNonNull(id, "User ID cannot be null");
 *         this.name = name;
 *     }
 *
 *     @Override
 *     public UserId getId() {
 *         return id;
 *     }
 * }
 * }</pre>
 *
 * @param <T> 标识符的值类型（如 String、Long、UUID 等）
 * @see DomainEntity
 * @since 0.1.0
 */
public interface Identity<T> {

    /**
     * 获取标识符的值。
     *
     * @return 标识符的值，可能为 null（由具体实现决定）
     */
    T value();
}
