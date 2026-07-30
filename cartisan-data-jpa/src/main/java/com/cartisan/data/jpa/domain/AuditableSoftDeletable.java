package com.cartisan.data.jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * 可审计且可软删除实体基类。
 *
 * <p>继承 {@link Auditable}，增加软删除能力：</p>
 * <ul>
 *   <li>{@code deleted} 字段标记是否已删除</li>
 *   <li>调用 {@code repository.delete(entity)} 自动软删除（无需手动设置 {@code deleted}）</li>
 * </ul>
 *
 * <h3>软删除读过滤（自动）</h3>
 * <p>所有走 Hibernate SQL 生成的读路径都自动排除 {@code deleted = true} 的记录：
 * {@code findById}、{@code findAll}、Specification、派生查询（方法名查询）、显式 JPQL/HQL、
 * {@code count} 等。该过滤由 {@link com.cartisan.data.jpa.hibernate.SoftDeletableRestrictionContributor}
 * 在 Hibernate 元模型构建期自动注册——为实现 {@link SoftDeletable} 的具体实体统一注入等价于
 * {@code @SQLRestriction("deleted = false")} 的 where 片段，子类<b>无需</b>声明任何注解。</p>
 *
 * <p>{@code @SQLRestriction} 声明在 {@code @MappedSuperclass} 上不会被 Hibernate 继承到子类，
 * 因此本基类不声明该注解——读过滤完全由上述 Contributor 在具体实体上注册保证。</p>
 *
 * <p><b>{@code findById} 语义</b>：已软删记录对所有仓储读方法不可见，{@code findById(id)} 对已删记录
 * 返回 {@link java.util.Optional#empty()}。早期文档所述的「{@code findById} 逃生通道」已作废——不存在
 * 能绕过读过滤的仓储读入口。</p>
 *
 * <p><b>不受过滤的路径（查询已删数据的逃生通道）</b>：原生 SQL 查询（{@code nativeQuery = true}）与批量
 * UPDATE/DELETE 不走实体加载、不经 Hibernate SQL 生成，读过滤不作用，<b>会</b>读到已软删记录。需要查询
 * 已软删数据时走读侧：cartisan-data-query 模块的 jOOQ 查询，或显式带 {@code deleted} 条件的原生 SQL。</p>
 *
 * <h3>软删除行为（写侧）</h3>
 * <ul>
 *   <li>调用 {@code repository.delete(entity)} 自动将 {@code deleted} 设为 {@code true}</li>
 *   <li>调用 {@code repository.deleteById(id)} 自动将对应记录的 {@code deleted} 设为 {@code true}</li>
 *   <li>调用 {@code repository.deleteAll()} 批量将所有记录的 {@code deleted} 设为 {@code true}</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Entity
 * public class Product extends AuditableSoftDeletable {
 *     @Id private Long id;
 *     private String name;
 *     // ...
 * }
 *
 * // 使用
 * productRepository.delete(product);  // deleted = true
 * productRepository.findAll();        // 不包含已删除记录（Contributor 自动过滤）
 * }</pre>
 *
 * @since 0.1.0
 */
@Getter
@MappedSuperclass
public abstract class AuditableSoftDeletable extends Auditable implements SoftDeletable {

    /**
     * 软删除标记。
     *
     * <p>{@code false} = 未删除（默认），{@code true} = 已删除。</p>
     * <p>读过滤由 {@link com.cartisan.data.jpa.hibernate.SoftDeletableRestrictionContributor} 自动注册，查询时排除 {@code deleted = true} 的记录。</p>
     * -- GETTER --
     *  判断是否已软删除。
     *
     * @return true 表示已删除，false 表示未删除

     */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    /**
     * 标记为已删除（领域方法）。
     *
     * <p>供 Repository.delete() 调用，业务端通常不需要直接调用。</p>
     *
     * @since 0.1.0
     */
    @Override
    public void markAsDeleted() {
        this.deleted = true;
    }

    /**
     * 获取软删除标记值。
     *
     * @return deleted 字段值
     */
    @Override
    public boolean getDeleted() {
        return deleted;
    }

    /**
     * 判断是否已软删除（Java Bean 规范）。
     *
     * @return true 表示已删除，false 表示未删除
     */
    public boolean isDeleted() {
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
