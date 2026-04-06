package com.cartisan.core.domain;

/**
 * 聚合根标记接口。
 *
 * <p>聚合根是 DDD 中的核心概念，表示一组相关对象的访问入口点。</p>
 *
 * <h3>聚合根特征</h3>
 * <ul>
 *   <li>拥有全局唯一标识</li>
 *   <li>负责维护其内部对象的不变性约束</li>
 *   <li>外部对象只能通过聚合根来访问其内部对象</li>
 * </ul>
 *
 * <h3>用途</h3>
 * <ul>
 *   <li>类型约束：只有聚合根才能有 {@code Repository}</li>
 *   <li>架构验证：通过 ArchUnit 验证聚合根的正确性</li>
 * </ul>
 *
 * @param <T> 聚合根类型
 * @param <ID> 标识符类型
 * @since 0.1.0
 */
public interface AggregateRoot<T, ID> {
}
