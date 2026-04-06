/**
 * 领域模型基础抽象包。
 *
 * <p>本包提供领域驱动设计（DDD）的核心抽象类型，包括：</p>
 * <ul>
 *   <li>{@link com.cartisan.core.domain.AggregateRoot} - 聚合根标记接口</li>
 *   <li>{@link com.cartisan.core.domain.DomainEntity} - 实体接口</li>
 *   <li>{@link com.cartisan.core.domain.ValueObject} - 值对象接口</li>
 *   <li>{@link com.cartisan.core.domain.Identity} - 标识符接口</li>
 *   <li>{@link com.cartisan.core.domain.BaseEnum} - 业务枚举基础接口</li>
 * </ul>
 *
 * <p>设计原则：</p>
 * <ul>
 *   <li>零外部依赖 - 仅使用 JDK 标准库</li>
 *   <li>类型安全 - 通过泛型确保编译时类型检查</li>
 *   <li>Java Record 友好 - 所有接口均可由 Record 实现</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.core.domain;
