/**
 * Hibernate 集成：在元模型构建期自动装配框架级映射增强（如软删除读过滤）。
 *
 * <p>本包下的 {@link com.cartisan.data.jpa.hibernate.SoftDeleteRestrictionContributor}
 * 通过 Hibernate 的 ServiceLoader SPI 自动发现，无需 Spring 配置。</p>
 */
package com.cartisan.data.jpa.hibernate;
