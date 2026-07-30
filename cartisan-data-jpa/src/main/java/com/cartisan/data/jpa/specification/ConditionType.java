package com.cartisan.data.jpa.specification;

/**
 * JPA Criteria 查询条件类型枚举。
 *
 * <p>定义支持的查询操作符，用于 {@link Condition} 注解的 type 属性。</p>
 *
 * <p>支持的查询类型：</p>
 * <ul>
 *   <li>相等性比较：EQUAL, NOT_EQUAL</li>
 *   <li>大小比较：GREATER_EQUAL, GREATER, LESS_EQUAL, LESS</li>
 *   <li>模糊查询：INNER_LIKE, LEFT_LIKE, RIGHT_LIKE</li>
 *   <li>集合与区间查询：IN, BETWEEN</li>
 * </ul>
 *
 * @since 0.1.0
 */
public enum ConditionType {

    // ==================== 相等性比较 ====================

    /**
     * 相等查询。
     *
     * <p>示例：WHERE field = value</p>
     */
    EQUAL,

    /**
     * 不相等查询。
     *
     * <p>示例：WHERE field != value</p>
     */
    NOT_EQUAL,

    // ==================== 大小比较 ====================

    /**
     * 大于等于查询。
     *
     * <p>示例：WHERE field >= value</p>
     */
    GREATER_EQUAL,

    /**
     * 大于查询。
     *
     * <p>示例：WHERE field > value</p>
     */
    GREATER,

    /**
     * 小于等于查询。
     *
     * <p>示例：WHERE field <= value</p>
     */
    LESS_EQUAL,

    /**
     * 小于查询。
     *
     * <p>示例：WHERE field < value</p>
     */
    LESS,

    // ==================== 模糊查询 ====================

    /**
     * 中间模糊查询。
     *
     * <p>示例：WHERE field LIKE '%value%'</p>
     */
    INNER_LIKE,

    /**
     * 左模糊查询。
     *
     * <p>示例：WHERE field LIKE '%value'</p>
     */
    LEFT_LIKE,

    /**
     * 右模糊查询。
     *
     * <p>示例：WHERE field LIKE 'value%'</p>
     */
    RIGHT_LIKE,

    // ==================== 集合与区间查询 ====================

    /**
     * IN 查询。
     *
     * <p>示例：WHERE field IN (value1, value2, ...)</p>
     */
    IN,

    /**
     * BETWEEN 区间查询。
     *
     * <p>示例：WHERE field BETWEEN value1 AND value2</p>
     */
    BETWEEN
}
