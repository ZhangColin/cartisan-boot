package com.cartisan.data.jpa.domain;

import com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证实体显式声明 {@code @SQLRestriction} 时，框架不覆盖、不叠加（AC #3）。
 *
 * <p>实体 {@link TestSoftDeletableCustomRestriction} 声明 {@code @SQLRestriction("name <> 'hidden'")}，
 * 且实现 {@link SoftDeletable}。若 Contributor 错误叠加 {@code deleted = false}，
 * 软删后的 visible 记录会被过滤——通过它仍可见来证明 Contributor 尊重了显式声明。</p>
 *
 * <p><b>L1 纪律</b>：读断言前 {@code flush + clear} 持久化上下文。</p>
 */
@DataJpaTest
@Import(SoftDeletableCustomRestrictionTest.TestConfig.class)
class SoftDeletableCustomRestrictionTest {

    @Autowired
    private TestSoftDeletableCustomRestrictionRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldHonorExplicitRestrictionAndNotAugment_when_softDeletableWithOwnRestriction() {
        TestSoftDeletableCustomRestriction visible = new TestSoftDeletableCustomRestriction();
        visible.setName("visible");
        repository.saveAndFlush(visible);

        TestSoftDeletableCustomRestriction hidden = new TestSoftDeletableCustomRestriction();
        hidden.setName("hidden");
        repository.saveAndFlush(hidden);
        entityManager.clear();

        // 自定义表达式生效：hidden 被过滤，visible 仍在
        assertThat(repository.findAll())
                .as("显式 @SQLRestriction 表达式应生效")
                .extracting(TestSoftDeletableCustomRestriction::getName)
                .containsExactly("visible");

        // 软删 visible
        repository.delete(visible);
        entityManager.flush();
        entityManager.clear();

        // 若 Contributor 叠加了 deleted = false，visible 会被过滤掉；
        // 仍返回 visible 证明 Contributor 未叠加，尊重了显式声明。
        List<TestSoftDeletableCustomRestriction> result = repository.findAll();
        assertThat(result)
                .as("显式 @SQLRestriction 不应被叠加 deleted = false")
                .extracting(TestSoftDeletableCustomRestriction::getName)
                .containsExactly("visible");
    }

    @org.springframework.context.annotation.Configuration
    @EnableJpaRepositories(
            basePackageClasses = TestSoftDeletableCustomRestrictionRepository.class,
            repositoryBaseClass = BaseRepositoryImpl.class
    )
    @EntityScan(basePackageClasses = TestSoftDeletableCustomRestriction.class)
    static class TestConfig {
    }
}
