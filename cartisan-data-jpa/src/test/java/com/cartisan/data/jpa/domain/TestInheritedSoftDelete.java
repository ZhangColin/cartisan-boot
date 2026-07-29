package com.cartisan.data.jpa.domain;

import com.cartisan.core.domain.AggregateRoot;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * 模拟下游应用真实用法：继承 {@link AuditableSoftDeletable}，自身不声明任何注解。
 *
 * <p>软删除读过滤由 {@code SoftDeleteRestrictionContributor} 在 Hibernate 元模型构建期
 * 自动注册（@SQLRestriction 声明在 @MappedSuperclass 上不会被 Hibernate 继承到子类）。</p>
 */
@Entity(name = "test_inherited_soft_delete")
public class TestInheritedSoftDelete extends AuditableSoftDeletable
        implements AggregateRoot<TestInheritedSoftDelete, Long> {

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
