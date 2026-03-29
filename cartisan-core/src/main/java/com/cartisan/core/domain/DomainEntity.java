package com.cartisan.core.domain;

/**
 * 实体接口。
 *
 * <p>实体是 DDD 中的核心概念，表示具有唯一标识的领域对象。</p>
 *
 * <h2>实体 vs 值对象</h2>
 *
 * <table border="1">
 *   <caption>实体与值对象的核心区别</caption>
 *   <tr><th>实体 (DomainEntity)</th><th>值对象 (ValueObject)</th></tr>
 *   <tr><td>有唯一标识</td><td>没有标识，通过属性值比较</td></tr>
 *   <tr><td>标识相同即为同一对象</td><td>所有属性值相同即为同一对象</td></tr>
 *   <tr><td>状态可变</td><td>状态不可变</td></tr>
 *   <tr><td>生命周期长</td><td>生命周期依附于实体</td></tr>
 * </table>
 *
 * <h2>使用示例</h2>
 *
 * <pre>{@code
 * public class User implements DomainEntity<User, UserId> {
 *     private UserId id;
 *     private String name;
 *     private String email;
 *
 *     public User(UserId id, String name, String email) {
 *         this.id = Objects.requireNonNull(id, "User ID cannot be null");
 *         this.name = name;
 *         this.email = email;
 *     }
 *
 *     @Override
 *     public UserId getId() {
 *         return id;
 *     }
 *
 *     public void changeName(String newName) {
 *         this.name = Objects.requireNonNull(newName, "Name cannot be null");
 *     }
 *
 *     // sameIdentityAs 由接口提供默认实现
 *     // equals/hashCode 可选择性覆写
 * }
 * }</pre>
 *
 * <h2>sameIdentityAs vs equals</h2>
 *
 * <ul>
 *   <li>{@code sameIdentityAs()} - 仅比较标识符，由接口提供</li>
 *   <li>{@code equals()} - 可选择性实现，通常也仅比较标识符</li>
 * </ul>
 *
 * @param <T> 实体类型
 * @param <ID> 标识符类型
 * @see ValueObject
 * @see Identity
 * @since 0.1.0
 */
public interface DomainEntity<T, ID> {

    /**
     * 获取实体的唯一标识符。
     *
     * @return 实体的标识符，可能为 null（表示尚未分配 ID）
     */
    ID getId();

    /**
     * 判断当前实体是否与另一个实体具有相同的标识。
     *
     * <p>默认实现通过 {@code Objects.equals()} 比较标识符。
     * 两个实体的 ID 都为 null 时，视为相同标识。</p>
     *
     * @param other 另一个实体，可能为 null
     * @return 如果标识相同则返回 {@code true}，否则返回 {@code false}
     */
    @SuppressWarnings("unchecked")
    default boolean sameIdentityAs(T other) {
        if (other == null) {
            return false;
        }
        // 由于 Java 类型擦除，这里需要进行运行时类型检查和强制转换
        if (this.getClass() != other.getClass()) {
            return false;
        }
        ID thisId = this.getId();
        ID otherId = ((DomainEntity<T, ID>) other).getId();
        return java.util.Objects.equals(thisId, otherId);
    }
}
