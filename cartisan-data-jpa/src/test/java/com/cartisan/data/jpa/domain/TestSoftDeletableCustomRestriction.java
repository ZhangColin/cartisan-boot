package com.cartisan.data.jpa.domain;

import com.cartisan.core.domain.AggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SQLRestriction;

/**
 * 显式声明 {@code @SQLRestriction} 的 {@link SoftDeletable} 测试实体。
 *
 * <p>使用自定义过滤表达式（{@code name <> 'hidden'}，非 {@code deleted = false}），
 * 用于验证 {@code SoftDeleteRestrictionContributor} 在实体已显式声明 restriction 时
 * 不覆盖、不叠加。</p>
 */
@Entity(name = "test_sd_custom_restriction")
@SQLRestriction("name <> 'hidden'")
public class TestSoftDeletableCustomRestriction
        implements AggregateRoot<TestSoftDeletableCustomRestriction, Long>, SoftDeletable {

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
