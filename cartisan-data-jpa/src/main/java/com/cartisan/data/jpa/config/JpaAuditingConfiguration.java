package com.cartisan.data.jpa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 自动配置。
 *
 * <p>仅在容器中存在 {@link AuditorAware} Bean 时启用 JPA Auditing。</p>
 *
 * <h3>条件装配</h3>
 * <ul>
 *   <li>若存在 {@code AuditorAware<?>} Bean：启用 auditing，注入该 AuditorAware</li>
 *   <li>若不存在：不启用 auditing，@CreatedBy/@LastModifiedBy 保持 null</li>
 * </ul>
 *
 * <h3>业务项目集成示例</h3>
 * <pre>{@code
 * // 在 cartisan-security 或业务项目中
 * @Bean
 * public AuditorAware<String> auditorAware() {
 *     return () -> {
 *         // 从 SecurityContext 获取当前用户
 *         String currentUser = SecurityContext.getCurrentUser();
 *         return Optional.ofNullable(currentUser);
 *     };
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
@Configuration
@ConditionalOnBean(AuditorAware.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfiguration {
    // 无需额外代码，注解即完成配置
}
