package com.cartisan.data.jpa.startup.valid;

import com.cartisan.core.domain.AggregateRoot;
import com.cartisan.data.jpa.domain.SoftDeletable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * 遵循 {@link SoftDeletable} 契约的对照实体：实现接口且映射 {@code deleted} 列。
 *
 * <p>用于 fail-fast 测试的合法场景对照——在与失败用例相同的启动基座下应正常启动，
 * 证明校验不会对合规实体产生误报。</p>
 */
@Entity(name = "test_sd_valid_deleted_column")
public class ValidSoftDeletableEntity implements AggregateRoot<ValidSoftDeletableEntity, Long>, SoftDeletable {

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
