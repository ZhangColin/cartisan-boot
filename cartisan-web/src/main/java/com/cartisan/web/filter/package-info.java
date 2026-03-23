/**
 * Web 过滤器组件。
 *
 * <p>提供 Servlet Filter 实现：</p>
 * <ul>
 *   <li>{@link com.cartisan.web.filter.RequestLogFilter} - 请求日志记录</li>
 * </ul>
 *
 * <p><strong>设计原则：</strong></p>
 * <ul>
 *   <li>继承 {@link org.springframework.web.filter.OncePerRequestFilter} 确保单次请求只执行一次</li>
 *   <li>日志记录使用 INFO 级别</li>
 *   <li>排除监控和管理路径（swagger、druid、actuator）</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.cartisan.web.filter;
