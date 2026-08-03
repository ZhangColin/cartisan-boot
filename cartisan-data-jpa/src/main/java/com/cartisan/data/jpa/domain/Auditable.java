package com.cartisan.data.jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * 可审计实体基类——聚合根的<b>默认推荐</b>基类。
 *
 * <p>实体继承此基类后，JPA Auditing 会自动填充审计字段：</p>
 * <ul>
 *   <li>{@code createdAt} — 首次保存时自动填充</li>
 *   <li>{@code updatedAt} — 每次保存时自动更新</li>
 *   <li>{@code createdBy} — 首次保存时从 {@code AuditorAware} 获取</li>
 *   <li>{@code updatedBy} — 每次保存时从 {@code AuditorAware} 获取</li>
 * </ul>
 *
 * <p>本基类<b>不改变任何读写语义</b>（无软删除、无读过滤），是聚合根最安全的首选。
 * 仅当业务明确需要「删除但保留/可恢复」时，才改用 {@link AuditableSoftDeletable}。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Entity
 * public class Product extends Auditable {
 *     @Id private Long id;
 *     private String name;
 *     // 自动拥有：createdAt, updatedAt, createdBy, updatedBy
 * }
 * }</pre>
 *
 * @see AuditableSoftDeletable
 * @since 0.1.0
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    /**
     * 创建时间，实体首次持久化时自动填充。
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 最后修改时间，每次保存时自动更新。
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 创建人ID，首次持久化时从 AuditorAware 获取。
     *
     * <p>若容器中不存在 AuditorAware Bean，此字段保持 null。</p>
     */
    @CreatedBy
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 最后修改人ID，每次保存时从 AuditorAware 获取。
     *
     * <p>若容器中不存在 AuditorAware Bean，此字段保持 null。</p>
     */
    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

}
