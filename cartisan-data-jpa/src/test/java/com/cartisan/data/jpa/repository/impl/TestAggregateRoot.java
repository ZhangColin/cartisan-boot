package com.cartisan.data.jpa.repository.impl;

import com.cartisan.core.domain.AbstractAggregateRoot;
import com.cartisan.core.domain.DomainEvent;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

/**
 * 测试用聚合根。
 */
@Entity
public class TestAggregateRoot extends AbstractAggregateRoot<TestAggregateRoot> {

    @Id
    private final String id;

    public TestAggregateRoot() {
        this.id = UUID.randomUUID().toString();
    }

    public String id() {
        return id;
    }

    /**
     * 注册测试事件。
     */
    public void registerTestEvent(String message) {
        registerEvent(new TestDomainEvent(id, message));
    }

    @Override
    public Object getId() {
        return id;
    }
}
