package com.cartisan.data.jpa.repository.impl;

import com.cartisan.data.jpa.repository.BaseRepository;
import org.springframework.stereotype.Repository;

/**
 * 测试用 Repository 接口。
 */
@Repository
interface TestAggregateRootRepository extends BaseRepository<TestAggregateRoot, String> {
}
