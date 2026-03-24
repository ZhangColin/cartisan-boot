package com.cartisan.data.jpa.domain;

/**
 * 可软删除实体接口。
 *
 * <p>标记实体支持软删除功能。实现此接口的实体在被删除时，
 * {@link com.cartisan.data.jpa.repository.BaseRepository#delete(Object)} 会调用
 * {@link #markAsDeleted()} 方法将 {@code deleted} 标记设为 {@code true}，而不是物理删除。</p>
 *
 * <p>此接口与 {@link AbstractSoftDeletable} 抽象类配合使用：</p>
 * <ul>
 *   <li>实体可以直接继承 {@link AbstractSoftDeletable} 获得完整实现</li>
 *   <li>或者继承其他基类（如 {@link com.cartisan.core.domain.AbstractAggregateRoot}）并实现此接口</li>
 * </ul>
 *
 * @see AbstractSoftDeletable
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
