package com.cartisan.data.jpa.repository.impl;

import com.cartisan.core.domain.DomainEvent;

/**
 * 测试用领域事件。
 */
public class TestDomainEvent extends DomainEvent {

    private final String message;

    public TestDomainEvent(String aggregateId, String message) {
        super(aggregateId);
        this.message = message;
    }

    public String message() {
        return message;
    }
}
