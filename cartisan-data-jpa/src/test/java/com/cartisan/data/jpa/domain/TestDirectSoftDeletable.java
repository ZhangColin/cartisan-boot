package com.cartisan.data.jpa.domain;

import com.cartisan.core.domain.AggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * 直接实现 {@link SoftDeletable}（不经 {@link AuditableSoftDeletable}）的测试实体。
 *
 * <p>自身不声明任何注解，{@code deleted} 字段按框架约定映射为列 {@code deleted}。
 * 用于验证 {@code SoftDeleteRestrictionContributor} 对直接接口实现的实体同样注册读过滤。</p>
 */
@Entity(name = "test_direct_soft_deletable")
public class TestDirectSoftDeletable implements AggregateRoot<TestDirectSoftDeletable, Long>, SoftDeletable {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void markAsDeleted() {
        this.deleted = true;
    }

    @Override
    public boolean getDeleted() {
        return deleted;
    }
}
