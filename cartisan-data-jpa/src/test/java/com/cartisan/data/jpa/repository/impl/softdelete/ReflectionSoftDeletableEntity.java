package com.cartisan.data.jpa.repository.impl.softdelete;

import com.cartisan.core.domain.AggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SQLRestriction;

/**
 * 测试用实体，通过 markAsDeleted() 方法支持软删除（不实现 SoftDeletable 接口）。
 * 用于测试 BaseRepositoryImpl 的反射调用分支。
 */
@Entity(name = "reflection_soft_deletable_entity")
@SQLRestriction("deleted = false")
public class ReflectionSoftDeletableEntity implements AggregateRoot<ReflectionSoftDeletableEntity, Long> {

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
