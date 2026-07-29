package com.cartisan.data.jpa.domain;

import com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 显式声明 {@code @SQLRestriction} 的读路径语义回归测试。
 *
 * <p>实体 {@link TestAggregateRootWithSoftDelete} 在自身声明 {@code @SQLRestriction("deleted = false")}。
 * 验证 restriction 对各读路径的作用边界：{@code find}/派生查询/显式 JPQL 均过滤已删记录；
 * 原生 SQL 直接绕过 Hibernate 生成的 SQL，restriction 不作用其上。</p>
 *
 * <p><b>L1 纪律</b>：读断言前 {@code flush + clear} 持久化上下文。</p>
 */
@DataJpaTest
@Import(SoftDeleteRestrictionSemanticsTest.TestConfig.class)
class SoftDeleteRestrictionSemanticsTest {

    @Autowired
    private SoftDeleteRestrictionSemanticsRepository repository;

    @Autowired
    private EntityManager entityManager;

    private Long deletedId;

    @BeforeEach
    void setUp() {
        TestAggregateRootWithSoftDelete entity = new TestAggregateRootWithSoftDelete();
        entity.setName("semantics");
        TestAggregateRootWithSoftDelete saved = repository.saveAndFlush(entity);

        repository.delete(saved);
        entityManager.flush();
        entityManager.clear();
        deletedId = saved.getId();
    }

    @Test
    void shouldExcludeDeletedFromFindById_when_restrictionDeclared() {
        assertThat(repository.findById(deletedId))
                .as("findById（清 L1 后）应被 restriction 过滤")
                .isEmpty();
    }

    @Test
    void shouldExcludeDeletedFromDerivedQuery_when_restrictionDeclared() {
        assertThat(repository.findByName("semantics"))
                .as("派生查询应被 restriction 过滤")
                .isEmpty();
    }

    @Test
    void shouldExcludeDeletedFromExplicitJpql_when_restrictionDeclared() {
        assertThat(repository.jpqlByName("semantics"))
                .as("显式 JPQL 应被 restriction 过滤（restriction 作用于 Hibernate 生成的 SQL）")
                .isEmpty();
    }

    @Test
    void shouldNotExcludeDeletedFromNativeSql_when_restrictionDoesNotApply() {
        assertThat(repository.nativeByName("semantics"))
                .as("原生 SQL 绕过 Hibernate 生成的 SQL，restriction 不作用")
                .hasSize(1);
    }

    @org.springframework.context.annotation.Configuration
    @EnableJpaRepositories(
            basePackageClasses = SoftDeleteRestrictionSemanticsRepository.class,
            repositoryBaseClass = BaseRepositoryImpl.class
    )
    @EntityScan(basePackageClasses = TestAggregateRootWithSoftDelete.class)
    static class TestConfig {
    }
}
