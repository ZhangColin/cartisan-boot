/**
 * 分布式 ID 生成。
 *
 * <p>提供基于 TSID（Time-Sorted ID）算法的 ID 生成器，适用于：
 * <ul>
 *   <li>JPA 实体主键生成（替代数据库自增 ID）</li>
 *   <li>分布式环境下全局唯一 ID</li>
 *   <li>时间排序的业务单据号</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.data.jpa.id;
