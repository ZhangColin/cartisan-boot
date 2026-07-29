package com.cartisan.data.jpa.domain;

import com.cartisan.data.jpa.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * {@link TestInheritedSoftDelete} 的 Repository，含派生查询与显式 JPQL 用于覆盖读路径。
 */
interface TestInheritedSoftDeleteRepository extends BaseRepository<TestInheritedSoftDelete, Long> {

    List<TestInheritedSoftDelete> findByName(String name);

    @Query("SELECT e FROM test_inherited_soft_delete e WHERE e.name = :name")
    List<TestInheritedSoftDelete> jpqlByName(@Param("name") String name);
}
