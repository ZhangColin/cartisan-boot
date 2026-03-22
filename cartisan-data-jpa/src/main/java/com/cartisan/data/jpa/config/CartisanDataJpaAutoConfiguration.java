package com.cartisan.data.jpa.config;

import com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl;
import com.cartisan.data.jpa.repository.impl.DomainEventPublisherHolder;
import com.cartisan.event.DomainEventPublisher;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;

/**
 * cartisan-data-jpa 模块自动配置。
 *
 * <p>配置内容：
 * <ul>
 *   <li>领域事件发布器持有者 — 使 Repository 实例能够发布领域事件</li>
 *   <li>Repository 基类 — 全局配置 BaseRepositoryImpl 为所有 Repository 基类</li>
 *   <li>JPA Auditing — 当存在 {@code AuditorAware} Bean 时自动启用</li>
 * </ul>
 */
@AutoConfiguration
@Import(JpaAuditingConfiguration.class)
public class CartisanDataJpaAutoConfiguration {

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
}
