package com.cartisan.core.stereotype;

/**
 * 六边形架构的端口类型。
 *
 * <p>端口是架构的边界，定义了系统与外部交互的契约。
 * 适配器（Adapter）实现端口，完成具体的技术集成。</p>
 *
 * <h2>端口类型</h2>
 * <ul>
 *   <li>{@link #REPOSITORY} - 仓储端口：聚合根的持久化</li>
 *   <li>{@link #CLIENT} - 客户端端口：调用外部服务</li>
 *   <li>{@link #PUBLISHER} - 发布者端口：发布应用事件</li>
 * </ul>
 *
 * @see Port
 * @see Adapter
 * @since 0.1.0
 */
public enum PortType {
    /**
     * 仓储端口。
     *
     * <p>用于聚合根的持久化，是六边形架构的出端口。
     * 仓储接口定义了如何存储和检索聚合根。</p>
     *
     * <p>适配器示例：JPA 仓储、MongoDB 仓储、内存仓储。</p>
     */
    REPOSITORY,

    /**
     * 客户端端口。
     *
     * <p>用于调用外部服务，是六边形架构的出端口。
     * 客户端端口定义了与外部系统交互的接口。</p>
     *
     * <p>适配器示例：REST 客户端、gRPC 客户端、消息队列客户端。</p>
     */
    CLIENT,

    /**
     * 发布者端口。
     *
     * <p>用于发布应用事件，是六边形架构的出端口。
     * 发布者端口定义了如何向外部发送应用事件。</p>
     *
     * <p>适配器示例：Spring Event 适配器、Kafka 发布器、RabbitMQ 发布器。</p>
     */
    PUBLISHER
}
