package com.cartisan.test.archunit.fixtures.compliant.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合规的领域实体示例
 * - 无 Spring 依赖
 * - 使用 java.time
 * - 金额字段用 BigDecimal
 */
public class GoodEntity {

    private final BigDecimal price;
    private final LocalDateTime createdAt;

    public GoodEntity(BigDecimal price, LocalDateTime createdAt) {
        this.price = price;
        this.createdAt = createdAt;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
