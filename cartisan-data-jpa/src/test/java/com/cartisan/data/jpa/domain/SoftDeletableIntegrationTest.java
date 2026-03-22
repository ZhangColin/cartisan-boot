package com.cartisan.data.jpa.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 软删除功能集成测试。
 *
 * <p>测试命名遵循 TEST-002 规则：given_{条件}_when_{操作}_then_{预期结果}</p>
 *
 * <p>验证软删除 AC3-AC4：</p>
 * <ul>
 *   <li>AC3: 删除时设置 deleted=true，查询时自动过滤</li>
 *   <li>AC4: 软删除实体不能通过常规查询找到，但可通过 ID 直接查询</li>
 * </ul>
 */
@DataJpaTest
@EntityScan(basePackageClasses = TestSoftDeletableEntity.class)
class SoftDeletableIntegrationTest {

    @Autowired
    private TestSoftDeletableEntityRepository repository;

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

    // ==================== AC4: 通过 ID 仍可找到已删除实体 ====================
    @Test
    @Transactional
    void given_softDeletedEntity_when_findById_then_stillFound() {
        // Given: 创建并软删除一个实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("To Be Deleted");
        TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

        saved.setDeleted(true);
        repository.saveAndFlush(saved);

        // When: 通过 ID 查询
        TestSoftDeletableEntity found = repository.findById(saved.getId()).orElse(null);

        // Then: 仍能找到该实体
        assertThat(found).isNotNull();
        assertThat(found.isDeleted()).isTrue();
        assertThat(found.getName()).isEqualTo("To Be Deleted");
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

    // ==================== 边界场景：多次删除 ====================
    @Test
    void given_deletedEntity_when_setDeletedAgain_then_remainsDeleted() {
        // Given: 已删除的实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("Entity");
        TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

        saved.setDeleted(true);
        repository.saveAndFlush(saved);

        // When: 再次设置为已删除
        saved.setDeleted(true);
        repository.saveAndFlush(saved);

        // When & Then: 仍已删除，幂等性
        assertThat(repository.findById(saved.getId())).isPresent()
                .hasValueSatisfying(e -> assertThat(e.isDeleted()).isTrue());
        assertThat(repository.findAll()).isEmpty();
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
