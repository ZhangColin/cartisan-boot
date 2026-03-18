package com.cartisan.data.jpa.repository.impl;

import com.cartisan.event.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 领域事件发布器持有者。
 *
 * <p>用于在非 Spring 管理的 Repository 实例中获取 {@link DomainEventPublisher}。
 * Repository 实例由 Spring Data JPA 创建，不是 Spring Bean，无法直接使用依赖注入。</p>
 *
 * <p>使用方法：</p>
 * <pre>{@code
 * @Component
 * public class MyConfig {
 *     @Autowired
 *     public void setDomainEventPublisher(DomainEventPublisher publisher) {
 *         DomainEventPublisherHolder.setPublisher(publisher);
 *     }
 * }
 * }</pre>
 */
public final class DomainEventPublisherHolder {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisherHolder.class);

    private static volatile DomainEventPublisher publisher;

    private DomainEventPublisherHolder() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 设置领域事件发布器（由 Spring 调用）。
     *
     * @param publisher 领域事件发布器，不能为 null
     */
    public static void setPublisher(DomainEventPublisher publisher) {
        DomainEventPublisherHolder.publisher = Objects.requireNonNull(publisher, "publisher cannot be null");
    }

    /**
     * 获取领域事件发布器。
     *
     * @return 领域事件发布器，可能为 null（如果尚未设置）
     */
    public static DomainEventPublisher getPublisher() {
        return publisher;
    }
}
