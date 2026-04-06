package com.cartisan.data.jpa.repository.impl.softdelete;

import com.cartisan.core.domain.AggregateRoot;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * 测试用常规实体，不支持软删除。
 */
@Entity(name = "regular_test_entity")
public class RegularTestEntity implements AggregateRoot<RegularTestEntity> {

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
