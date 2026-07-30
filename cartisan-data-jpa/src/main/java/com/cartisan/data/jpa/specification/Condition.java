package com.cartisan.data.jpa.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询条件注解，用于标注查询 DTO 字段，指定查询条件类型。
 *
 * <p>该注解配合 JPA Specification 使用，通过注解方式声明查询条件，
 * 避免手动编写 Predicate 构建逻辑。</p>
 *
 * <p>支持的查询类型由 {@link ConditionType} 枚举定义，包括：
 * 相等性比较、大小比较、模糊查询、集合与区间查询等。</p>
 *
 * <h3>基本用法</h3>
 * <pre>{@code
 * public record UserQuery(
 *     @Condition(type = ConditionType.INNER_LIKE) String username,
 *     @Condition(propName = "status", type = ConditionType.EQUAL) Integer status,
 *     @Condition(blurry = "title,content") String keyword
 * ) {}
 * }</pre>
 *
 * <h3>属性说明</h3>
 * <ul>
 *   <li>{@code propName} - 实体属性名，默认与字段名相同。
 *       当 DTO 字段名与实体属性名不一致时使用。</li>
 *   <li>{@code type} - 查询条件类型，默认为 {@link ConditionType#EQUAL}。</li>
 *   <li>{@code blurry} - 多字段模糊搜索，逗号分隔。
 *       用于在一个字段上对多个实体属性进行模糊查询。</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 等值查询（默认类型）
 * public record UserQuery(
 *     @Condition String username  // WHERE username = ?
 * ) {}
 *
 * // 指定实体属性名
 * public record UserQuery(
 *     @Condition(propName = "userStatus") Integer status  // WHERE userStatus = ?
 * ) {}
 *
 * // 模糊查询
 * public record UserQuery(
 *     @Condition(type = ConditionType.INNER_LIKE) String title  // WHERE title LIKE %?%
 * ) {}
 *
 * // 多字段模糊查询
 * public record ArticleQuery(
 *     @Condition(blurry = "title,subtitle,content") String keyword
 *     // WHERE title LIKE %keyword% OR subtitle LIKE %keyword% OR content LIKE %keyword%
 * ) {}
 * }</pre>
 *
 * @see ConditionType
 * @since 0.1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Condition {

    /**
     * 实体属性名。
     *
     * <p>默认为空字符串，表示使用与注解字段相同的名称。
     * 当 DTO 字段名与实体属性名不一致时，通过此属性指定实体属性名。</p>
     *
     * @return 实体属性名，默认为空字符串
     */
    String propName() default "";

    /**
     * 查询条件类型。
     *
     * <p>默认为相等查询 {@link ConditionType#EQUAL}。
     * 可选值包括：相等、不等、大于、小于、模糊查询、IN、BETWEEN 等。</p>
     *
     * @return 查询条件类型
     */
    ConditionType type() default ConditionType.EQUAL;

    /**
     * 多字段模糊搜索配置。
     *
     * <p>指定需要进行模糊查询的实体属性列表，用逗号分隔。
     * 当设置此属性时，会对指定字段分别进行 LIKE 查询，并用 OR 连接。</p>
     *
     * <p>注意：使用此属性时，{@code type} 属性会被忽略，始终使用 INNER_LIKE 查询。</p>
     *
     * @return 多字段模糊搜索配置，默认为空字符串表示不启用
     */
    String blurry() default "";
}
