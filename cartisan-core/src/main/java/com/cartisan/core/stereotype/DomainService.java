package com.cartisan.core.stereotype;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 领域服务注解。
 *
 * <p>标注在领域服务类上，标识该类是 DDD 领域服务。
 * 领域服务用于封装不属于任何聚合根或值对象的领域逻辑。</p>
 *
 * <h2>何时使用领域服务</h2>
 * <ul>
 *   <li>操作涉及多个聚合根</li>
 *   <li>操作需要访问基础设施（如调用外部服务）</li>
 *   <li>计算逻辑不属于任何实体或值对象</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * {@literal @}DomainService
 * public class OrderPricingService {
 *
 *     public Money calculateDiscount(Order order, Customer customer) {
 *         // 不属于任何聚合根的定价逻辑
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * <h2>设计约束</h2>
 * <ul>
 *   <li>领域服务不包含应用层逻辑（如编排、事务管理）</li>
 *   <li>领域服务保持无状态</li>
 *   <li>优先将逻辑放入聚合根，只有无法归属时才使用领域服务</li>
 * </ul>
 *
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface DomainService {
}
