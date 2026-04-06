package com.cartisan.data.jpa.repository.impl;

import com.cartisan.data.jpa.repository.impl.softdelete.ReflectionSoftDeletableEntity;
import com.cartisan.data.jpa.repository.impl.softdelete.ReflectionSoftDeletableRepository;
import com.cartisan.data.jpa.repository.impl.softdelete.RegularTestEntity;
import com.cartisan.data.jpa.repository.impl.softdelete.RegularTestRepository;
import com.cartisan.data.jpa.repository.impl.softdelete.SoftDeletableTestEntity;
import com.cartisan.data.jpa.repository.impl.softdelete.SoftDeletableTestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseRepositoryImpl 软删除功能集成测试。
 *
 * <p>测试命名遵循 shouldX_whenY 风格。</p>
 *
 * <p>验证 BaseRepositoryImpl 的软删除分支覆盖：</p>
 * <ul>
 *   <li>deleteById 软删除和物理删除</li>
 *   <li>delete 软删除和物理删除</li>
 *   <li>deleteAllById 混合场景</li>
 *   <li>deleteAll() 批量软删除</li>
 *   <li>deleteAll(entities) 混合场景</li>
 *   <li>通过反射调用 markAsDeleted() 的场景</li>
 * </ul>
 */
@DataJpaTest
@Import(BaseRepositoryImplIntegrationTest.TestConfig.class)
class BaseRepositoryImplIntegrationTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private SoftDeletableTestRepository softDeletableRepository;

    @Autowired
    private RegularTestRepository regularRepository;

    @Autowired
    private ReflectionSoftDeletableRepository reflectionRepository;

    @AfterEach
    void tearDown() {
        softDeletableRepository.deleteAll();
        regularRepository.deleteAll();
        reflectionRepository.deleteAll();
    }

    // ==================== Step 2: deleteById 软删除 ====================

    @Test
    @Transactional
    void should_markAsDeleted_when_deleteById_onSoftDeletableEntity() {
        // Given: 创建软删除实体
        SoftDeletableTestEntity entity = new SoftDeletableTestEntity();
        entity.setName("To Delete");
        SoftDeletableTestEntity saved = softDeletableRepository.saveAndFlush(entity);

        assertThat(softDeletableRepository.findAll()).hasSize(1);

        // When: 调用 deleteById
        softDeletableRepository.deleteById(saved.getId());

        // Then: 实体被标记为已删除（非物理删除）
        assertThat(softDeletableRepository.findAll()).isEmpty();

        // 通过原生查询验证记录仍存在
        SoftDeletableTestEntity deletedEntity = testEntityManager.find(
            SoftDeletableTestEntity.class, saved.getId());
        assertThat(deletedEntity).isNotNull();
        assertThat(deletedEntity.isDeleted()).isTrue();
    }

    // ==================== Step 3: deleteById 物理删除 ====================

    @Test
    void should_physicalDelete_when_deleteById_onRegularEntity() {
        // Given: 创建常规实体
        RegularTestEntity entity = new RegularTestEntity();
        entity.setName("Regular Entity");
        RegularTestEntity saved = regularRepository.saveAndFlush(entity);

        assertThat(regularRepository.findAll()).hasSize(1);

        // When: 调用 deleteById
        regularRepository.deleteById(saved.getId());

        // Then: 实体被物理删除
        assertThat(regularRepository.findAll()).isEmpty();

        // 验证数据库中不存在
        RegularTestEntity deletedEntity = testEntityManager.find(
            RegularTestEntity.class, saved.getId());
        assertThat(deletedEntity).isNull();
    }

    // ==================== Step 4: delete 软删除分支 ====================

    @Test
    void should_markAsDeleted_when_delete_onSoftDeletableEntity() {
        // Given: 创建软删除实体
        SoftDeletableTestEntity entity = new SoftDeletableTestEntity();
        entity.setName("To Delete");
        SoftDeletableTestEntity saved = softDeletableRepository.saveAndFlush(entity);

        assertThat(softDeletableRepository.findAll()).hasSize(1);

        // When: 调用 delete(entity)
        softDeletableRepository.delete(saved);

        // Then: 实体被标记为已删除
        assertThat(softDeletableRepository.findAll()).isEmpty();

        // 通过原生查询验证记录仍存在
        SoftDeletableTestEntity deletedEntity = testEntityManager.find(
            SoftDeletableTestEntity.class, saved.getId());
        assertThat(deletedEntity).isNotNull();
        assertThat(deletedEntity.isDeleted()).isTrue();
    }

    @Test
    void should_physicalDelete_when_delete_onRegularEntity() {
        // Given: 创建常规实体
        RegularTestEntity entity = new RegularTestEntity();
        entity.setName("Regular Entity");
        RegularTestEntity saved = regularRepository.saveAndFlush(entity);

        assertThat(regularRepository.findAll()).hasSize(1);

        // When: 调用 delete(entity)
        regularRepository.delete(saved);

        // Then: 实体被物理删除
        assertThat(regularRepository.findAll()).isEmpty();

        // 验证数据库中不存在
        RegularTestEntity deletedEntity = testEntityManager.find(
            RegularTestEntity.class, saved.getId());
        assertThat(deletedEntity).isNull();
    }

    // ==================== Step 5: deleteAllById 混合场景 ====================

    @Test
    @Transactional
    void should_mixedDelete_when_deleteAllById_onMixedEntities() {
        // Given: 创建软删除和常规实体
        SoftDeletableTestEntity softEntity1 = new SoftDeletableTestEntity();
        softEntity1.setName("Soft 1");
        SoftDeletableTestEntity softEntity2 = new SoftDeletableTestEntity();
        softEntity2.setName("Soft 2");

        RegularTestEntity regularEntity = new RegularTestEntity();
        regularEntity.setName("Regular");

        softDeletableRepository.save(softEntity1);
        softDeletableRepository.save(softEntity2);
        regularRepository.save(regularEntity);
        testEntityManager.flush();

        Long softId1 = softEntity1.getId();
        Long softId2 = softEntity2.getId();
        Long regularId = regularEntity.getId();

        assertThat(softDeletableRepository.findAll()).hasSize(2);
        assertThat(regularRepository.findAll()).hasSize(1);

        // When: 对每个 repository 调用 deleteAllById
        softDeletableRepository.deleteAllById(List.of(softId1, softId2));
        regularRepository.deleteAllById(List.of(regularId));

        // Then: 软删除实体被标记，常规实体被物理删除
        assertThat(softDeletableRepository.findAll()).isEmpty();
        assertThat(regularRepository.findAll()).isEmpty();

        // 验证软删除实体仍存在但标记为已删除
        SoftDeletableTestEntity deletedSoft1 = testEntityManager.find(
            SoftDeletableTestEntity.class, softId1);
        assertThat(deletedSoft1).isNotNull();
        assertThat(deletedSoft1.isDeleted()).isTrue();

        // 验证常规实体已被物理删除
        RegularTestEntity deletedRegular = testEntityManager.find(
            RegularTestEntity.class, regularId);
        assertThat(deletedRegular).isNull();
    }

    // ==================== Step 6: deleteAll 批量软删除 ====================

    @Test
    void should_markAllAsDeleted_when_deleteAll_onSoftDeletableEntities() {
        // Given: 创建多个软删除实体
        SoftDeletableTestEntity entity1 = new SoftDeletableTestEntity();
        entity1.setName("Entity 1");
        SoftDeletableTestEntity entity2 = new SoftDeletableTestEntity();
        entity2.setName("Entity 2");
        SoftDeletableTestEntity entity3 = new SoftDeletableTestEntity();
        entity3.setName("Entity 3");

        softDeletableRepository.save(entity1);
        softDeletableRepository.save(entity2);
        softDeletableRepository.save(entity3);
        testEntityManager.flush();

        assertThat(softDeletableRepository.findAll()).hasSize(3);

        // When: 调用 deleteAll()
        softDeletableRepository.deleteAll();

        // Then: 所有实体被标记为已删除
        assertThat(softDeletableRepository.findAll()).isEmpty();

        // 验证所有记录仍存在但被标记为已删除（使用原生查询绕过 @SQLRestriction）
        Boolean deleted1 = (Boolean) testEntityManager.getEntityManager()
            .createNativeQuery("SELECT deleted FROM soft_delete_test_entity WHERE id = ?")
            .setParameter(1, entity1.getId())
            .getSingleResult();
        Boolean deleted2 = (Boolean) testEntityManager.getEntityManager()
            .createNativeQuery("SELECT deleted FROM soft_delete_test_entity WHERE id = ?")
            .setParameter(1, entity2.getId())
            .getSingleResult();
        Boolean deleted3 = (Boolean) testEntityManager.getEntityManager()
            .createNativeQuery("SELECT deleted FROM soft_delete_test_entity WHERE id = ?")
            .setParameter(1, entity3.getId())
            .getSingleResult();

        assertThat(deleted1).isNotNull();
        assertThat(deleted1).isTrue();
        assertThat(deleted2).isNotNull();
        assertThat(deleted2).isTrue();
        assertThat(deleted3).isNotNull();
        assertThat(deleted3).isTrue();
    }

    @Test
    void should_physicalDeleteAll_when_deleteAll_onRegularEntities() {
        // Given: 创建多个常规实体
        RegularTestEntity entity1 = new RegularTestEntity();
        entity1.setName("Entity 1");
        RegularTestEntity entity2 = new RegularTestEntity();
        entity2.setName("Entity 2");

        regularRepository.save(entity1);
        regularRepository.save(entity2);
        testEntityManager.flush();

        assertThat(regularRepository.findAll()).hasSize(2);

        // When: 调用 deleteAll()
        regularRepository.deleteAll();

        // Then: 所有实体被物理删除
        assertThat(regularRepository.findAll()).isEmpty();

        // 验证数据库中不存在
        RegularTestEntity deleted1 = testEntityManager.find(
            RegularTestEntity.class, entity1.getId());
        RegularTestEntity deleted2 = testEntityManager.find(
            RegularTestEntity.class, entity2.getId());
        assertThat(deleted1).isNull();
        assertThat(deleted2).isNull();
    }

    // ==================== Step 7: deleteAll(entities) 混合场景 ====================

    @Test
    void should_mixedDelete_when_deleteAllIterable_onMixedEntities() {
        // Given: 创建软删除和常规实体
        SoftDeletableTestEntity softEntity1 = new SoftDeletableTestEntity();
        softEntity1.setName("Soft 1");
        SoftDeletableTestEntity softEntity2 = new SoftDeletableTestEntity();
        softEntity2.setName("Soft 2");

        RegularTestEntity regularEntity1 = new RegularTestEntity();
        regularEntity1.setName("Regular 1");
        RegularTestEntity regularEntity2 = new RegularTestEntity();
        regularEntity2.setName("Regular 2");

        softDeletableRepository.save(softEntity1);
        softDeletableRepository.save(softEntity2);
        regularRepository.save(regularEntity1);
        regularRepository.save(regularEntity2);
        testEntityManager.flush();

        Long softId1 = softEntity1.getId();
        Long softId2 = softEntity2.getId();
        Long regularId1 = regularEntity1.getId();
        Long regularId2 = regularEntity2.getId();

        assertThat(softDeletableRepository.findAll()).hasSize(2);
        assertThat(regularRepository.findAll()).hasSize(2);

        // When: 对每个 repository 调用 deleteAll(entities)
        softDeletableRepository.deleteAll(List.of(softEntity1, softEntity2));
        regularRepository.deleteAll(List.of(regularEntity1, regularEntity2));

        // Then: 软删除实体被标记，常规实体被物理删除
        assertThat(softDeletableRepository.findAll()).isEmpty();
        assertThat(regularRepository.findAll()).isEmpty();

        // 验证软删除实体仍存在但标记为已删除
        SoftDeletableTestEntity deletedSoft1 = testEntityManager.find(
            SoftDeletableTestEntity.class, softId1);
        assertThat(deletedSoft1).isNotNull();
        assertThat(deletedSoft1.isDeleted()).isTrue();

        // 验证常规实体已被物理删除
        RegularTestEntity deletedRegular1 = testEntityManager.find(
            RegularTestEntity.class, regularId1);
        assertThat(deletedRegular1).isNull();
    }

    // ==================== 通过反射调用 markAsDeleted() ====================

    @Test
    void should_markAsDeletedViaReflection_when_delete_onEntityWithMarkAsDeletedMethod() {
        // Given: 创建带 markAsDeleted() 方法的实体（不实现 SoftDeletable 接口）
        ReflectionSoftDeletableEntity entity = new ReflectionSoftDeletableEntity();
        entity.setName("Reflection Delete");
        ReflectionSoftDeletableEntity saved = reflectionRepository.saveAndFlush(entity);

        assertThat(reflectionRepository.findAll()).hasSize(1);

        // When: 调用 delete，BaseRepositoryImpl 会通过反射调用 markAsDeleted()
        reflectionRepository.delete(saved);

        // Then: 实体被标记为已删除
        assertThat(reflectionRepository.findAll()).isEmpty();

        // 验证记录仍存在但被标记为已删除
        ReflectionSoftDeletableEntity deleted = testEntityManager.find(
            ReflectionSoftDeletableEntity.class, saved.getId());
        assertThat(deleted).isNotNull();
        assertThat(deleted.isDeleted()).isTrue();
    }

    @Test
    void should_markAsDeletedViaReflection_when_deleteById_onEntityWithMarkAsDeletedMethod() {
        // Given: 创建带 markAsDeleted() 方法的实体
        ReflectionSoftDeletableEntity entity = new ReflectionSoftDeletableEntity();
        entity.setName("Reflection Delete By Id");
        ReflectionSoftDeletableEntity saved = reflectionRepository.saveAndFlush(entity);

        assertThat(reflectionRepository.findAll()).hasSize(1);

        // When: 调用 deleteById
        reflectionRepository.deleteById(saved.getId());

        // Then: 实体被标记为已删除
        assertThat(reflectionRepository.findAll()).isEmpty();

        // 验证记录仍存在但被标记为已删除
        ReflectionSoftDeletableEntity deleted = testEntityManager.find(
            ReflectionSoftDeletableEntity.class, saved.getId());
        assertThat(deleted).isNotNull();
        assertThat(deleted.isDeleted()).isTrue();
    }

    // ==================== 边界场景：删除不存在的实体 ====================

    @Test
    void should_doNothing_when_deleteById_onNonExistentSoftDeletableEntity() {
        // Given: 数据库中不存在该 ID
        Long nonExistentId = 999999L;

        // When: 调用 deleteById
        softDeletableRepository.deleteById(nonExistentId);

        // Then: 不抛出异常
        assertThat(softDeletableRepository.findAll()).isEmpty();
    }

    @Test
    void should_doNothing_when_deleteById_onNonExistentRegularEntity() {
        // Given: 数据库中不存在该 ID
        Long nonExistentId = 999999L;

        // When: 调用 deleteById
        regularRepository.deleteById(nonExistentId);

        // Then: 不抛出异常
        assertThat(regularRepository.findAll()).isEmpty();
    }

    // ==================== 边界场景：空集合删除 ====================

    @Test
    void should_doNothing_when_deleteAll_withEmptyList() {
        // Given: 空列表
        List<SoftDeletableTestEntity> emptyList = List.of();

        // When: 调用 deleteAll
        softDeletableRepository.deleteAll(emptyList);

        // Then: 不抛出异常
        assertThat(softDeletableRepository.findAll()).isEmpty();
    }

    @Test
    void should_doNothing_when_deleteAllById_withEmptyList() {
        // Given: 空列表
        List<Long> emptyList = List.of();

        // When: 调用 deleteAllById
        softDeletableRepository.deleteAllById(emptyList);

        // Then: 不抛出异常
        assertThat(softDeletableRepository.findAll()).isEmpty();
    }

    /**
     * 测试配置。
     */
    @Configuration
    @EnableJpaRepositories(
        basePackageClasses = {
            SoftDeletableTestRepository.class,
            RegularTestRepository.class,
            ReflectionSoftDeletableRepository.class
        },
        repositoryBaseClass = BaseRepositoryImpl.class
    )
    @EntityScan(basePackageClasses = {
        SoftDeletableTestEntity.class,
        RegularTestEntity.class,
        ReflectionSoftDeletableEntity.class
    })
    static class TestConfig {
    }
}
