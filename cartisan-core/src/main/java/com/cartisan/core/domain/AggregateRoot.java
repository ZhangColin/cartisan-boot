package com.cartisan.core.domain;

/**
 * 聚合根接口，继承自 {@link DomainEntity}。
 *
 * <p>聚合根是 DDD 中的核心概念，表示一组相关对象的访问入口点。
 * 聚合根本身也是一种实体，具有唯一标识，因此继承 {@link DomainEntity}。</p>
 *
 * <h2>聚合根特征</h2>
 * <ul>
 *   <li>拥有全局唯一标识（继承自 {@link DomainEntity#getId()}）</li>
 *   <li>负责维护其内部对象的不变性约束</li>
 *   <li>外部对象只能通过聚合根来访问其内部对象</li>
 *   <li>支持标识相等性比较（继承自 {@link DomainEntity#sameIdentityAs(Object)}）</li>
 * </ul>
 *
 * <h2>用途</h2>
 * <ul>
 *   <li>类型约束：只有聚合根才能有 {@code Repository}</li>
 *   <li>架构验证：通过 ArchUnit 验证聚合根的正确性</li>
 * </ul>
 *
 * @param <T> 聚合根类型
 * @param <ID> 标识符类型
 * @see DomainEntity
 * @since 0.1.0
 */
public interface AggregateRoot<T, ID> extends DomainEntity<T, ID> {
}
