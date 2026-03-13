package com.cartisan.test.archunit.fixtures.compliant.shared;

import java.math.BigDecimal;

/**
 * 合规的金额值对象示例
 * - 金额字段使用 BigDecimal
 */
public class GoodMoney {

    private final BigDecimal amount;
    private final String currency;

    public GoodMoney(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
