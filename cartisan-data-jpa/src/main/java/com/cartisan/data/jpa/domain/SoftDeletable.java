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
 *   <li>或者继承其他基类（如 {@link com.cartisan.core.domain.AggregateRoot}）并实现此接口</li>
 * </ul>
 *
 * <h3>读过滤（自动）</h3>
 * <p>实现此接口的实体，其所有走 Hibernate SQL 生成的读路径（{@code findById}/{@code findAll}/
 * Specification/派生查询/JPQL/{@code count} 等）自动排除 {@code deleted = true} 的记录——由
 * {@link com.cartisan.data.jpa.hibernate.SoftDeletableRestrictionContributor} 在元模型构建期
 * 注册等价 {@code @SQLRestriction("deleted = false")} 的过滤。{@code findById} 对已删记录返回空，
 * 原生 SQL 与批量 DML 不受过滤（详见 {@link AuditableSoftDeletable}）。</p>
 *
 * <h3>实现契约：必须映射 {@code deleted} 列</h3>
 * <p>实现此接口的实体须将软删标记映射为持久化列 {@code deleted}
 * （{@link AuditableSoftDeletable} 已遵循此约定；自定义实现须保持一致）。该约定由
 * {@link com.cartisan.data.jpa.hibernate.SoftDeletableRestrictionContributor} 在元模型构建期校验——
 * 缺失 {@code deleted} 持久化列时直接抛 {@code MappingException}，应用<b>启动期</b>即失败
 * （错误消息指明违约的实体类），而非运行期才因自动过滤的 SQL 找不到列而抛异常。</p>
 *
 * <h3>鸭子类型（不实现本接口）的局限</h3>
 * <p>{@code BaseRepositoryImpl} 的写侧（{@code delete}/{@code deleteById}/{@code deleteAll}）对仅提供
 * {@code markAsDeleted()} 方法、但<b>未实现</b>本接口的实体，会通过反射调用其 {@code markAsDeleted()}
 * 完成软删除。但读过滤只在元模型期为<b>实现本接口</b>的实体注册——鸭子类型实体<b>不会</b>被自动加上读过滤，
 * 其 {@code findById}/{@code findAll} 等仍会返回 {@code deleted = true} 的记录。故若需要完整的
 * 「写软删 + 读过滤」语义，<b>请实现本接口</b>（或继承 {@link AuditableSoftDeletable}），
 * 而非仅凭 {@code markAsDeleted()} 方法名鸭子类型。</p>
 *
 * @see AuditableSoftDeletable
 * @since 0.1.0
 */
public interface SoftDeletable {

    /**
     * 标记为已删除（领域方法）。
     *
     * <p>供 Repository.delete() 调用，业务端通常不需要直接调用。</p>
     *
     * @since 0.1.0
     */
    void markAsDeleted();

    /**
     * 获取软删除标记值。
     *
     * @return deleted 字段值，true 表示已删除，false 表示未删除
     */
    boolean getDeleted();
}
