package com.cartisan.data.jpa.domain;

import com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 软删除读过滤（直接实现 {@link SoftDeletable} 接口）回归测试（AC #2）。
 *
 * <p>实体 {@link TestDirectSoftDeletable} 直接实现 {@link SoftDeletable}（不经
 * {@link AuditableSoftDeletable}），自身不声明 @SQLRestriction，应获得与继承路径同样的读过滤。</p>
 *
 * <p><b>L1 纪律</b>：所有读过滤断言前先 {@code flush + clear} 持久化上下文。</p>
 */
@DataJpaTest
@Import(DirectSoftDeletableReadFilterTest.TestConfig.class)
class DirectSoftDeletableReadFilterTest {

    @Autowired
    private TestDirectSoftDeletableRepository repository;

    @Autowired
    private EntityManager entityManager;

    private Long softDeleteAndClear(String name) {
        TestDirectSoftDeletable entity = new TestDirectSoftDeletable();
        entity.setName(name);
        TestDirectSoftDeletable saved = repository.saveAndFlush(entity);

        repository.delete(saved);
        entityManager.flush();
        entityManager.clear();
        return saved.getId();
    }

    @Test
    void shouldExcludeDeletedFromFindById_when_directSoftDeletable() {
        Long deletedId = softDeleteAndClear("x");

        assertThat(repository.findById(deletedId))
                .as("findById（清 L1 后）应排除已软删记录")
                .isEmpty();
    }

    @Test
    void shouldExcludeDeletedFromFindAll_when_directSoftDeletable() {
        softDeleteAndClear("x");

        assertThat(repository.findAll())
                .as("findAll 应排除已软删记录")
                .isEmpty();
    }

    @Test
    void shouldExcludeDeletedFromCount_when_directSoftDeletable() {
        softDeleteAndClear("x");

        assertThat(repository.count())
                .as("count 不应计入已软删记录")
                .isZero();
    }

    @Test
    void shouldIncludeActiveInFindAll_when_directSoftDeletable() {
        TestDirectSoftDeletable active = new TestDirectSoftDeletable();
        active.setName("active");
        repository.saveAndFlush(active);
        entityManager.clear();

        assertThat(repository.findAll())
                .as("未删除记录应正常返回")
                .hasSize(1);
    }

    @org.springframework.context.annotation.Configuration
    @EnableJpaRepositories(
            basePackageClasses = TestDirectSoftDeletableRepository.class,
            repositoryBaseClass = BaseRepositoryImpl.class
    )
    @EntityScan(basePackageClasses = TestDirectSoftDeletable.class)
    static class TestConfig {
    }
}
