package com.cartisan.data.jpa.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SQLRestriction;

/**
 * 测试用实体，用于软删除功能集成测试。
 */
@Entity(name = "test_soft_deletable_entity")
@SQLRestriction("deleted = false")
public class TestSoftDeletableEntity extends SoftDeletable {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
