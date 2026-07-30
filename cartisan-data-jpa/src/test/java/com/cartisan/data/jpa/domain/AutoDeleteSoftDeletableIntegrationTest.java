package com.cartisan.data.jpa.domain;

import com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动软删除功能集成测试。
 *
 * <p>验证 repository.delete() 和 repository.deleteById() 自动触发软删除。</p>
 *
 * <p><b>L1 纪律</b>：读过滤的断言前先 {@code flush + clear} 持久化上下文，避免命中 L1 缓存导致
 * 「假通过」（同一事务内已加载的实体 findById 直接返回缓存对象，绕过 SQL 读过滤）。</p>
 *
 * <p>使用 TestAggregateRootWithSoftDelete 和 TestAggregateRootWithSoftDeleteRepository，
 * 因为 BaseRepositoryImpl 只对实现 AggregateRoot 接口的实体生效。</p>
 *
 * <p>使用内嵌的 TestApplication 作为测试配置，避免与 JpaTestApplication 冲突。</p>
 */
@DataJpaTest
@Import(AutoDeleteSoftDeletableIntegrationTest.TestConfig.class)
class AutoDeleteSoftDeletableIntegrationTest {

    @Autowired
    private TestAggregateRootWithSoftDeleteRepository repository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    // ==================== delete(entity) 自动软删除 ====================
    @Test
    void should_markAsDeleted_when_deleteEntity() {
        // Given: 创建实体
        TestAggregateRootWithSoftDelete entity = new TestAggregateRootWithSoftDelete();
        entity.setName("To Delete");
        TestAggregateRootWithSoftDelete saved = repository.saveAndFlush(entity);

        assertThat(repository.findAll()).hasSize(1);

        // When: 调用 delete(entity)
        repository.delete(saved);

        // Then: 实体被标记为已删除，查询时过滤
        assertThat(repository.findAll()).isEmpty();
    }

    // ==================== deleteById(id) 自动软删除 ====================
    @Test
    @Transactional
    void should_markAsDeleted_when_deleteById() {
        // Given: 创建实体
        TestAggregateRootWithSoftDelete entity = new TestAggregateRootWithSoftDelete();
        entity.setName("To Delete");
        TestAggregateRootWithSoftDelete saved = repository.saveAndFlush(entity);

        assertThat(repository.findAll()).hasSize(1);

        // When: 调用 deleteById(id)
        repository.deleteById(saved.getId());

        // Then: 实体被标记为已删除
        assertThat(repository.findAll()).isEmpty();
    }

    // ==================== deleteAll(Iterable) 混合实体 ====================
    @Test
    void should_markAllAsDeleted_when_deleteAllIterable() {
        // Given: 创建多个实体
        TestAggregateRootWithSoftDelete entity1 = new TestAggregateRootWithSoftDelete();
        entity1.setName("Entity 1");
        TestAggregateRootWithSoftDelete entity2 = new TestAggregateRootWithSoftDelete();
        entity2.setName("Entity 2");
        TestAggregateRootWithSoftDelete entity3 = new TestAggregateRootWithSoftDelete();
        entity3.setName("Entity 3");

        repository.save(entity1);
        repository.save(entity2);
        repository.save(entity3);
        repository.flush();

        assertThat(repository.findAll()).hasSize(3);

        // When: 批量删除
        repository.deleteAll(List.of(entity1, entity2, entity3));

        // Then: 全部被标记为已删除
        assertThat(repository.findAll()).isEmpty();
    }

    // ==================== deleteAll() 批量删除 ====================
    @Test
    void should_markAllAsDeleted_when_deleteAll() {
        // Given: 创建多个实体
        TestAggregateRootWithSoftDelete entity1 = new TestAggregateRootWithSoftDelete();
        entity1.setName("Entity 1");
        TestAggregateRootWithSoftDelete entity2 = new TestAggregateRootWithSoftDelete();
        entity2.setName("Entity 2");

        repository.save(entity1);
        repository.save(entity2);
        repository.flush();

        assertThat(repository.findAll()).hasSize(2);

        // When: 删除所有
        repository.deleteAll();

        // Then: 全部被标记为已删除
        assertThat(repository.findAll()).isEmpty();
    }

    // ==================== 已删记录对 findById 不可见（清 L1 后） ====================
    @Test
    void should_returnEmpty_when_findByIdAfterDelete() {
        // Given: 创建并删除实体
        TestAggregateRootWithSoftDelete entity = new TestAggregateRootWithSoftDelete();
        entity.setName("Deleted");
        TestAggregateRootWithSoftDelete saved = repository.saveAndFlush(entity);

        repository.delete(saved);

        // 清 L1：避免持久化上下文缓存命中导致 findById「假通过」，强制走 SQL 触发读过滤
        entityManager.flush();
        entityManager.clear();

        // When & Then: findById 被读过滤排除，对已删记录返回空
        assertThat(repository.findById(saved.getId()))
                .as("findById（清 L1 后）应被读过滤排除，对已删记录返回空")
                .isEmpty();
    }

    /**
     * 测试配置。
     *
     * <p>配置 BaseRepositoryImpl 为 Repository 基类。</p>
     */
    @org.springframework.context.annotation.Configuration
    @EnableJpaRepositories(
        basePackageClasses = TestAggregateRootWithSoftDeleteRepository.class,
        repositoryBaseClass = BaseRepositoryImpl.class
    )
    @org.springframework.boot.autoconfigure.domain.EntityScan(basePackageClasses = TestAggregateRootWithSoftDelete.class)
    static class TestConfig {
    }
}
