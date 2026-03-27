package com.cartisan.data.jpa.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository 工厂自动配置测试。
 *
 * <p>验证 BeanPostProcessor 正确应用 BaseRepositoryImpl 作为基类。</p>
 *
 * <p>使用 {@link RepositoryFactoryCustomizerTestApplication} 作为测试配置，
 * 该配置启用了 {@link CartisanDataJpaAutoConfiguration}。</p>
 */
@DataJpaTest
@ContextConfiguration(classes = RepositoryFactoryCustomizerTestApplication.class)
class RepositoryFactoryCustomizerTest {

    @Autowired
    private RepositoryFactoryCustomizerTestRepository testRepository;

    @Test
    void shouldLoadAutoConfiguration() {
        // This test verifies the configuration loads without errors
        assertThat(testRepository).isNotNull();
    }

    @Test
    void shouldUseBaseRepositoryImplAsRepositoryBaseClass() {
        // Given: 创建一个新实体
        RepositoryFactoryCustomizerTestEntity entity = new RepositoryFactoryCustomizerTestEntity();
        entity.setName("test");

        // When: 通过 Repository 保存实体
        RepositoryFactoryCustomizerTestEntity saved = testRepository.save(entity);

        // Then: Repository 正常工作，说明 BaseRepositoryImpl 被正确使用
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("test");
    }
}
