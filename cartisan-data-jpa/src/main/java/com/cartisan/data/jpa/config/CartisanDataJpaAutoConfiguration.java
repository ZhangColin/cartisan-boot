package com.cartisan.data.jpa.config;

import com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

import java.util.Optional;

/**
 * cartisan-data-jpa 模块自动配置。
 *
 * <p>配置内容：
 * <ul>
 *   <li>Repository 基类 — 全局配置 BaseRepositoryImpl 为所有 Repository 基类</li>
 *   <li>JPA Auditing — 启用审计功能，提供默认 {@code AuditorAware<Long>} Bean</li>
 * </ul>
 *
 * <h3>JPA Auditing 集成</h3>
 * <p>默认提供一个返回 {@code Optional.empty()} 的 {@link AuditorAware} Bean，
 * 业务系统可通过自定义 {@code AuditorAware<Long>} Bean 覆盖默认实现：</p>
 * <pre>{@code
 * @Bean
 * public AuditorAware<Long> auditorAware() {
 *     return () -> {
 *         // 从 SecurityContext 获取当前用户ID
 *         Long currentUserId = SecurityContext.getCurrentUserId();
 *         return Optional.ofNullable(currentUserId);
 *     };
 * }
 * }</pre>
 */
@AutoConfiguration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class CartisanDataJpaAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CartisanDataJpaAutoConfiguration.class);

    /**
     * 默认 AuditorAware Bean。
     *
     * <p>提供空实现避免启动失败，业务系统可通过自定义 Bean 覆盖。</p>
     *
     * @return 返回 {@code Optional.empty()} 的 AuditorAware
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditorAware<Long> auditorAware() {
        return () -> Optional.empty();
    }

    /**
     * 全局配置 Repository 基类。
     *
     * <p>所有继承 {@link com.cartisan.data.jpa.repository.BaseRepository} 的接口
     * 自动使用 {@link com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl}，
     * 业务端无需手动指定 {@code repositoryBaseClass}。</p>
     *
     * <p>参考 @ docs/PITFALLS.md 规则 BOOT-001</p>
     *
     * @return BeanPostProcessor Bean
     */
    @Bean
    public BeanPostProcessor repositoryFactoryBeanCustomizer() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof JpaRepositoryFactoryBean<?, ?, ?> factoryBean) {
                    factoryBean.setRepositoryBaseClass(BaseRepositoryImpl.class);
                }
                return bean;
            }
        };
    }
}
