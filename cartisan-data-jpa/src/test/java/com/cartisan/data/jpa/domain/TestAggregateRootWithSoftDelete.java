package com.cartisan.data.jpa.domain;

import com.cartisan.core.domain.AggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SQLRestriction;

/**
 * 测试用聚合根实体，同时支持软删除功能。
 *
 * <p>用于测试 BaseRepositoryImpl 的自动软删除功能。</p>
 */
@Entity(name = "test_soft_deletable_aggregate")
@SQLRestriction("deleted = false")
public class TestAggregateRootWithSoftDelete implements AggregateRoot<TestAggregateRootWithSoftDelete, Long> {

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
