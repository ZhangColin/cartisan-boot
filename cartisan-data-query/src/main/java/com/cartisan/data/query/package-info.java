/**
 * cartisan-data-query 模块根包。
 *
 * <p>提供 jOOQ 读侧封装基础设施，包括多租户查询支持、自动配置等。分页出口
 * （1-based wire 契约与 {@code offset()}/{@code limit()} 直出）由本模块依赖的
 * cartisan-web {@link com.cartisan.web.request.Pagination} 提供——jOOQ 读侧
 * {@code .limit(pagination.limit()).offset(pagination.offset())}，排序经
 * {@code pagination.toSort(白名单)} 防 SQL 注入。</p>
 *
 * <p>与 cartisan-data-jpa（写侧）配合使用，实现 CQRS 架构。</p>
 */
package com.cartisan.data.query;
