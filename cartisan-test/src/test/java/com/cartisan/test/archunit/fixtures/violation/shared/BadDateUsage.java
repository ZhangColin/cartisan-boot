package com.cartisan.test.archunit.fixtures.violation.shared;

import java.util.Date;  // ❌ 导入被禁止的类

/**
 * 违规：使用 java.util.Date
 * 违反规则：noJavaUtilDate
 */
public class BadDateUsage {

    private Date date;  // ❌ 应该使用 java.time

    public Date getDate() {
        return date;
    }
}
