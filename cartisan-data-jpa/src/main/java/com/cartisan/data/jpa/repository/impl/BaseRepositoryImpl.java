package com.cartisan.data.jpa.repository.impl;

import com.cartisan.core.domain.AbstractAggregateRoot;
import com.cartisan.core.domain.AggregateRoot;
import com.cartisan.core.domain.DomainEvent;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.List;

/**
 * Repository 实现基类，在保存聚合根时自动发布领域事件。
 *
 * <p>继承 {@link SimpleJpaRepository} 并重写 {@link #save(Object)} 方法，
 * 在 JPA 持久化后发布聚合根上的领域事件。</p>
 *
 * @param <T>  聚合根类型，必须实现 {@link AggregateRoot}
 * @param <ID> 聚合根标识符类型，必须实现 {@link Serializable}
 */
public class BaseRepositoryImpl<T extends AggregateRoot<?>, ID extends Serializable>
        extends SimpleJpaRepository<T, ID> {

    /**
     * 创建 Repository 实例（由 Spring Data JPA 调用）。
     *
     * @param entityInformation JPA 实体信息，不能为 null
     * @param entityManager      JPA 实体管理器，不能为 null
     */
    public BaseRepositoryImpl(
            JpaEntityInformation<T, ?> entityInformation,
            EntityManager entityManager) {
        super(entityInformation, entityManager);
    }

    /**
     * 保存实体并自动发布领域事件。
     *
     * <p>如果实体是 {@link AbstractAggregateRoot} 的实例，
     * 则在保存后发布其上注册的所有领域事件，然后清空事件列表。</p>
     *
     * @param entity 要保存的实体，不能为 null
     * @param <S>    实体类型
     * @return 保存后的实体
     */
    @Override
    @SuppressWarnings("unchecked")
    public <S extends T> S save(S entity) {
        S savedEntity = super.save(entity);
        // 使用原始 entity 发布事件，因为 savedEntity 可能是新实例
        publishDomainEvents(entity);
        return savedEntity;
    }

    /**
     * 发布聚合根上的领域事件。
     *
     * <p>仅当实体是 {@link AbstractAggregateRoot} 的实例时发布事件。</p>
     *
     * @param entity 可能包含事件的实体
     */
    private void publishDomainEvents(T entity) {
        var publisher = DomainEventPublisherHolder.getPublisher();
        if (entity instanceof AbstractAggregateRoot<?> aggregateRoot && publisher != null) {
            List<DomainEvent> events = aggregateRoot.getDomainEvents();
            events.forEach(publisher::publish);
            aggregateRoot.clearDomainEvents();
        }
    }
}
