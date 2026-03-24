package com.cartisan.data.jpa.repository.impl;

import com.cartisan.core.domain.AbstractAggregateRoot;
import com.cartisan.core.domain.AggregateRoot;
import com.cartisan.core.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BaseRepositoryImpl.class);

    private final EntityManager entityManager;
    private final JpaEntityInformation<T, ?> entityInformation;

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
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
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
     * 删除实体，软删除实体自动标记为已删除。
     *
     * <p>如果实体实现了 {@link com.cartisan.data.jpa.domain.SoftDeletable}，
     * 则调用 {@code markAsDeleted()} 并保存，否则执行物理删除。</p>
     *
     * <p>也支持通过反射调用 {@code markAsDeleted()} 方法，以支持继承自
     * {@link AbstractAggregateRoot} 的软删除实体。</p>
     *
     * @param entity 要删除的实体，不能为 null
     */
    @Override
    public void delete(T entity) {
        if (entity instanceof com.cartisan.data.jpa.domain.SoftDeletable softDeletable) {
            softDeletable.markAsDeleted();
            save(entity);  // 复用 save() 的事件发布逻辑
        } else if (hasMarkAsDeletedMethod(entity)) {
            markAsDeletedViaReflection(entity);
            save(entity);
        } else {
            super.delete(entity);  // 非软删除实体，物理删除
        }
    }

    /**
     * 根据 ID 删除实体，软删除实体自动标记为已删除。
     *
     * <p>先通过 ID 查找实体，然后调用 {@link #delete(Object)}。</p>
     *
     * @param id 实体 ID，不能为 null
     * @throws IllegalArgumentException 如果 id 为 null
     */
    @Override
    public void deleteById(ID id) {
        findById(id).ifPresent(this::delete);
    }

    /**
     * 批量删除实体，软删除实体自动标记为已删除。
     *
     * <p>实现了 {@link com.cartisan.data.jpa.domain.SoftDeletable} 的实体或
     * 带有 {@code markAsDeleted()} 方法的实体执行软删除，其他实体执行物理删除。</p>
     *
     * @param entities 要删除的实体集合，不能为 null
     */
    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        java.util.List<T> softDeletable = new java.util.ArrayList<>();
        java.util.List<T> physicalDelete = new java.util.ArrayList<>();

        entities.forEach(e -> {
            if (e instanceof com.cartisan.data.jpa.domain.SoftDeletable || hasMarkAsDeletedMethod(e)) {
                softDeletable.add(e);
            } else {
                physicalDelete.add(e);
            }
        });

        // 软删除：标记并保存
        softDeletable.forEach(e -> {
            if (e instanceof com.cartisan.data.jpa.domain.SoftDeletable entity) {
                entity.markAsDeleted();
            } else {
                markAsDeletedViaReflection(e);
            }
        });
        if (!softDeletable.isEmpty()) {
            saveAll(softDeletable);
        }

        // 物理删除
        if (!physicalDelete.isEmpty()) {
            super.deleteAll(physicalDelete);
        }
    }

    /**
     * 发布聚合根上的领域事件。
     *
     * <p>仅当实体是 {@link AbstractAggregateRoot} 的实例时发布事件。</p>
     *
     * @param entity 可能包含事件的实体
     */
    private void publishDomainEvents(T entity) {
        if (!(entity instanceof AbstractAggregateRoot<?> aggregateRoot)) {
            return;
        }
        List<DomainEvent> events = aggregateRoot.getDomainEvents();
        if (events.isEmpty()) {
            return;
        }
        var publisher = DomainEventPublisherHolder.getPublisher();
        if (publisher == null) {
            log.warn("DomainEventPublisher not configured: {} domain event(s) on {} will be dropped. " +
                     "Ensure cartisan-event is on the classpath and CartisanEventAutoConfiguration is active.",
                     events.size(), entity.getClass().getSimpleName());
            aggregateRoot.clearDomainEvents();
            return;
        }
        events.forEach(publisher::publish);
        aggregateRoot.clearDomainEvents();
    }

    /**
     * 判断当前 Repository 的实体类型是否支持软删除。
     *
     * @return true 如果实体类型是 SoftDeletable 的子类
     */
    private boolean isSoftDeletableEntityType() {
        return com.cartisan.data.jpa.domain.SoftDeletable.class.isAssignableFrom(getDomainClass());
    }

    /**
     * 删除所有实体。
     *
     * <p>如果实体类型支持软删除，则批量更新所有记录的 deleted 标记，
     * 否则执行物理删除。</p>
     */
    @Override
    public void deleteAll() {
        if (isSoftDeletableEntityType()) {
            // 软删除：批量更新所有记录
            jakarta.persistence.Query query = entityManager.createQuery(
                "UPDATE " + entityInformation.getEntityName() + " e SET e.deleted = true"
            );
            query.executeUpdate();
        } else {
            super.deleteAll();
        }
    }

    /**
     * 根据 ID 批量删除实体。
     *
     * <p>先查找所有实体，然后调用 {@link #deleteAll(Iterable)}。</p>
     *
     * @param ids 实体 ID 集合，不能为 null
     */
    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        java.util.List<T> entities = new java.util.ArrayList<>();
        ids.forEach(id -> findById(id).ifPresent(entities::add));
        deleteAll(entities);
    }

    /**
     * 检查实体是否有 markAsDeleted() 方法。
     *
     * @param entity 要检查的实体
     * @return true 如果实体有 markAsDeleted() 方法
     */
    private boolean hasMarkAsDeletedMethod(T entity) {
        try {
            entity.getClass().getMethod("markAsDeleted");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * 通过反射调用实体的 markAsDeleted() 方法。
     *
     * @param entity 要标记为删除的实体
     */
    private void markAsDeletedViaReflection(T entity) {
        try {
            java.lang.reflect.Method method = entity.getClass().getMethod("markAsDeleted");
            method.invoke(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mark entity as deleted", e);
        }
    }
}
