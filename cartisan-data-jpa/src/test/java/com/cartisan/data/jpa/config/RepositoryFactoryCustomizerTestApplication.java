package com.cartisan.data.jpa.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Repository 工厂自动配置测试应用。
 *
 * <p>作为 @DataJpaTest 的 @SpringBootConfiguration。</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = RepositoryFactoryCustomizerTestEntity.class)
@EnableJpaRepositories(basePackageClasses = RepositoryFactoryCustomizerTestRepository.class)
public class RepositoryFactoryCustomizerTestApplication {
}
