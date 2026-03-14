package com.cartisan.data.jpa.domain;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 测试配置，不包含 AuditorAware Bean，用于验证条件装配。
 *
 * <p>此配置专门用于测试 AC5：当容器中不存在 AuditorAware Bean 时，
 * JPA Auditing 仍会填充时间字段，但 by 字段保持 null。</p>
 */
@Configuration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = TestAuditableEntity.class)
@EnableJpaRepositories(basePackageClasses = TestAuditableEntityRepository.class)
// 注意：不定义 @EnableJpaAuditing，也不提供 AuditorAware Bean
public class NoAuditorTestConfiguration {
    // 空配置，不提供任何 AuditorAware Bean
}
