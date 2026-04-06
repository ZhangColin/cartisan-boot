package com.cartisan.data.jpa.repository.impl.softdelete;

import com.cartisan.core.domain.AggregateRoot;
import com.cartisan.data.jpa.domain.SoftDeletable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SQLRestriction;

/**
 * 测试用软删除实体，实现 {@link SoftDeletable} 接口。
 */
@Entity(name = "soft_delete_test_entity")
@SQLRestriction("deleted = false")
public class SoftDeletableTestEntity implements AggregateRoot<SoftDeletableTestEntity>, SoftDeletable {

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

    @Override
    public void markAsDeleted() {
        this.deleted = true;
    }

    @Override
    public boolean getDeleted() {
        return deleted;
    }
}
