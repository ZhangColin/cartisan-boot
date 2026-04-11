/**
 * Cartisan Core - DDD 基础设施模块
 *
 * <p>本模块提供领域驱动设计（DDD）的核心基础设施，包括：</p>
 * <ul>
 *   <li>领域模型基础抽象（Entity, ValueObject, AggregateRoot）</li>
 *   <li>Repository 接口抽象</li>
 *   <li>领域异常基类</li>
 * </ul>
 *
 * <p>设计原则：</p>
 * <ul>
 *   <li>stereotype 注解基于 Spring @Component（provided scope）</li>
 *   <li>提供纯粹的领域抽象，可被其他模块继承和扩展</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.core;
