package com.cartisan.data.jpa.domain;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

/**
 * 测试应用配置，用于 cartisan-data-jpa 模块的所有 JPA 测试。
 *
 * <p>配置了：
 * <ul>
 *   <li>EntityScan：扫描所有测试用实体</li>
 *   <li>EnableJpaRepositories：扫描所有测试用 Repository</li>
 *   <li>EnableJpaAuditing：启用审计功能</li>
 *   <li>AuditorAware Bean：提供测试用审计人</li>
 * </ul>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.cartisan.data.jpa.domain")
@EnableJpaRepositories(basePackages = "com.cartisan.data.jpa.domain")
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaTestApplication {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return new TestAuditorAware();
    }

    /**
     * 测试用 AuditorAware。
     */
    static class TestAuditorAware implements AuditorAware<Long> {

        private Long currentAuditor = 1L;

        @Override
        public Optional<Long> getCurrentAuditor() {
            return Optional.ofNullable(currentAuditor);
        }

        public void setCurrentAuditor(Long currentAuditor) {
            this.currentAuditor = currentAuditor;
        }

        public void clearCurrentAuditor() {
            this.currentAuditor = null;
        }
    }
}
