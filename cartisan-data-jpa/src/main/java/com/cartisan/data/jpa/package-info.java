/**
 * JPA 持久化基础设施。
 *
 * <p>本模块提供基于 Spring Data JPA 的 Repository 基类，通过泛型约束在编译期强制
 * "只有聚合根才能拥有 Repository" 这一 DDD 约束。</p>
 *
 * @since 0.2.0
 */
package com.cartisan.data.jpa;
