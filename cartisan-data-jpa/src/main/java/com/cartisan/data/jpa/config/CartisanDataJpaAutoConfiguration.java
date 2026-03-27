package com.cartisan.data.jpa.config;

import com.cartisan.data.jpa.converter.EnumConverterRegistrar;
import com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl;
import com.cartisan.data.jpa.repository.impl.DomainEventPublisherHolder;
import com.cartisan.event.DomainEventPublisher;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

import java.util.Optional;
import java.util.Set;

/**
 * cartisan-data-jpa 模块自动配置。
 *
 * <p>配置内容：
 * <ul>
 *   <li>领域事件发布器持有者 — 使 Repository 实例能够发布领域事件</li>
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
     * 配置领域事件发布器持有者。
     *
     * <p>将 {@link DomainEventPublisher} Bean 注入到
     * {@link DomainEventPublisherHolder} 中，供 Repository 实例使用。</p>
     *
     * @param publisher 领域事件发布器，由 Spring 提供
     * @return 配置器 Runnable，在容器启动时执行
     */
    @Bean
    public Runnable configureDomainEventPublisherHolder(DomainEventPublisher publisher) {
        return () -> DomainEventPublisherHolder.setPublisher(publisher);
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

    /**
     * 扫描枚举转换器类型。
     * <p>
     * 在应用上下文刷新完成后扫描所有带 {@link com.cartisan.data.jpa.annotation.EnumConvert} 的字段，
     * 记录发现的枚举类型用于调试。
     * <p>
     * 使用 {@link ApplicationListener} 延迟到 {@link EntityManagerFactory} 初始化后执行，
     * 避免 Spring Boot 3.4.x 中的自动配置顺序问题。
     *
     * @param entityManagerFactory JPA EntityManagerFactory
     * @return ApplicationListener
     */
    @Bean
    public ApplicationListener<ContextRefreshedEvent> enumConverterScanner(EntityManagerFactory entityManagerFactory) {
        return event -> {
            EnumConverterRegistrar registrar = new EnumConverterRegistrar();
            Set<Class<?>> enumTypes = registrar.scanEnumTypes(entityManagerFactory);

            if (!enumTypes.isEmpty()) {
                log.info("Discovered {} enum type(s) with @EnumConvert annotation", enumTypes.size());
                for (Class<?> enumType : enumTypes) {
                    log.debug("  - {}", enumType.getSimpleName());
                }
            }
        };
    }
}
