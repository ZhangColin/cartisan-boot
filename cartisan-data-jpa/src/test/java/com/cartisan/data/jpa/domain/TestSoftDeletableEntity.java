package com.cartisan.data.jpa.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * 测试用实体，用于软删除功能集成测试。
 *
 * <p><b>故意不自声明 {@code @SQLRestriction}</b>：本实体继承 {@link AuditableSoftDeletable}（实现
 * {@link SoftDeletable}），读过滤由框架 {@link com.cartisan.data.jpa.hibernate.SoftDeletableRestrictionContributor}
 * 在元模型期自动注册。本类的整套测试由此验证<b>框架机制</b>而非实体自声明——请勿补回注解。
 * （显式声明对照见 {@link TestAggregateRootWithSoftDelete}。）</p>
 */
@Entity(name = "test_soft_deletable_entity")
public class TestSoftDeletableEntity extends AuditableSoftDeletable {

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
