package com.cartisan.data.jpa.repository.impl;

import com.cartisan.core.domain.AggregateRoot;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

/**
 * 测试用聚合根。
 */
@Entity
public class TestAggregateRoot implements AggregateRoot<TestAggregateRoot, String> {

    @Id
    private final String id;

    public TestAggregateRoot() {
        this.id = UUID.randomUUID().toString();
    }

    public TestAggregateRoot(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
