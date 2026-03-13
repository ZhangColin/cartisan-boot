/**
 * DDD 架构注解（Stereotype Annotations）。
 *
 * <p>本包提供领域驱动设计（DDD）的架构标记注解，作为"可执行的架构文档"。
 * 这些注解本身不产生运行时行为，配合 ArchUnit 规则可自动验证架构约束。</p>
 *
 * <h2>包含的注解</h2>
 * <ul>
 *   <li>{@link com.cartisan.core.stereotype.BoundedContext} - 限界上下文注解</li>
 *   <li>{@link com.cartisan.core.stereotype.Aggregate} - 聚合根注解</li>
 *   <li>{@link com.cartisan.core.stereotype.DomainService} - 领域服务注解</li>
 *   <li>{@link com.cartisan.core.stereotype.Port} - 端口注解</li>
 *   <li>{@link com.cartisan.core.stereotype.Adapter} - 适配器注解</li>
 * </ul>
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>零外部依赖：仅使用 JDK 标准库</li>
 *   <li>标记而非行为：注解本身不产生运行时行为</li>
 *   <li>可执行：配合 ArchUnit 规则，自动验证架构约束</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.core.stereotype;
