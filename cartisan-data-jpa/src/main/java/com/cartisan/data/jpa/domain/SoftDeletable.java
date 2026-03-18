package com.cartisan.data.jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.SQLRestriction;

/**
 * 可软删除实体基类。
 *
 * <p>继承 {@link Auditable}，增加软删除能力：</p>
 * <ul>
 *   <li>{@code deleted} 字段标记是否已删除</li>
 *   <li>{@code @SQLRestriction} 在查询时自动过滤 {@code deleted = true} 的记录</li>
 * </ul>
 *
 * <h3>软删除行为</h3>
 * <ul>
 *   <li>调用 {@code repository.delete(entity)} 会将 {@code deleted} 设为 {@code true}</li>
 *   <li>调用 {@code repository.deleteById(id)} 会将对应记录的 {@code deleted} 设为 {@code true}</li>
 *   <li>所有查询（如 {@code findAll()}）自动排除 {@code deleted = true} 的记录</li>
 * </ul>
 *
 * <h3>⚠️ JPQL 查询限制</h3>
 * <p>
 * {@code @SQLRestriction} 仅对 Hibernate 自动生成的 SQL 查询生效。
 * 使用 {@code @Query} 注解的 JPQL 查询时，需要手动添加 {@code deleted = false} 条件：
 * </p>
 * <pre>{@code
 * // 正确：JPQL 查询手动添加软删除条件
 * @Query("SELECT e FROM Product e WHERE e.deleted = false AND e.name = :name")
 * List<Product> findActiveByName(@Param("name") String name);
 *
 * // 错误：JPQL 查询缺少软删除条件，会返回已删除记录
 * @Query("SELECT e FROM Product e WHERE e.name = :name")
 * List<Product> findByName(@Param("name") String name);
 * }</pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Entity
 * public class Product extends SoftDeletable {
 *     @Id private Long id;
 *     private String name;
 *     // ...
 * }
 *
 * // 使用
 * productRepository.delete(product);  // deleted = true
 * productRepository.findAll();        // 不包含已删除记录
 * }</pre>
 *
 * @since 0.2.0
 */
@MappedSuperclass
@SQLRestriction("deleted = false")
public abstract class SoftDeletable extends Auditable {

    /**
     * 软删除标记。
     *
     * <p>{@code false} = 未删除（默认），{@code true} = 已删除。</p>
     * <p>注意：@SQLRestriction 会在查询时自动过滤 {@code deleted = true} 的记录。</p>
     */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    /**
     * 判断是否已软删除。
     *
     * @return true 表示已删除，false 表示未删除
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * 获取软删除标记值。
     *
     * @return deleted 字段值
     */
    public boolean getDeleted() {
        return deleted;
    }

    /**
     * 设置软删除标记（仅供子类调用）。
     *
     * <p>通常不需要手动调用，通过 {@code repository.delete(entity)} 触发。</p>
     *
     * @param deleted 删除标记
     */
    protected void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
