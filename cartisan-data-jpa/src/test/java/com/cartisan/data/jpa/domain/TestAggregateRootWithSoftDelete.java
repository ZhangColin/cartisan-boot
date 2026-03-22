package com.cartisan.data.jpa.domain;

import com.cartisan.core.domain.AbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SQLRestriction;

/**
 * 测试用聚合根实体，同时支持软删除功能。
 *
 * <p>用于测试 BaseRepositoryImpl 的自动软删除功能。</p>
 *
 * <p>注意：此实体不继承 SoftDeletable，因为 Java 不支持多重继承。
 * 相反，它实现了相同的软删除模式，BaseRepositoryImpl 会通过 instanceof 检查来识别。</p>
 */
@Entity(name = "test_soft_deletable_aggregate")
@SQLRestriction("deleted = false")
public class TestAggregateRootWithSoftDelete extends AbstractAggregateRoot<TestAggregateRootWithSoftDelete> {

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

    public boolean isDeleted() {
        return deleted;
    }

    public void markAsDeleted() {
        this.deleted = true;
    }
}
