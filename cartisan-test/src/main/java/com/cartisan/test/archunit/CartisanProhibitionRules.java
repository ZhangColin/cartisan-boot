package com.cartisan.test.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * 禁止规则 — 阻止反模式和危险实践
 *
 * <p>包含以下规则：</p>
 * <ul>
 *   <li>禁止字段注入（强制构造函数注入）</li>
 *   <li>禁止使用 java.util.Date（强制 java.time）</li>
 *   <li>禁止金额字段使用 Double/Float（强制 BigDecimal）</li>
 * </ul>
 */
public class CartisanProhibitionRules {

    /**
     * 禁止 @Autowired 字段注入
     *
     * <p>字段注入隐藏依赖关系，不利于测试和重构。</p>
     * <p>应使用构造函数注入。</p>
     */
    @ArchTest
    static final ArchRule noFieldInjection =
        noFields()
            .should()
            .beAnnotatedWith(Autowired.class)
            .because("Use constructor injection instead of field injection");

    /**
     * 禁止使用 java.util.Date
     *
     * <p>java.util.Date 是可变的、线程不安全的遗留 API。</p>
     * <p>应使用 java.time API（LocalDateTime, Instant 等）。</p>
     */
    @ArchTest
    static final ArchRule noJavaUtilDate =
        noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.util.Date")
            .because("Use java.time API instead of java.util.Date");

    /**
     * 禁止金额字段使用浮点数
     *
     * <p>金额相关的字段（按名称匹配）不能使用 Double 或 Float。</p>
     * <p>应使用 BigDecimal 保证精度。</p>
     *
     * <p>匹配模式（不区分大小写）：</p>
     * <pre>price, amount, fee, cost, balance, money, payment, refund, commission</pre>
     */
    @ArchTest
    static final ArchRule noFloatingPointForMoney =
        noFields()
            .that()
            .haveNameMatching(".*(?i)(price|amount|fee|cost|balance|money|payment|refund|commission).*")
            .should()
            .haveRawType(Double.class)
            .orShould()
            .haveRawType(Float.class)
            .because("Use BigDecimal for monetary fields to avoid precision loss");
}
