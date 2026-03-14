package com.cartisan.data.jpa.repository.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseRepositoryImpl 单元测试。
 *
 * <p>注意：由于 SimpleJpaRepository 的构造器需要真实的 EntityManager，
 * 单元测试无法 mock。完整的行为测试在集成测试中进行。</p>
 *
 * @see RepositoryEventPublishingIntegrationTest
 */
class BaseRepositoryImplTest {

    @Test
    void baseRepositoryImpl_classExists() {
        // 验证类可以被加载（编译通过）
        assertThat(BaseRepositoryImpl.class).isNotNull();
    }
}
