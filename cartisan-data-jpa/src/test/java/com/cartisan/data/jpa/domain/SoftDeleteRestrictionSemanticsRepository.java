package com.cartisan.data.jpa.domain;

import com.cartisan.data.jpa.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * {@link TestAggregateRootWithSoftDelete}（实体自身显式声明 @SQLRestriction）的 Repository，
 * 含派生查询 / 显式 JPQL / 原生 SQL，用于覆盖读路径语义边界。
 */
interface SoftDeleteRestrictionSemanticsRepository extends BaseRepository<TestAggregateRootWithSoftDelete, Long> {

    List<TestAggregateRootWithSoftDelete> findByName(String name);

    @Query("SELECT e FROM test_soft_deletable_aggregate e WHERE e.name = :name")
    List<TestAggregateRootWithSoftDelete> jpqlByName(@Param("name") String name);

    @Query(value = "SELECT * FROM test_soft_deletable_aggregate WHERE name = :name", nativeQuery = true)
    List<TestAggregateRootWithSoftDelete> nativeByName(@Param("name") String name);
}
