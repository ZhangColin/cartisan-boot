package com.cartisan.core.domain;

/**
 * 值对象接口。
 *
 * <p>值对象是 DDD 中的核心概念，表示通过其属性值而非标识来定义的领域对象。</p>
 *
 * <h2>值对象 vs 实体</h2>
 *
 * <table border="1">
 *   <caption>值对象与实体的核心区别</caption>
 *   <tr><th>值对象 (ValueObject)</th><th>实体 (Entity)</th></tr>
 *   <tr><td>没有标识</td><td>有唯一标识</td></tr>
 *   <tr><td>通过属性值比较相等性</td><td>通过标识比较相等性</td></tr>
 *   <tr><td>状态不可变</td><td>状态可变</td></tr>
 *   <tr><td>生命周期依附于实体</td><td>有独立生命周期</td></tr>
 * </table>
 *
 * <h2>使用 Record 实现</h2>
 *
 * <p>Java Record 是实现值对象的理想方式，编译器自动生成正确的 equals/hashCode：</p>
 *
 * <pre>{@code
 * // 地址值对象
 * public record Address(
 *     String street,
 *     String city,
 *     String postalCode
 * ) implements ValueObject<Address> {
 *     public Address {
 *         // 紧凑构造函数进行验证
 *         if (street == null || street.isBlank()) {
 *             throw new IllegalArgumentException("Street cannot be blank");
 *         }
 *         if (city == null || city.isBlank()) {
 *             throw new IllegalArgumentException("City cannot be blank");
 *         }
 *         if (postalCode == null || postalCode.isBlank()) {
 *             throw new IllegalArgumentException("Postal code cannot be blank");
 *         }
 *     }
 * }
 *
 * // 金额值对象（避免使用 double/float）
 * public record Money(
 *     BigDecimal amount,
 *     Currency currency
 * ) implements ValueObject<Money> {
 *     public Money {
 *         amount = amount != null ? amount : BigDecimal.ZERO;
 *         currency = Objects.requireNonNull(currency, "Currency cannot be null");
 *     }
 *
 *     public Money add(Money other) {
 *         if (!currency.equals(other.currency)) {
 *             throw new IllegalArgumentException("Cannot add different currencies");
 *         }
 *         return new Money(amount.add(other.amount), currency);
 *     }
 * }
 * }</pre>
 *
 * <h2>sameValueAs vs equals</h2>
 *
 * <ul>
 *   <li>{@code sameValueAs()} - 显式的值比较语义，由接口提供默认实现（委托给 equals）</li>
 *   <li>{@code equals()} - Java 标准相等性比较，Record 自动生成正确实现</li>
 * </ul>
 *
 * <h2>自定义比较逻辑</h2>
 *
 * <p>如果需要自定义值比较逻辑（例如忽略大小写），可以覆写 sameValueAs：</p>
 *
 * <pre>{@code
 * public class Email implements ValueObject<Email> {
 *     private final String value;
 *
 *     public Email(String value) {
 *         this.value = Objects.requireNonNull(value, "Email cannot be null");
 *     }
 *
 *     @Override
 *     public boolean sameValueAs(Email other) {
 *         if (other == null) {
 *             return false;
 *         }
 *         // 忽略大小写比较
 *         return value.equalsIgnoreCase(other.value);
 *     }
 *
 *     @Override
 *     public boolean equals(Object obj) {
 *         // equals 保持区分大小写
 *         if (this == obj) return true;
 *         if (!(obj instanceof Email)) return false;
 *         return value.equals(((Email) obj).value);
 *     }
 * }
 * }</pre>
 *
 * @param <T> 值对象类型
 * @see Entity
 * @since 0.1.0
 */
public interface ValueObject<T> {

    /**
     * 判断当前值对象是否与另一个值对象具有相同的值。
     *
     * <p>默认实现委托给 {@link Object#equals(Object)} 方法。
     * 对于 Record 实现的值对象，这提供了零成本的值比较。</p>
     *
     * <p>如果需要自定义比较逻辑（例如忽略大小写、容忍小数误差等），
     * 可以覆写此方法。</p>
     *
     * @param other 另一个值对象，可能为 null
     * @return 如果值相同则返回 {@code true}，否则返回 {@code false}
     */
    default boolean sameValueAs(T other) {
        if (other == null) {
            return false;
        }
        return this.equals(other);
    }
}
