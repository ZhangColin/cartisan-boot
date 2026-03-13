package com.cartisan.core.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 端口注解。
 *
 * <p>标注在端口接口上，标识六边形架构中的端口（出/入站契约）。
 * 端口定义了系统与外部世界交互的接口，是架构的边界。</p>
 *
 * <h2>端口类型</h2>
 * <ul>
 *   <li>{@link PortType#REPOSITORY} - 仓储端口：聚合根的持久化</li>
 *   <li>{@link PortType#CLIENT} - 客户端端口：调用外部服务</li>
 *   <li>{@link PortType#PUBLISHER} - 发布者端口：发布领域事件</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 仓储端口（出端口）
 * {@literal @}Port(PortType.REPOSITORY)
 * public interface OrderRepository extends BaseRepository<Order, OrderId> {
 *     Optional<Order> findById(OrderId id);
 * }
 *
 * // 客户端端口（出端口）
 * {@literal @}Port(PortType.CLIENT)
 * public interface PaymentGateway {
 *     PaymentResult charge(Money amount);
 * }
 * }</pre>
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>端口是接口，定义契约而不包含实现细节</li>
 *   <li>端口位于领域层，不依赖具体技术</li>
 *   <li>适配器（{@link Adapter}）实现端口，完成技术集成</li>
 * </ul>
 *
 * @see Adapter
 * @see PortType
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Port {

    /**
     * 端口类型。
     *
     * <p>标识该端口属于哪种类型：仓储、客户端或发布者。</p>
     *
     * @return 端口类型
     */
    PortType value();
}
