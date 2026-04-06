package com.cartisan.data.jpa.repository.impl;

import com.cartisan.core.domain.Identity;

import java.util.Objects;

/**
 * TestAggregateRoot 标识符。
 */
public class TestAggregateRootId implements Identity {
    private final String value;

    public TestAggregateRootId(String value) {
        this.value = Objects.requireNonNull(value, "ID cannot be null");
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestAggregateRootId that = (TestAggregateRootId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "TestAggregateRootId{" + "value='" + value + '\'' + '}';
    }
}
