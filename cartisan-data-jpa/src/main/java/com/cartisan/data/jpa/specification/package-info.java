/**
 * 基于 JPA Criteria API 的动态查询条件生成。
 *
 * <p>本包提供基于注解的查询条件构建功能，支持通过声明式注解自动生成 JPA Specification，
 * 简化动态查询场景的编码工作。</p>
 *
 * <p>核心组件：</p>
 * <ul>
 *   <li>{@link com.cartisan.data.jpa.specification.Condition} — 查询条件注解，用于标记查询参数</li>
 *   <li>{@link com.cartisan.data.jpa.specification.ConditionType} — 条件类型枚举，定义支持的查询操作符</li>
 *   <li>{@link com.cartisan.data.jpa.specification.ConditionSpecifications} — Specification 生成工具类</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * public record UserQuery(
 *     @Condition(field = "name", type = ConditionType.LIKE)
 *     String name,
 *
 *     @Condition(field = "status", type = ConditionType.EQUAL)
 *     UserStatus status
 * ) {}
 *
 * Specification<User> spec = ConditionSpecifications.of(query);
 * List<User> users = repository.findAll(spec);
 * }</pre>
 *
 * @since 0.3.0
 */
package com.cartisan.data.jpa.specification;
