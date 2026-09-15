/**
 * 请求参数契约（wire 进）。
 *
 * <p>定义分页/排序的 wire 契约：{@link com.cartisan.web.request.Pagination}
 * （全链 1-based 分页 + 排序）与 {@link com.cartisan.web.request.Ordering}
 * （不分页端点的客户端控排序）。controller 直接声明 record 参数，
 * Spring MVC 按组件名原生绑定顶级参数；回显出口见
 * {@link com.cartisan.web.response.PageResponse#of}。</p>
 *
 * <p>查询端三段式范式：controller 并列参数（业务 Query record + Pagination）
 * → appservice 返回 Spring Data {@code Page} 或自组读侧（jOOQ 用
 * {@code offset()}/{@code limit()}）→ controller/appservice 工厂回显
 * {@code PageResponse.of}。页码换算与防御收在框架一处，业务代码零算术。</p>
 *
 * @since 0.2.0
 */
package com.cartisan.web.request;
