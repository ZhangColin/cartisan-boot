/**
 * SSE 流式输出工具。
 *
 * <p>提供将响应式流转换为 Spring MVC {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter SseEmitter}
 * 的能力，封装超时、异常和客户端断开处理。</p>
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link com.cartisan.ai.sse.SseHelper SseHelper} — 将 {@code Flux<ChatStreamEvent>} 转换为 {@code SseEmitter}</li>
 *   <li>{@link com.cartisan.ai.sse.SseProperties SseProperties} — SSE 配置属性（超时、心跳等）</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.ai.sse;
