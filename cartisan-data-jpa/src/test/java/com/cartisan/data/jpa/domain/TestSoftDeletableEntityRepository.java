package com.cartisan.data.jpa.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 测试用 Repository，用于软删除功能集成测试。
 */
interface TestSoftDeletableEntityRepository extends JpaRepository<TestSoftDeletableEntity, Long> {

    List<TestSoftDeletableEntity> findByName(@Param("name") String name);

    @Query("SELECT e FROM test_soft_deletable_entity e WHERE e.name = :name")
    List<TestSoftDeletableEntity> queryByName(@Param("name") String name);
}
