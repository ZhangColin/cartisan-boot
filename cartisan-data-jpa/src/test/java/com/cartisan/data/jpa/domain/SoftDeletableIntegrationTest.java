package com.cartisan.data.jpa.domain;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 软删除功能集成测试。
 *
 * <p>测试命名遵循 TEST-002 规则：given_{条件}_when_{操作}_then_{预期结果}</p>
 *
 * <p><b>L1 纪律</b>：读过滤的断言前先 {@code flush + clear} 持久化上下文，避免命中 L1 缓存导致
 * 「假通过」（同一事务内已加载的实体 findById 直接返回缓存对象，绕过 SQL 读过滤）。</p>
 *
 * <p>验证软删除 AC3-AC4：</p>
 * <ul>
 *   <li>AC3: 删除时设置 deleted=true，查询时自动过滤</li>
 *   <li>AC4: 已软删记录对所有仓储读方法（含 findById）不可见</li>
 * </ul>
 */
@DataJpaTest
@EntityScan(basePackageClasses = TestSoftDeletableEntity.class)
class SoftDeletableIntegrationTest {

    @Autowired
    private TestSoftDeletableEntityRepository repository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    // ==================== AC3: 软删除设置和查询过滤 ====================
    @Test
    void given_existingEntities_when_softDelete_one_then_filteredFromQuery() {
        // Given: 创建 3 个实体
        TestSoftDeletableEntity entity1 = new TestSoftDeletableEntity();
        entity1.setName("Entity 1");
        TestSoftDeletableEntity entity2 = new TestSoftDeletableEntity();
        entity2.setName("Entity 2");
        TestSoftDeletableEntity entity3 = new TestSoftDeletableEntity();
        entity3.setName("Entity 3");

        repository.save(entity1);
        repository.save(entity2);
        repository.save(entity3);

        assertThat(repository.findAll()).hasSize(3);

        // When: 软删除 entity2
        entity2.setDeleted(true);
        repository.saveAndFlush(entity2);

        // Then: 常规查询只能找到 2 个实体
        List<TestSoftDeletableEntity> activeEntities = repository.findAll();
        assertThat(activeEntities).hasSize(2);
        assertThat(activeEntities).extracting(TestSoftDeletableEntity::getName)
                .containsExactly("Entity 1", "Entity 3");
    }

    // ==================== AC4: 已删记录对 findById 不可见（清 L1 后） ====================
    @Test
    void given_softDeletedEntity_when_findById_then_notFound() {
        // Given: 创建并软删除一个实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("To Be Deleted");
        TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

        saved.setDeleted(true);
        repository.saveAndFlush(saved);

        // 清 L1：避免持久化上下文缓存命中导致 findById「假通过」，强制走 SQL 触发读过滤
        entityManager.flush();
        entityManager.clear();

        // When & Then: findById 被读过滤排除，对已删记录返回空
        assertThat(repository.findById(saved.getId()))
                .as("findById（清 L1 后）应被读过滤排除，对已删记录返回空")
                .isEmpty();
    }

    // ==================== AC4: 通过常规查询找不到已删除实体 ====================
    @Test
    void given_softDeletedEntity_when_findAll_then_notIncluded() {
        // Given: 创建并软删除一个实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("Deleted Entity");
        TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

        saved.setDeleted(true);
        repository.saveAndFlush(saved);

        // When: 常规查询所有实体
        List<TestSoftDeletableEntity> allEntities = repository.findAll();

        // Then: 已删除实体不在结果中
        assertThat(allEntities).isEmpty();
    }

    // ==================== 边界场景：多次删除（幂等） ====================
    @Test
    void given_deletedEntity_when_setDeletedAgain_then_remainsDeleted() {
        // Given: 已删除的实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("Entity");
        TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

        saved.setDeleted(true);
        repository.saveAndFlush(saved);

        // When: 再次标记为已删除
        saved.setDeleted(true);
        repository.saveAndFlush(saved);

        // 清 L1：读过滤断言前先 flush + clear，避免缓存命中「假通过」
        entityManager.flush();
        entityManager.clear();

        // Then: 幂等——仍被读过滤排除（findById / findAll 均不可见）
        assertThat(repository.findById(saved.getId())).isEmpty();
        assertThat(repository.findAll()).isEmpty();

        // 且记录仍在、deleted=true（原生 SQL 绕过读过滤，是查询已删数据的逃生通道）
        Long survivingDeleted = ((Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM test_soft_deletable_entity WHERE id = :id AND deleted = TRUE")
                .setParameter("id", saved.getId())
                .getSingleResult()).longValue();
        assertThat(survivingDeleted).as("幂等：再次标记后记录仍在且 deleted=true").isEqualTo(1L);
    }

    // ==================== 边界场景：删除后查询条件 ====================
    @Test
    void given_mixedEntities_when_findByName_then_onlyActiveReturned() {
        // Given: 创建多个实体，部分已删除
        TestSoftDeletableEntity active1 = new TestSoftDeletableEntity();
        active1.setName("SameName");
        TestSoftDeletableEntity deleted1 = new TestSoftDeletableEntity();
        deleted1.setName("SameName");
        TestSoftDeletableEntity active2 = new TestSoftDeletableEntity();
        active2.setName("SameName");

        repository.save(active1);
        repository.save(deleted1);
        repository.save(active2);
        repository.flush();

        deleted1.setDeleted(true);
        repository.saveAndFlush(deleted1);

        // When: 按名称查询
        List<TestSoftDeletableEntity> found = repository.findByName("SameName");

        // Then: 只返回未删除的
        assertThat(found).hasSize(2);
        assertThat(found).allMatch(e -> !e.isDeleted());
    }

    // ==================== 新增：自动软删除测试 ====================
    @Test
    void given_existingEntity_when_deleteAuto_then_markedAsDeleted() {
        // Given: 创建实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("Auto Delete");
        TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

        assertThat(repository.findAll()).hasSize(1);

        // When: 使用 repository.delete() 自动软删除
        repository.delete(saved);

        // Then: 实体被标记为已删除，常规查询过滤
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void given_existingEntity_when_deleteByIdAuto_then_markedAsDeleted() {
        // Given: 创建实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("Auto Delete By Id");
        TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

        assertThat(repository.findAll()).hasSize(1);

        // When: 使用 repository.deleteById() 自动软删除
        repository.deleteById(saved.getId());

        // Then: 实体被标记为已删除
        assertThat(repository.findAll()).isEmpty();
    }
}
