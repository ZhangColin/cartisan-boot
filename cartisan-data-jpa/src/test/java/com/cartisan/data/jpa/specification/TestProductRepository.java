package com.cartisan.data.jpa.specification;

import com.cartisan.data.jpa.repository.BaseRepository;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

/**
 * 测试产品 Repository。
 *
 * <p>用于测试 @Condition 注解和 ConditionSpecifications。</p>
 */
public interface TestProductRepository extends BaseRepository<TestProduct, Long> {

    /**
     * 根据查询条件查找产品。
     *
     * @param query 查询条件
     * @return 产品列表
     */
    default List<TestProduct> findByCondition(ProductQuery query) {
        Specification<TestProduct> spec = Specification.where(null);
        Specification<TestProduct> conditionSpec = ConditionSpecifications.fromAnnotation(query);

        if (conditionSpec != null) {
            spec = spec.and(conditionSpec);
        }

        return findAll(spec);
    }

    /**
     * 根据名称查找产品。
     *
     * @param name 产品名称
     * @return 产品列表
     */
    List<TestProduct> findByName(String name);

    /**
     * 根据分类查找产品。
     *
     * @param category 产品分类
     * @return 产品列表
     */
    List<TestProduct> findByCategory(String category);
}
