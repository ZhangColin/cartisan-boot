package com.cartisan.core.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 聚合根注解。
 *
 * <p>标注在聚合根类上，标识该类是 DDD 聚合根。
 * 聚合根是 DDD 中一致性维护的边界，也是数据持久化的基本单位。</p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * {@literal @}Aggregate
 * public class Order extends AbstractAggregateRoot<OrderId> {
 *     private OrderId id;
 *     // ...
 * }
 * }</pre>
 *
 * <h2>设计约束</h2>
 * <ul>
 *   <li>只有聚合根才能拥有 Repository（由 BaseRepository 泛型约束）</li>
 *   <li>聚合根负责维护其内部实体的一致性</li>
 *   <li>聚合根通过 {@code registerEvent()} 发布领域事件</li>
 * </ul>
 *
 * @see com.cartisan.core.domain.AggregateRoot
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Aggregate {
}
