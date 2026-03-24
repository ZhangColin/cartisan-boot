package com.cartisan.data.jpa.repository.impl.softdelete;

import com.cartisan.core.domain.AbstractAggregateRoot;
import com.cartisan.core.domain.DomainEvent;
import com.cartisan.data.jpa.repository.impl.TestDomainEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SQLRestriction;

/**
 * 测试用软删除实体。
 */
@Entity(name = "soft_delete_test_entity")
@SQLRestriction("deleted = false")
public class SoftDeletableTestEntity extends AbstractAggregateRoot<SoftDeletableTestEntity> {

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

    /**
     * 注册测试事件（用于测试领域事件发布）。
     */
    public void registerTestEvent(String message) {
        registerEvent(new TestDomainEvent(String.valueOf(id), message));
    }
}
