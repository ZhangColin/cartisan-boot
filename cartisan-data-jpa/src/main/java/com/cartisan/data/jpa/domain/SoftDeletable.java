package com.cartisan.data.jpa.domain;

/**
 * 可软删除实体接口。
 *
 * <p>标记实体支持软删除功能。实现此接口的实体在被删除时，
 * {@link com.cartisan.data.jpa.repository.BaseRepository#delete(Object)} 会调用
 * {@link #markAsDeleted()} 方法将 {@code deleted} 标记设为 {@code true}，而不是物理删除。</p>
 *
 * <p>此接口与 {@link AuditableSoftDeletable} 抽象类配合使用：</p>
 * <ul>
 *   <li>实体可以直接继承 {@link AuditableSoftDeletable} 获得完整实现</li>
 *   <li>或者继承其他基类（如 {@link com.cartisan.core.domain.AbstractAggregateRoot}）并实现此接口</li>
 * </ul>
 *
 * <h3>读过滤（自动）</h3>
 * <p>实现此接口的实体，其所有读路径（{@code findById}/{@code findAll}/Specification/派生查询/JPQL/
 * {@code count} 等）自动排除 {@code deleted = true} 的记录——由
 * {@code com.cartisan.data.jpa.hibernate.SoftDeleteRestrictionContributor} 在元模型构建期
 * 注册等价 {@code @SQLRestriction("deleted = false")} 的过滤。</p>
 *
 * <p><b>约定</b>：实现此接口的实体须将软删标记映射为列 {@code deleted}
 * （{@link AuditableSoftDeletable} 已遵循此约定；自定义实现须保持一致，否则自动过滤的 SQL 会找不到列）。</p>
 *
 * @see AuditableSoftDeletable
 * @since 0.3.0
 */
public interface SoftDeletable {

    /**
     * 标记为已删除（领域方法）。
     *
     * <p>供 Repository.delete() 调用，业务端通常不需要直接调用。</p>
     *
     * @since 0.3.0
     */
    void markAsDeleted();

    /**
     * 获取软删除标记值。
     *
     * @return deleted 字段值，true 表示已删除，false 表示未删除
     */
    boolean getDeleted();
}
