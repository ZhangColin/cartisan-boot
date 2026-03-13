package com.cartisan.test.archunit.fixtures.compliant.application;

import com.cartisan.test.archunit.fixtures.compliant.domain.GoodEntity;
import com.cartisan.test.archunit.fixtures.compliant.infrastructure.GoodRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

/**
 * 合规的应用服务示例
 * - 使用 @Service 注解
 * - 位于 application 包
 * - 以 AppService 结尾
 * - 无 JPA 直接访问
 */
@Service
public class GoodAppService {

    private final GoodRepository repository;

    public GoodAppService(GoodRepository repository) {
        this.repository = repository;
    }

    public GoodEntity create(BigDecimal price) {
        return repository.save(new GoodEntity(price, java.time.LocalDateTime.now()));
    }
}
