package com.cartisan.test.archunit.fixtures.violation.shared;

/**
 * 违规：金额字段使用 Double
 * 违反规则：noFloatingPointForMoney
 */
public class BadMoneyWithDouble {

    private Double price;       // ❌ 金额字段应该用 BigDecimal
    private Double amount;      // ❌ 金额字段应该用 BigDecimal
    private Double fee;         // ❌ 金额字段应该用 BigDecimal
    private double balance;     // ❌ 金额字段应该用 BigDecimal

    public BadMoneyWithDouble(Double price, Double amount, Double fee, double balance) {
        this.price = price;
        this.amount = amount;
        this.fee = fee;
        this.balance = balance;
    }
}
