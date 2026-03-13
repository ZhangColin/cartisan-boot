package com.cartisan.test.archunit.fixtures.compliant.domain;

import com.cartisan.core.stereotype.DomainService;
import java.math.BigDecimal;

/**
 * 合规的领域服务示例
 * - 使用 @DomainService 注解
 * - 以 Service 结尾
 * - 无 Spring 依赖
 */
@DomainService
public class GoodDomainService {

    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(0.01));
    }
}
