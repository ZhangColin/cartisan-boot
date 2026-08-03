package com.cartisan.data.jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * 可审计且可软删除实体基类——<b>按需 opt-in</b>，非默认推荐。
 *
 * <p><b>聚合根默认建议继承 {@link Auditable}</b>（纯审计，不改变读写语义）。
 * 仅当业务明确需要「删除但保留/可恢复」时，才继承本类——软删是领域决策，不是技术默认值。</p>
 *
 * <p>继承 {@link Auditable}，增加软删除能力：</p>
 * <ul>
 *   <li>{@code deleted} 字段标记是否已删除</li>
 *   <li>调用 {@code repository.delete(entity)} 自动软删除（无需手动设置 {@code deleted}）</li>
 * </ul>
 *
 * <h3>能力边界（必读）</h3>
 *
 * <h4>✅ 自动过滤覆盖面</h4>
 * <p>所有走 Hibernate SQL 生成的读路径都自动排除 {@code deleted = true} 的记录：
 * {@code findById}、{@code findAll}、Specification、派生查询、显式 JPQL/HQL、
 * {@code count} 等。该过滤由 {@link com.cartisan.data.jpa.hibernate.SoftDeletableRestrictionContributor}
 * 在 Hibernate 元模型构建期自动注册。</p>
 *
 * <h4>❌ 不覆盖：跨实体查询</h4>
 * <p>查询实体 B 但需按关联实体 A 的 deleted 过滤时（典型：关联表、按 ID 引用），过滤<b>不会传播</b>。
 * 两个应对：</p>
 * <ul>
 *   <li>显式 join + {@code deleted = false} 条件</li>
 *   <li>（推荐）在聚合删除生命周期里清理关联——{@code markAsDeleted()} 中清理自有子关联，
 *       orphanRemoval 同事务物理删除，消灭残留本身而不是教每个读者容忍残留</li>
 * </ul>
 *
 * <h4>❌ 不覆盖：原生 SQL 与批量 DML</h4>
 * <p>原生 SQL 查询（{@code nativeQuery = true}）与批量 UPDATE/DELETE 不走实体加载，
 * 读过滤不作用，<b>会</b>读到已软删记录。需要查询已删数据时走读侧：cartisan-data-query 模块的
 * jOOQ 查询，或显式带 {@code deleted} 条件的原生 SQL。</p>
 *
 * <h4>⚠️ 软删不清理关联残留</h4>
 * <p>{@code repository.delete(entity)} 只置标志位，<b>不触碰任何关联行</b>。
 * 残留行对所有按关联计数/回显的查询可见（如角色已删但用户-角色关联行仍在，
 * 汇总 roleCodes/permissions 时仍会回显已删角色）。</p>
 *
 * <h4>⚠️ 唯一索引冲突</h4>
 * <p>软删记录仍占唯一约束（如 username 不可复用）。需复用时用 PG 部分唯一索引
 * {@code CREATE UNIQUE INDEX ... ON table (col) WHERE deleted = false}，DDL 自行声明。</p>
 *
 * <h3>软删除行为（写侧）</h3>
 * <ul>
 *   <li>调用 {@code repository.delete(entity)} 自动将 {@code deleted} 设为 {@code true}</li>
 *   <li>调用 {@code repository.deleteById(id)} 自动将对应记录的 {@code deleted} 设为 {@code true}</li>
 *   <li>调用 {@code repository.deleteAll()} 批量将所有记录的 {@code deleted} 设为 {@code true}</li>
 * </ul>
 *
 * <h3>何时不该用软删</h3>
 * <ul>
 *   <li><b>只是要留痕</b> → 审计字段（{@link Auditable}）或领域事件 / append-only 建模</li>
 *   <li><b>只是要停用</b> → 领域状态字段（ENABLED/DISABLED），而非 deleted 标志</li>
 *   <li><b>关联表实体</b> → 物理删除，避免唯一索引冲突</li>
 *   <li><b>配置型小对象</b> → 可低成本重建，物理删除</li>
 *   <li><b>日志/临时数据</b> → 量大且无需恢复，物理删除</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 按需 opt-in：仅当业务需要"删除但可恢复"时使用
 * @Entity
 * public class Order extends AuditableSoftDeletable {
 *     @Id private Long id;
 *     private String status;
 *     // 自动拥有：审计字段 + deleted + 软删读写行为
 * }
 *
 * orderRepository.delete(order);  // UPDATE SET deleted = true
 * orderRepository.findAll();      // 自动过滤 deleted = true
 * }</pre>
 *
 * @see Auditable
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
