package com.cartisan.data.jpa.domain;

import com.cartisan.data.jpa.repository.BaseRepository;

/**
 * 测试用 Repository，用于自动软删除功能集成测试。
 *
 * <p>继承 BaseRepository 以使用 BaseRepositoryImpl 的自动软删除功能。</p>
 */
interface TestAggregateRootWithSoftDeleteRepository extends BaseRepository<TestAggregateRootWithSoftDelete, Long> {
}
