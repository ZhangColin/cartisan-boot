package com.cartisan.core.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 适配器注解。
 *
 * <p>标注在适配器实现类上，标识六边形架构中的适配器（端口的具体实现）。
 * 适配器负责将端口接口与具体技术集成。</p>
 *
 * <h2>适配器类型</h2>
 * <ul>
 *   <li>{@link PortType#REPOSITORY} - 仓储适配器：JPA、MongoDB 等</li>
 *   <li>{@link PortType#CLIENT} - 客户端适配器：REST、gRPC 等</li>
 *   <li>{@link PortType#PUBLISHER} - 发布者适配器：Kafka、RabbitMQ 等</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 仓储适配器
 * {@literal @}Adapter(PortType.REPOSITORY)
 * public class JpaOrderRepository implements OrderRepository {
 *     {@literal @}PersistenceContext
 *     private EntityManager em;
 *     // ...
 * }
 *
 * // 客户端适配器
 * {@literal @}Adapter(PortType.CLIENT)
 * public class StripePaymentGatewayAdapter implements PaymentGateway {
 *     private StripeClient client;
 *     // ...
 * }
 * }</pre>
 *
 * <h2>设计约束</h2>
 * <ul>
 *   <li>适配器的 PortType 必须与实现的端口的 PortType 一致</li>
 *   <li>适配器位于基础设施层，依赖具体技术</li>
 *   <li>由 ArchUnit 规则验证适配器与端口的类型一致性</li>
 * </ul>
 *
 * @see Port
 * @see PortType
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Adapter {

    /**
     * 适配的端口类型。
     *
     * <p>必须与实现的端口接口的 {@link Port#value()} 一致。
     * 由 ArchUnit 规则强制验证此约束。</p>
     *
     * @return 端口类型
     */
    PortType value();
}
