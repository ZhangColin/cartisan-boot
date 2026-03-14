package com.cartisan.data.jpa.domain;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

/**
 * 测试配置，提供返回 null 的 AuditorAware Bean。
 *
 * <p>用于验证 AC6-4：当 AuditorAware 返回 null 时，by 字段保持 null。</p>
 */
@Configuration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = TestAuditableEntity.class)
@EnableJpaRepositories(basePackageClasses = TestAuditableEntityRepository.class)
@EnableJpaAuditing(auditorAwareRef = "nullAuditorAware")
public class NullAuditorTestConfiguration {

    @Bean
    public AuditorAware<String> nullAuditorAware() {
        return () -> Optional.empty();
    }
}
