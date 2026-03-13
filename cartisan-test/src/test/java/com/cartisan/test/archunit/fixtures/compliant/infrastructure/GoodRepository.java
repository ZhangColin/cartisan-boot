package com.cartisan.test.archunit.fixtures.compliant.infrastructure;

import com.cartisan.test.archunit.fixtures.compliant.domain.GoodEntity;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

/**
 * 合规的仓储示例
 * - 使用 @Repository 注解
 * - 以 Repository 结尾
 */
@Repository
public class GoodRepository {

    public GoodEntity save(GoodEntity entity) {
        // 模拟持久化
        return entity;
    }
}
