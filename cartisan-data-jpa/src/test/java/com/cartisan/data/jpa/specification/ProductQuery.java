package com.cartisan.data.jpa.specification;

import com.cartisan.data.jpa.specification.Condition;
import com.cartisan.data.jpa.specification.ConditionType;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产品查询 DTO。
 *
 * <p>使用 @Condition 注解标注各种查询条件，用于测试 ConditionSpecifications。</p>
 *
 * <h3>支持的查询条件：</h3>
 * <ul>
 *   <li>name - 模糊搜索 (INNER_LIKE)</li>
 *   <li>minStock - 库存大于等于 (GREATER_EQUAL)</li>
 *   <li>maxStock - 库存小于等于 (LESS_EQUAL)</li>
 *   <li>category - 相等 (EQUAL)</li>
 *   <li>categories - IN 查询</li>
 *   <li>stockRange - 库存区间 (BETWEEN 查询)</li>
 *   <li>keyword - 多字段模糊搜索 (blurry)</li>
 * </ul>
 *
 * <p><b>注意：</b>由于 ConditionSpecifications 对 BigDecimal 的类型推断限制，
 * 大小比较和区间查询测试使用 Integer 类型的 stock 字段。</p>
 */
public record ProductQuery(

        /**
         * 产品名称模糊搜索。
         */
        @Condition(type = ConditionType.INNER_LIKE)
        String name,

        /**
         * 最低库存（大于等于）。
         */
        @Condition(propName = "stock", type = ConditionType.GREATER_EQUAL)
        Integer minStock,

        /**
         * 最高库存（小于等于）。
         */
        @Condition(propName = "stock", type = ConditionType.LESS_EQUAL)
        Integer maxStock,

        /**
         * 产品分类（相等）。
         */
        @Condition(type = ConditionType.EQUAL)
        String category,

        /**
         * 产品分类列表（IN 查询）。
         */
        @Condition(propName = "category", type = ConditionType.IN)
        List<String> categories,

        /**
         * 库存区间（BETWEEN 查询）。
         */
        @Condition(propName = "stock", type = ConditionType.BETWEEN)
        List<Integer> stockRange,

        /**
         * 关键词多字段模糊搜索。
         */
        @Condition(blurry = "name,category")
        String keyword
) {
}
