package com.cartisan.data.jpa.specification;

import com.cartisan.core.domain.AggregateRoot;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 测试用产品实体。
 *
 * <p>用于测试 @Condition 注解和 ConditionSpecifications 的集成测试。</p>
 */
@Entity
public class TestProduct implements AggregateRoot<TestProduct, Long> {

    @Id
    private final Long id;

    private String name;

    private BigDecimal price;

    private String category;

    private Integer stock;

    private LocalDateTime createdAt;

    /**
     * JPA 默认构造函数。
     */
    protected TestProduct() {
        this.id = null;
    }

    /**
     * 创建测试产品。
     *
     * @param id       产品 ID
     * @param name     产品名称
     * @param price    价格
     * @param category 分类
     * @param stock    库存
     */
    public TestProduct(Long id, String name, BigDecimal price, String category, Integer stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stock = stock;
        this.createdAt = LocalDateTime.now();
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal price() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String category() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer stock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
