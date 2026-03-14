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
 * 测试应用配置，用于 domain 包的审计功能测试。
 *
 * <p>作为 @DataJpaTest 的测试配置。</p>
 */
@Configuration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = TestAuditableEntity.class)
@EnableJpaRepositories(basePackageClasses = TestAuditableEntityRepository.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class DomainTestApplication {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new TestAuditorAware();
    }

    /**
     * 测试用 AuditorAware。
     */
    static class TestAuditorAware implements AuditorAware<String> {

        private String currentAuditor = "test-user";

        @Override
        public Optional<String> getCurrentAuditor() {
            return Optional.ofNullable(currentAuditor);
        }

        public void setCurrentAuditor(String currentAuditor) {
            this.currentAuditor = currentAuditor;
        }

        public void clearCurrentAuditor() {
            this.currentAuditor = null;
        }
    }
}
