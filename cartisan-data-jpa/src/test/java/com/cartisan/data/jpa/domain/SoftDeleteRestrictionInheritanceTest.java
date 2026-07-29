package com.cartisan.data.jpa.domain;

import com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 软删除读过滤（继承路径）回归测试。
 *
 * <p>实体 {@link TestInheritedSoftDelete} 继承 {@link AuditableSoftDeletable}、自身不声明任何注解，
 * 完全模拟下游应用（如 admin 的 AdminUser）的真实用法。软删除读过滤由
 * {@code SoftDeleteRestrictionContributor} 在元模型构建期自动注册。</p>
 *
 * <p><b>L1 纪律</b>：所有读过滤断言前先 {@code flush + clear} 持久化上下文，
 * 强制读路径走 DB（而非命中 L1 缓存），否则会掩盖 restriction 未生效的问题。</p>
 */
@DataJpaTest
@Import(SoftDeleteRestrictionInheritanceTest.TestConfig.class)
class SoftDeleteRestrictionInheritanceTest {

    @Autowired
    private TestInheritedSoftDeleteRepository repository;

    @Autowired
    private EntityManager entityManager;

    /**
     * 创建并软删一个实体，返回其 id。软删后 flush + clear，保证后续读路径走 DB。
     */
    private Long softDeleteAndClear(String name) {
        TestInheritedSoftDelete entity = new TestInheritedSoftDelete();
        entity.setName(name);
        TestInheritedSoftDelete saved = repository.saveAndFlush(entity);

        repository.delete(saved);
        entityManager.flush();
        entityManager.clear();
        return saved.getId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldExcludeDeletedFromFindById_when_inheritedSoftDelete() {
        Long deletedId = softDeleteAndClear("repro");

        assertThat(repository.findById(deletedId))
                .as("findById（清 L1 后）应排除已软删记录")
                .isEmpty();
    }

    @Test
    void shouldExcludeDeletedFromFindAll_when_inheritedSoftDelete() {
        softDeleteAndClear("repro");

        assertThat(repository.findAll())
                .as("findAll 应排除已软删记录")
                .isEmpty();
    }

    @Test
    void shouldExcludeDeletedFromCount_when_inheritedSoftDelete() {
        softDeleteAndClear("repro");

        assertThat(repository.count())
                .as("count 不应计入已软删记录")
                .isZero();
    }

    @Test
    void shouldExcludeDeletedFromDerivedQuery_when_inheritedSoftDelete() {
        softDeleteAndClear("repro");

        assertThat(repository.findByName("repro"))
                .as("派生查询应排除已软删记录")
                .isEmpty();
    }

    @Test
    void shouldExcludeDeletedFromExplicitJpql_when_inheritedSoftDelete() {
        softDeleteAndClear("repro");

        assertThat(repository.jpqlByName("repro"))
                .as("显式 JPQL 应排除已软删记录（@SQLRestriction 作用于 Hibernate 生成的 SQL）")
                .isEmpty();
    }

    @Test
    void shouldExcludeDeletedFromSpecification_when_inheritedSoftDelete() {
        softDeleteAndClear("repro");

        Specification<TestInheritedSoftDelete> all = (root, query, builder) -> builder.conjunction();

        assertThat(repository.findAll(all))
                .as("Specification 查询应排除已软删记录")
                .isEmpty();
    }

    @Test
    void shouldIncludeActiveInFindAll_when_inheritedSoftDelete() {
        TestInheritedSoftDelete active = new TestInheritedSoftDelete();
        active.setName("active");
        repository.saveAndFlush(active);
        flushAndClear();

        assertThat(repository.findAll())
                .as("未删除记录应正常返回")
                .hasSize(1)
                .extracting(TestInheritedSoftDelete::getName)
                .containsExactly("active");
    }

    @org.springframework.context.annotation.Configuration
    @EnableJpaRepositories(
            basePackageClasses = TestInheritedSoftDeleteRepository.class,
            repositoryBaseClass = BaseRepositoryImpl.class
    )
    @EntityScan(basePackageClasses = TestInheritedSoftDelete.class)
    @org.springframework.data.jpa.repository.config.EnableJpaAuditing(auditorAwareRef = "auditorAware")
    static class TestConfig {

        @org.springframework.context.annotation.Bean
        org.springframework.data.domain.AuditorAware<Long> auditorAware() {
            return java.util.Optional::empty;
        }
    }
}
